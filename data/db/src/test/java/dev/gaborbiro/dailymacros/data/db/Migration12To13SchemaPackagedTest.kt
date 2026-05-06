package dev.gaborbiro.dailymacros.data.db

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class Migration12To13SchemaPackagedTest {

    @Test
    fun `exported v12 schema is packaged for unit tests`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.assets.open("schemas/dev.gaborbiro.dailymacros.data.db.AppDatabase/12.json").use {
            assertNotNull(it)
        }
    }
}
