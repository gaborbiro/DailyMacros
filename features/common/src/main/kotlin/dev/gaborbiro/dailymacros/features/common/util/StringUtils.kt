fun String?.ellipsize(maxLength: Int): String? {
    val ellipsize = "…"
    return if (this == null || length <= maxLength) {
        this
    } else {
        substring(0, maxLength) + ellipsize
    }
}
