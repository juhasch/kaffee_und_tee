package com.example.kaffee_und_tee.network

import com.example.kaffee_und_tee.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class RecipeService {
    private val baseUrl = "https://www.swr.de/video/sendungen-a-z/kaffee-oder-tee/rezepte/rezeptearchiv-100.html"
    
    suspend fun fetchRecipes(): List<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(baseUrl)
                    .timeout(10000) // 10 second timeout
                    .get()
                parseRecipes(doc)
            } catch (e: IOException) {
                e.printStackTrace()
                emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun parseRecipes(doc: Document): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        val recipeElements = doc.select("div.teaser")
        
        recipeElements.forEach { element ->
            try {
                val title = element.select("h3").text()
                val imageUrl = element.select("img").attr("src")
                val link = element.select("a").attr("href")
                
                if (title.isNotEmpty() && imageUrl.isNotEmpty() && link.isNotEmpty()) {
                    val fullUrl = if (link.startsWith("http")) link else "https://www.swr.de$link"
                    val recipe = Recipe(
                        title = title,
                        imageUrl = imageUrl,
                        ingredients = emptyList(), // Will be fetched when viewing recipe details
                        videoUrl = null, // Will be fetched when viewing recipe details
                        webpageUrl = fullUrl
                    )
                    recipes.add(recipe)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Skip this recipe if there's an error parsing it
            }
        }
        
        return recipes
    }
    
    suspend fun fetchRecipeDetails(recipe: Recipe): Recipe {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(recipe.webpageUrl)
                    .timeout(10000) // 10 second timeout
                    .get()
                val ingredients = parseIngredients(doc)
                val videoUrl = parseVideoUrl(doc)
                
                recipe.copy(
                    ingredients = ingredients,
                    videoUrl = videoUrl
                )
            } catch (e: IOException) {
                e.printStackTrace()
                recipe
            } catch (e: Exception) {
                e.printStackTrace()
                recipe
            }
        }
    }
    
    private fun parseIngredients(doc: Document): List<String> {
        val ingredients = mutableListOf<String>()
        val ingredientElements = doc.select("div.ingredients ul li")
        
        ingredientElements.forEach { element ->
            try {
                ingredients.add(element.text())
            } catch (e: Exception) {
                e.printStackTrace()
                // Skip this ingredient if there's an error parsing it
            }
        }
        
        return ingredients
    }
    
    private fun parseVideoUrl(doc: Document): String? {
        return try {
            val videoElement = doc.select("div.video-player source").first()
            videoElement?.attr("src")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 