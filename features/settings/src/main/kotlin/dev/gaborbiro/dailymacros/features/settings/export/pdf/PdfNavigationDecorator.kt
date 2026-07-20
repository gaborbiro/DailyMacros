package dev.gaborbiro.dailymacros.features.settings.export.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PageMode
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import java.io.OutputStream

/**
 * Post-processes a [RenderedDiary] (produced by [PdfDiaryRenderer] via Android's PdfDocument) to add
 * the two navigation features mainstream PDF viewers understand but Android's PDF API cannot emit:
 *  - a bookmark outline (one entry per day), and
 *  - clickable links on the table-of-contents lines that jump to each day.
 */
object PdfNavigationDecorator {

    fun writeDecorated(rendered: RenderedDiary, output: OutputStream) {
        PDDocument.load(rendered.bytes).use { document ->
            val outline = PDDocumentOutline()
            document.documentCatalog.documentOutline = outline
            document.documentCatalog.pageMode = PageMode.USE_OUTLINES

            rendered.days.forEach { day ->
                val targetPage = document.getPage(day.targetPageIndex)

                // Bookmark → top of the day's page.
                val bookmarkDest = PDPageFitWidthDestination().apply {
                    page = targetPage
                    top = rendered.pageHeightPt.toInt()
                }
                outline.addLast(PDOutlineItem().apply {
                    title = day.label
                    destination = bookmarkDest
                })

                // Clickable TOC line → same destination. Convert the top-left canvas rect to
                // PDF's bottom-left coordinate space.
                val link = PDAnnotationLink().apply {
                    destination = PDPageFitWidthDestination().apply {
                        page = targetPage
                        top = rendered.pageHeightPt.toInt()
                    }
                    borderStyle = PDBorderStyleDictionary().apply { width = 0f }
                    rectangle = com.tom_roush.pdfbox.pdmodel.common.PDRectangle().apply {
                        lowerLeftX = day.tocRect.left
                        lowerLeftY = rendered.pageHeightPt - day.tocRect.bottom
                        upperRightX = day.tocRect.right
                        upperRightY = rendered.pageHeightPt - day.tocRect.top
                    }
                }
                document.getPage(day.tocPageIndex).annotations.add(link)
            }

            outline.openNode()
            document.save(output)
        }
    }
}
