package dev.gaborbiro.nutrition.core.clause

import androidx.annotation.StringRes


sealed class Clause {

    sealed class Text : Clause() {

        data class Plain(
            val text: String
        ) : Text()

        class Localised(
            @StringRes val resId: Int,
            vararg val args: Any = emptyArray()
        ) : Text()
    }

    sealed class Date : Clause() {
        sealed class Timestamp(
            open val timestampMillis: Long,
        ) : Date() {

            /**
             * Example usage:
             * ```
             * Clause.Date.Timestamp.Skeleton(System.currentTimeMillis(), "yMMMd")
             * Clause.Date.Timestamp.Skeleton(System.currentTimeMillis(), "yMMMMd")
             * Clause.Date.Timestamp.Skeleton(System.currentTimeMillis(), "Md")
             * Clause.Date.Timestamp.Skeleton(System.currentTimeMillis(), "Hm")
             * ```
             */
            data class Skeleton(
                override val timestampMillis: Long,
                val skeleton: String,
            ) : Timestamp(timestampMillis)

            data class Pattern(
                override val timestampMillis: Long,
                val pattern: String,
                val timeZoneId: String? = null,
            ) : Timestamp(timestampMillis)
        }
    }

    companion object {
        val empty: Text get() = "".asText()
        val whiteSpace: Text get() = " ".asText()
        val newLine: Text get() = "\n".asText()
    }
}

fun String.asText(): Clause.Text.Plain =
    Clause.Text.Plain(text = this)

fun Int.asText(): Clause.Text.Localised = Clause.Text.Localised(this)