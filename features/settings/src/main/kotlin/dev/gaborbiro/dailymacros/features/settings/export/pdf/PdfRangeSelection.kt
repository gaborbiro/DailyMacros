package dev.gaborbiro.dailymacros.features.settings.export.pdf

import java.time.LocalDate

/** What the user chose in the export dialog's range picker. */
sealed interface PdfRangeSelection {
    data class Preset(val preset: DateRangePreset) : PdfRangeSelection
    data class Custom(val from: LocalDate, val to: LocalDate) : PdfRangeSelection
}
