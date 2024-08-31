package dev.gaborbiro.nutrition.core.clause

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.ZoneId
import java.util.Locale


class ClauseParser(
    private val localeProvider: LocaleProvider,
    private val dateFormatter: DateFormatter,
) {

    @Composable
    fun parse(clause: Clause): String {
        return when (clause) {
            is Clause.Text -> parseText(clause)
            is Clause.Date -> parseDate(clause)
        }
    }

    @Composable
    private fun parseText(clause: Clause.Text): String {
        return when (clause) {
            is Clause.Text.Plain -> parsePlain(clause)
            is Clause.Text.Localised -> parseLocalized(clause)
        }
    }

    @Composable
    private fun parseLocalized(clause: Clause.Text.Localised): String {
        val resolvedArgs = clause.args.map {
            if (it is Clause) {
                it.resolve()
            } else {
                it
            }
        }
        return stringResource(id = clause.resId, *resolvedArgs.toTypedArray())
    }

    @Composable
    private fun parsePlain(clause: Clause.Text.Plain): String {
        return clause.text
    }

    @Composable
    private fun parseDate(clause: Clause.Date): String {
        return when (clause) {
            is Clause.Date.Timestamp -> remember(clause) { parseTimestamp(clause) }
        }
    }

    private fun parseTimestamp(clause: Clause.Date.Timestamp): String {
        return when (clause) {
            is Clause.Date.Timestamp.Pattern -> parseExplicit(clause)
            is Clause.Date.Timestamp.Skeleton -> parseSkeleton(clause)
        }
    }

    private fun parseExplicit(clause: Clause.Date.Timestamp.Pattern): String {
        return dateFormatter.format(
            timestampMillis = clause.timestampMillis,
            zoneId = ZoneId.of(clause.timeZoneId),
            pattern = clause.pattern
        )
    }

    private fun parseSkeleton(clause: Clause.Date.Timestamp.Skeleton): String {
        return dateFormatter.format(
            timestampMillis = clause.timestampMillis,
            zoneId = ZoneId.systemDefault(),
            pattern = DateFormat.getBestDateTimePattern(localeProvider.getLocale(), clause.skeleton)
        )
    }
}

val clauseParser = ClauseParser(
    localeProvider = object : LocaleProvider {
        override fun getLocale() = Locale.getDefault()
    },
    dateFormatter = DateFormatterImpl(
        localeProvider = object : LocaleProvider {
            override fun getLocale() = Locale.getDefault()
        }
    )
)

@Composable
fun Clause.resolve() = clauseParser.parse(this)

interface LocaleProvider {
    fun getLocale(): Locale
}