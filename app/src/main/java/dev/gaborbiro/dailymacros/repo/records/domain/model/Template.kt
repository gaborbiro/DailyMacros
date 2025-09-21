package dev.gaborbiro.dailymacros.repo.records.domain.model

data class Template(
    val dbId: Long,
    val images: List<String>,
    val name: String,
    val description: String,
    val isPending: Boolean,
    val macros: Macros?,
) {
    val primaryImage: String? get() = images.firstOrNull()
}
