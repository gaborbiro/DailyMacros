package dev.gaborbiro.dailymacros.features.settings.export.pdf

import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

/**
 * Validates the PdfBox post-processing that Android's PdfDocument cannot do itself: a bookmark
 * outline (one per day) and clickable TOC links, including the top-left → bottom-left coordinate
 * flip. The Canvas renderer needs a real device (Robolectric can't back PdfDocument), so this feeds
 * the decorator a PdfBox-built stand-in PDF with known page geometry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class PdfNavigationDecoratorTest {

    private val pageHeight = PDRectangle.A4.height

    @Before
    fun setUp() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    private fun blankPdf(pageCount: Int): ByteArray {
        val doc = PDDocument()
        repeat(pageCount) { doc.addPage(PDPage(PDRectangle.A4)) }
        val out = ByteArrayOutputStream()
        doc.save(out)
        doc.close()
        return out.toByteArray()
    }

    @Test
    fun `adds one bookmark and one toc link per day, with a flipped link rectangle`() {
        // 1 TOC page (index 0) + 3 day pages (indices 1..3).
        val rendered = RenderedDiary(
            bytes = blankPdf(pageCount = 4),
            pageHeightPt = pageHeight,
            days = listOf(
                PdfDayNav("Mon 14 Jul", tocPageIndex = 0, tocRect = RectF(40f, 100f, 200f, 116f), targetPageIndex = 1),
                PdfDayNav("Tue 15 Jul", tocPageIndex = 0, tocRect = RectF(40f, 120f, 200f, 136f), targetPageIndex = 2),
                PdfDayNav("Wed 16 Jul", tocPageIndex = 0, tocRect = RectF(40f, 140f, 200f, 156f), targetPageIndex = 3),
            ),
        )

        val out = ByteArrayOutputStream()
        PdfNavigationDecorator.writeDecorated(rendered, out)

        PDDocument.load(out.toByteArray()).use { doc ->
            val bookmarkTitles = generateSequence(doc.documentCatalog.documentOutline.firstChild) { it.nextSibling }
                .map { it.title }
                .toList()
            assertEquals(listOf("Mon 14 Jul", "Tue 15 Jul", "Wed 16 Jul"), bookmarkTitles)

            val links = doc.getPage(0).annotations
            assertEquals(3, links.size)

            // Top-left canvas rect (100..116 from the top) maps to bottom-left PDF coordinates.
            val rect = (links.first() as PDAnnotationLink).rectangle
            assertEquals(40f, rect.lowerLeftX, 0.5f)
            assertEquals(200f, rect.upperRightX, 0.5f)
            assertEquals(pageHeight - 116f, rect.lowerLeftY, 0.5f)
            assertEquals(pageHeight - 100f, rect.upperRightY, 0.5f)
            assertTrue("link rect should have positive height", rect.upperRightY > rect.lowerLeftY)
        }
    }
}
