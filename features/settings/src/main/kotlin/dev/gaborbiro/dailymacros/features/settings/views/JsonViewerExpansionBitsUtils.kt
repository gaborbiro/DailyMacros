package dev.gaborbiro.dailymacros.features.settings.views

/**
 * Preorder container path list index ↔ one char in [bits] ('1' = expanded, '0' = collapsed).
 * Shorter [bits] pads with '0'; extra chars are ignored when reading.
 */
internal fun isPathExpandedInBits(containerPaths: List<String>, bits: String, path: String): Boolean {
    val i = containerPaths.indexOf(path)
    if (i < 0) return false
    return i < bits.length && bits[i] == '1'
}

internal fun flipExpansionBit(containerPaths: List<String>, bits: String, path: String): String {
    val n = containerPaths.size
    val padded = bits.padEnd(n, '0').toCharArray()
    val i = containerPaths.indexOf(path)
    if (i < 0 || i >= n) return String(padded)
    padded[i] = if (padded[i] == '1') '0' else '1'
    return String(padded)
}

internal fun allExpandedBits(containerPaths: List<String>): String =
    "1".repeat(containerPaths.size)

internal fun allCollapsedBits(containerPaths: List<String>): String =
    "0".repeat(containerPaths.size)
