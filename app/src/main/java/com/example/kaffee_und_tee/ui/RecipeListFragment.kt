package com.example.kaffee_und_tee.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kaffee_und_tee.R
import com.example.kaffee_und_tee.databinding.FragmentRecipeListBinding
import com.example.kaffee_und_tee.ui.RecipeViewModel
import kotlin.math.max

class RecipeListFragment : Fragment() {
    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter { recipe ->
            viewModel.selectRecipe(recipe)
            findNavController().navigate(
                RecipeListFragmentDirections.actionRecipeListToRecipeDetail(recipe.title)
            )
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecipeListFragment.adapter
            setHasFixedSize(true)
            // Remove any item decoration that might add spacing
            if (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }
    }

    private fun setupObservers() {
        viewModel.recipes.observe(viewLifecycleOwner, Observer { recipes ->
            adapter.submitList(recipes)
            updateEmptyState(recipes.isEmpty())
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.errorContainer.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                showError(errorMessage)
            } else {
                hideError()
            }
        })
    }

    private fun setupClickListeners() {
        binding.bottomNavigation.btnHome.setOnClickListener {
            // Already on home screen
        }

        binding.errorContainer.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            viewModel.retry()
        }
    }

    private fun showError(message: String) {
        binding.errorContainer.apply {
            visibility = View.VISIBLE
            findViewById<TextView>(R.id.txtErrorMessage).text = message
        }
        binding.recyclerView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun hideError() {
        binding.errorContainer.visibility = View.GONE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 