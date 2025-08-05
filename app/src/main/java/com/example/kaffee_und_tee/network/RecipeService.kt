package com.example.kaffee_und_tee.network

import android.util.Log
import com.example.kaffee_und_tee.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class RecipeService {
    private val baseUrl = "https://www.swr.de/leben/rezepte/kaffee-oder-tee-rezepte-archiv-100.html"
    private val TAG = "RecipeService"
    
    suspend fun fetchRecipes(): List<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting to fetch recipes from $baseUrl")
                val doc = Jsoup.connect(baseUrl)
                    .timeout(10000) // 10 second timeout
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()
                Log.d(TAG, "Successfully connected to website")
                
                val recipes = parseRecipes(doc)
                Log.d(TAG, "Found ${recipes.size} recipes")
                recipes
            } catch (e: IOException) {
                Log.e(TAG, "Network error while fetching recipes", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error while fetching recipes", e)
                emptyList()
            }
        }
    }
    
    private fun parseRecipes(doc: Document): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        val recipeElements = doc.select("article")
        Log.d(TAG, "Found ${recipeElements.size} recipe elements")

        recipeElements.forEach { element ->
            // Try to get title from h2 first, then from link text if h2 is empty
            var title = element.select("h2").text()
            if (title.isBlank()) {
                title = element.select("a").first()?.text() ?: ""
            }
            title = title.replace("Rezepte ", "").trim()

            val imageUrl = element.select("img").firstOrNull()?.attr("src") ?: ""
            val link = element.select("a").firstOrNull()?.attr("href") ?: ""

            // Make URLs absolute
            val absoluteImageUrl = if (imageUrl.startsWith("/")) {
                "https://www.swr.de$imageUrl"
            } else {
                imageUrl
            }
            val absoluteLink = if (link.startsWith("/")) {
                "https://www.swr.de$link"
            } else {
                link
            }

            if (title.isNotEmpty() && absoluteImageUrl.isNotEmpty() && absoluteLink.isNotEmpty()) {
                val recipe = Recipe(
                    title = title,
                    imageUrl = absoluteImageUrl,
                    ingredients = emptyList(),
                    videoUrl = null,
                    webpageUrl = absoluteLink,
                    pdfUrl = null
                )
                Log.d(TAG, "Parsing recipe: title='$title', imageUrl='$absoluteImageUrl', webpageUrl='$absoluteLink'")
                recipes.add(recipe)
            } else {
                Log.w(TAG, "Skipping recipe due to missing data: title='$title', imageUrl='$absoluteImageUrl', link='$absoluteLink'")
            }
        }

        Log.d(TAG, "Found ${recipes.size} recipes")
        return recipes
    }
    
    suspend fun fetchRecipeDetails(recipe: Recipe): Recipe {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching details for recipe: ${recipe.title}")
                Log.d(TAG, "URL: ${recipe.webpageUrl}")
                
                val doc = Jsoup.connect(recipe.webpageUrl)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()
                
                // Log all div classes for debugging
                doc.select("div").forEach { div ->
                    Log.d(TAG, "Found div with classes: ${div.classNames().joinToString(", ")}")
                }
                
                val ingredients = parseIngredients(doc)
                val videoUrl = parseVideoUrl(doc)
                val pdfUrl = parsePdfUrl(doc)
                
                Log.d(TAG, "Found ${ingredients.size} ingredients, video URL: $videoUrl, PDF URL: $pdfUrl")
                
                recipe.copy(
                    ingredients = ingredients,
                    videoUrl = videoUrl,
                    pdfUrl = pdfUrl
                )
            } catch (e: IOException) {
                Log.e(TAG, "Network error while fetching recipe details", e)
                recipe
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error while fetching recipe details", e)
                recipe
            }
        }
    }
    
    private fun parseIngredients(doc: Document): List<String> {
        val ingredients = mutableListOf<String>()
        
        // Try different selectors for ingredients
        val selectors = listOf(
            "div.ingredients ul li",
            "div[class*='ingredients'] ul li",
            "div[class*='recipe'] ul li",
            "ul.ingredients li",
            "div[class*='zutaten'] ul li",  // German word for ingredients
            "div[class*='Zutaten'] ul li"   // German word for ingredients (capitalized)
        )
        
        for (selector in selectors) {
            val elements = doc.select(selector)
            Log.d(TAG, "Trying ingredients selector '$selector': found ${elements.size} elements")
            
            if (elements.isNotEmpty()) {
                elements.forEach { element ->
                    try {
                        val ingredient = element.text().trim()
                        if (ingredient.isNotEmpty()) {
                            ingredients.add(ingredient)
                            Log.d(TAG, "Added ingredient: $ingredient")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing ingredient element", e)
                    }
                }
                break // Stop trying other selectors if we found ingredients
            }
        }
        
        if (ingredients.isEmpty()) {
            Log.w(TAG, "No ingredients found with any selector")
        }
        
        return ingredients
    }
    
    private fun parseVideoUrl(doc: Document): String? {
        // Try different selectors for video URL
        val selectors = listOf(
            "video source",
            "video[src]",
            "iframe[src*='video']",
            "div[class*='video'] iframe"
        )
        
        for (selector in selectors) {
            val element = doc.select(selector).firstOrNull()
            if (element != null) {
                val url = element.attr("src")
                if (url.isNotEmpty()) {
                    Log.d(TAG, "Found video URL: $url")
                    return url
                }
            }
        }
        
        Log.w(TAG, "No video URL found")
        return null
    }
    
    private fun parsePdfUrl(doc: Document): String? {
        // Look for links containing "Ausdrucken" or "PDF"
        val selectors = listOf(
            "a:contains(Ausdrucken)",
            "a:contains(PDF)",
            "a[href$=.pdf]"
        )
        
        for (selector in selectors) {
            val element = doc.select(selector).firstOrNull()
            if (element != null) {
                val url = element.attr("href")
                if (url.isNotEmpty()) {
                    // Make URL absolute if it's relative
                    val absoluteUrl = if (url.startsWith("/")) {
                        "https://www.swr.de$url"
                    } else {
                        url
                    }
                    Log.d(TAG, "Found PDF URL: $absoluteUrl")
                    return absoluteUrl
                }
            }
        }
        
        Log.w(TAG, "No PDF URL found")
        return null
    }
} 