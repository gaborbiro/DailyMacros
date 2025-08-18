package dev.gaborbiro.dailymacros.repo.records.domain.model

data class Template(
    val dbId: Long,
    val images: List<String>,
    val name: String,
    val description: String,
    val nutrients: Nutrients?,
) {
    val primaryImage: String? get() = images.firstOrNull()
}
