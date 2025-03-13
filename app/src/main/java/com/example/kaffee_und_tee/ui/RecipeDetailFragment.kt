package com.example.kaffee_und_tee.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.kaffee_und_tee.R
import com.example.kaffee_und_tee.databinding.FragmentRecipeDetailBinding
import com.squareup.picasso.Picasso

class RecipeDetailFragment : Fragment() {
    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by activityViewModels()
    private val args: RecipeDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        
        // Try to find and load the recipe
        val recipe = viewModel.findRecipeByTitle(args.recipeTitle)
        if (recipe != null) {
            viewModel.selectRecipe(recipe)
        } else {
            // If recipe not found in list, try to load recipes first
            viewModel.recipes.value?.let { recipes ->
                if (recipes.isEmpty()) {
                    viewModel.loadRecipes()
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) {
                binding.txtTitle.text = recipe.title
                binding.txtIngredients.text = if (recipe.ingredients.isNotEmpty()) {
                    recipe.ingredients.joinToString("\n") { "• $it" }
                } else {
                    "Keine Zutaten gefunden"
                }
                
                // Load image with error handling
                Picasso.get()
                    .load(recipe.imageUrl)
                    .error(R.drawable.ic_error)
                    .into(binding.imgRecipe)
                
                // Show all views
                binding.txtTitle.visibility = View.VISIBLE
                binding.imgRecipe.visibility = View.VISIBLE
                binding.txtIngredients.visibility = View.VISIBLE
            } else {
                // Hide all views when no recipe is selected
                binding.txtTitle.visibility = View.GONE
                binding.imgRecipe.visibility = View.GONE
                binding.txtIngredients.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.txtTitle.text = "Fehler"
                binding.txtIngredients.text = errorMessage
                binding.imgRecipe.setImageResource(R.drawable.ic_error)
                binding.txtTitle.visibility = View.VISIBLE
                binding.imgRecipe.visibility = View.VISIBLE
                binding.txtIngredients.visibility = View.VISIBLE
            }
        }

        // Observe recipes list to retry loading recipe when list is available
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            if (recipes.isNotEmpty() && viewModel.selectedRecipe.value == null) {
                viewModel.findRecipeByTitle(args.recipeTitle)?.let { recipe ->
                    viewModel.selectRecipe(recipe)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.bottomNavigation.btnHome.setOnClickListener {
            findNavController().navigate(
                RecipeDetailFragmentDirections.actionRecipeDetailToRecipeList()
            )
        }

        binding.bottomNavigation.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.imgRecipe.setOnClickListener {
            viewModel.selectedRecipe.value?.let { recipe ->
                findNavController().navigate(
                    RecipeDetailFragmentDirections.actionRecipeDetailToVideo(recipe.videoUrl ?: "")
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 