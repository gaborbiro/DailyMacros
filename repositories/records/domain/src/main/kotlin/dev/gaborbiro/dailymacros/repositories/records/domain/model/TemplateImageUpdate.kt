package dev.gaborbiro.dailymacros.repositories.records.domain.model

/**
 * One row in [template_images]: backing filename plus optional representative-meal classification
 * from nutrient analysis (null = never classified / leave unchanged when not rewriting images).
 */
data class TemplateImageUpdate(
    val filename: String,
    val isRepresentativeOfMeal: Boolean? = null,
)
