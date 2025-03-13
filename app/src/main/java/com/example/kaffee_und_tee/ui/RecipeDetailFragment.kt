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
    }

    private fun setupObservers() {
        viewModel.selectedRecipe.observe(viewLifecycleOwner) { recipe ->
            recipe?.let {
                binding.txtTitle.text = it.title
                Picasso.get()
                    .load(it.imageUrl)
                    .into(binding.imgRecipe)
                binding.txtIngredients.text = it.ingredients.joinToString("\n")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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