package dev.gaborbiro.dailymacros.data.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class TemplatesDAOTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TemplatesDAO

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            AppDatabase::class.java,
        ).build()
        dao = database.templatesDAO()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun mealWithPhotos(): Long {
        val id = dao.insertOrUpdate(TemplateEntity(name = "Soup", description = "Lentil"))
        dao.insertImages(listOf(ImageEntity(id, "meal.jpg", 0), ImageEntity(id, "label.jpg", 1)))
        return id
    }

    @Test
    fun `replacing photo metadata preserves filenames order and representative flags`() = runBlocking {
        val id = mealWithPhotos()
        dao.replaceImages(
            id,
            listOf(ImageEntity(id, "label.jpg", 0, false), ImageEntity(id, "meal.jpg", 1, true)),
        )
        val images = dao.getImagesForTemplate(id)
        assertEquals(listOf("label.jpg", "meal.jpg"), images.map { it.image })
        assertEquals(listOf(false, true), images.map { it.isRepresentativeMealPhoto })
    }

    @Test
    fun `failed photo replacement rolls back the deletion so the original strip remains intact`() = runBlocking {
        val id = mealWithPhotos()
        val original = dao.getImagesForTemplate(id)
        // Duplicate sort positions violate the unique index after the old photos have been deleted.
        val failure = runCatching {
            dao.replaceImages(id, listOf(ImageEntity(id, "new.jpg", 0), ImageEntity(id, "other.jpg", 0)))
        }.exceptionOrNull()
        assertNotNull(failure)
        assertEquals(original, dao.getImagesForTemplate(id))
    }

    @Test
    fun `intentionally removing every photo still clears the strip`() = runBlocking {
        val id = mealWithPhotos()
        dao.replaceImages(id, emptyList())
        assertTrue(dao.getImagesForTemplate(id).isEmpty())
    }
}
