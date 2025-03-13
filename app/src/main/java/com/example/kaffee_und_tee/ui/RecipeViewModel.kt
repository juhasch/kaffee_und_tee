package com.example.kaffee_und_tee.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaffee_und_tee.data.Recipe
import com.example.kaffee_und_tee.network.RecipeService
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {
    private val recipeService = RecipeService()
    
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes
    
    private val _selectedRecipe = MutableLiveData<Recipe?>()
    val selectedRecipe: LiveData<Recipe?> = _selectedRecipe
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        loadRecipes()
    }
    
    fun loadRecipes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val recipeList = recipeService.fetchRecipes()
                _recipes.value = recipeList
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load recipes. Please try again later."
                _recipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                // Always fetch fresh details for the recipe
                val detailedRecipe = recipeService.fetchRecipeDetails(recipe)
                _selectedRecipe.value = detailedRecipe
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load recipe details. Please try again later."
                _selectedRecipe.value = null // Don't fall back to basic recipe info
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }
    
    fun retry() {
        loadRecipes()
    }
    
    fun findRecipeByTitle(title: String): Recipe? {
        return _recipes.value?.find { it.title == title }
    }
} 