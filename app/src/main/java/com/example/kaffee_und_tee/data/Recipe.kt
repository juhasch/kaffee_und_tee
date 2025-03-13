package com.example.kaffee_und_tee.data

data class Recipe(
    val title: String,
    val imageUrl: String,
    val ingredients: List<String>,
    val videoUrl: String?,
    val webpageUrl: String,
    val pdfUrl: String?
) 