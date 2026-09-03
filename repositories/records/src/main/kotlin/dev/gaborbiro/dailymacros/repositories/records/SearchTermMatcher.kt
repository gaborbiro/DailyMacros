package dev.gaborbiro.dailymacros.repositories.records

/**
 * Multi-word recall filter: true if [name] or [description] contains ANY of the
 * whitespace-separated words in [searchTerm], case-insensitively - word order and how many
 * words hit don't matter here, this is deliberately a loose pass/fail filter. Ranking
 * full-phrase matches above partial ones is a separate, presentation-layer concern (see
 * OverviewUiMapper.mapSearchResults).
 */
internal fun matchesAnySearchWord(searchTerm: String, name: String, description: String): Boolean {
    val words = searchTerm.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return false
    val haystack = "$name $description".lowercase()
    return words.any { haystack.contains(it) }
}
