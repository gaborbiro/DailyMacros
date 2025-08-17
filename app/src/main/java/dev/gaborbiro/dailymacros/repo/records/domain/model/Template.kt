package dev.gaborbiro.dailymacros.repo.records.domain.model

data class Template(
    val id: Long,
    val image: String?,
    val name: String,
    val description: String,
    val nutrients: Nutrients?,
)
