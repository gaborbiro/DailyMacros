package dev.gaborbiro.dailymacros.repositories.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Debug/qa/release builds share one Google Drive appDataFolder (their OAuth clients belong to the
 * same GCP project), so the backup filename must be namespaced per variant to stop them clobbering
 * each other, while still falling back to the old shared name for existing users' backups.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CloudSyncRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val namespacedName = "daily_macros_backup_${context.packageName}.tar"
    private val legacyName = "daily_macros_backup.tar"

    private lateinit var requestedUrls: MutableList<String>

    @Before
    fun setUp() {
        requestedUrls = mutableListOf()
    }

    private fun repository(handler: (Request) -> Response): CloudSyncRepositoryImpl {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                requestedUrls += request.url.toString()
                handler(request)
            })
            .build()
        return CloudSyncRepositoryImpl(context, client)
    }

    private fun jsonResponse(request: Request, body: String) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    private fun filesListJson(fileId: String?) = if (fileId == null) {
        """{"files":[]}"""
    } else {
        """{"files":[{"id":"$fileId","name":"unused","modifiedTime":"2026-01-01T00:00:00Z","size":"42"}]}"""
    }

    private fun requestBodyText(request: Request): String {
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        return buffer.readUtf8()
    }

    /** The `q` query parameter, decoded (OkHttp percent-encodes the literal quotes in name='...'). */
    private fun nameQueriedBy(request: Request): String? = request.url.queryParameter("q")

    @Test
    fun `getBackupInfo returns namespaced backup without querying legacy name when found`() = runBlocking {
        val repo = repository { request ->
            assertEquals("name='$namespacedName'", nameQueriedBy(request))
            jsonResponse(request, filesListJson(fileId = "namespaced-id"))
        }

        val info = repo.getBackupInfo("token")

        assertEquals("namespaced-id", info?.fileId)
        assertEquals(1, requestedUrls.size)
    }

    @Test
    fun `getBackupInfo falls back to legacy name when namespaced backup is missing`() = runBlocking {
        val repo = repository { request ->
            val body = if (nameQueriedBy(request) == "name='$namespacedName'") {
                filesListJson(fileId = null)
            } else {
                assertEquals("name='$legacyName'", nameQueriedBy(request))
                filesListJson(fileId = "legacy-id")
            }
            jsonResponse(request, body)
        }

        val info = repo.getBackupInfo("token")

        assertEquals("legacy-id", info?.fileId)
        assertEquals(2, requestedUrls.size)
    }

    @Test
    fun `getBackupInfo returns null when neither namespaced nor legacy backup exists`() = runBlocking {
        val repo = repository { request -> jsonResponse(request, filesListJson(fileId = null)) }

        val info = repo.getBackupInfo("token")

        assertNull(info)
        assertEquals(2, requestedUrls.size)
    }

    @Test
    fun `uploadBackup creates a new namespaced file even when a legacy backup exists`() = runBlocking {
        var createRequestBody: String? = null
        val repo = repository { request ->
            when {
                nameQueriedBy(request) == "name='$namespacedName'" ->
                    jsonResponse(request, filesListJson(fileId = null))

                request.method == "POST" -> {
                    createRequestBody = requestBodyText(request)
                    jsonResponse(request, """{"id":"new-id","modifiedTime":"2026-01-01T00:00:00Z","size":"10"}""")
                }

                else -> throw AssertionError("unexpected request: ${request.method} ${request.url}")
            }
        }

        val tarFile = File.createTempFile("backup", ".tar").apply { writeText("data") }
        val result = repo.uploadBackup("token", tarFile)

        assertEquals("new-id", result.fileId)
        assertTrue(createRequestBody.orEmpty().contains(namespacedName))
        // Only the namespaced-name lookup happened before creating - the legacy file was never queried or touched.
        assertEquals(2, requestedUrls.size)
    }

    @Test
    fun `uploadBackup updates the existing namespaced file in place`() = runBlocking {
        val repo = repository { request ->
            when {
                nameQueriedBy(request) == "name='$namespacedName'" ->
                    jsonResponse(request, filesListJson(fileId = "existing-id"))

                request.method == "PATCH" -> {
                    assertTrue(request.url.toString().contains("/existing-id"))
                    jsonResponse(request, """{"id":"existing-id","modifiedTime":"2026-01-02T00:00:00Z","size":"20"}""")
                }

                else -> throw AssertionError("unexpected request: ${request.method} ${request.url}")
            }
        }

        val tarFile = File.createTempFile("backup", ".tar").apply { writeText("data") }
        val result = repo.uploadBackup("token", tarFile)

        assertEquals("existing-id", result.fileId)
        assertEquals(2, requestedUrls.size)
    }
}
