package com.example.kaffee_und_tee.network

import android.util.Log
import com.example.kaffee_und_tee.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class RecipeService {
    private val baseUrl = "https://www.swr.de/video/sendungen-a-z/kaffee-oder-tee/rezepte/rezeptearchiv-100.html"
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
            // Get title from h2 or h3 element, or from the link text if those aren't available
            val title = element.select("h2, h3").firstOrNull()?.text()
                ?: element.select("a").firstOrNull()?.text()
                ?: ""
            
            // Clean up title by removing "Rezepte " prefix if present
            val cleanTitle = title.replace("Rezepte ", "").trim()

            // Get image URL and ensure it's absolute
            val imageUrl = element.select("img").firstOrNull()?.attr("src") ?: ""
            val absoluteImageUrl = if (imageUrl.startsWith("/")) {
                "https://www.swr.de$imageUrl"
            } else {
                imageUrl
            }

            // Get link and ensure it's absolute
            val link = element.select("a").firstOrNull()?.attr("href") ?: ""
            val absoluteLink = if (link.startsWith("/")) {
                "https://www.swr.de$link"
            } else {
                link
            }

            if (cleanTitle.isNotEmpty() && absoluteImageUrl.isNotEmpty() && absoluteLink.isNotEmpty()) {
                val recipe = Recipe(
                    title = cleanTitle,
                    imageUrl = absoluteImageUrl,
                    ingredients = emptyList(),
                    videoUrl = null,
                    webpageUrl = absoluteLink
                )
                Log.d(TAG, "Parsing recipe: title='$cleanTitle', imageUrl='$absoluteImageUrl', webpageUrl='$absoluteLink'")
                recipes.add(recipe)
            } else {
                Log.w(TAG, "Skipping recipe due to missing data: title='$cleanTitle', imageUrl='$absoluteImageUrl', link='$absoluteLink'")
            }
        }

        Log.d(TAG, "Found ${recipes.size} recipes")
        return recipes
    }
    
    suspend fun fetchRecipeDetails(recipe: Recipe): Recipe {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching details for recipe: ${recipe.title}")
                val doc = Jsoup.connect(recipe.webpageUrl)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()
                val ingredients = parseIngredients(doc)
                val videoUrl = parseVideoUrl(doc)
                
                Log.d(TAG, "Found ${ingredients.size} ingredients and video URL: $videoUrl")
                
                recipe.copy(
                    ingredients = ingredients,
                    videoUrl = videoUrl
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
            "ul.ingredients li"
        )
        
        for (selector in selectors) {
            val elements = doc.select(selector)
            Log.d(TAG, "Trying ingredients selector '$selector': found ${elements.size} elements")
            
            if (elements.isNotEmpty()) {
                elements.forEach { element ->
                    try {
                        ingredients.add(element.text())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing ingredient element", e)
                        // Skip this ingredient if there's an error parsing it
                    }
                }
                break // Stop trying other selectors if we found ingredients
            }
        }
        
        return ingredients
    }
    
    private fun parseVideoUrl(doc: Document): String? {
        // Try different selectors for video
        val selectors = listOf(
            "div.video-player source",
            "video source",
            "div[class*='video'] source",
            "div[class*='player'] source",
            "video",
            "iframe[src*='video']"
        )
        
        for (selector in selectors) {
            try {
                val videoElement = doc.select(selector).first()
                val videoUrl = videoElement?.attr("src")
                    ?: videoElement?.attr("data-src")
                Log.d(TAG, "Trying video selector '$selector': found URL: $videoUrl")
                if (videoUrl != null) {
                    return videoUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing video URL with selector '$selector'", e)
            }
        }
        
        return null
    }
} 