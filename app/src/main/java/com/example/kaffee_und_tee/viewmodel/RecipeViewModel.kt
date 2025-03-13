package com.example.kaffee_und_tee.viewmodel

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
    
    private val _selectedRecipe = MutableLiveData<Recipe>()
    val selectedRecipe: LiveData<Recipe> = _selectedRecipe
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        loadRecipes()
    }
    
    fun loadRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _recipes.value = recipeService.fetchRecipes()
            _isLoading.value = false
        }
    }
    
    fun selectRecipe(recipe: Recipe) {
        viewModelScope.launch {
            _isLoading.value = true
            val detailedRecipe = recipeService.fetchRecipeDetails(recipe)
            _selectedRecipe.value = detailedRecipe
            _isLoading.value = false
        }
    }
} 