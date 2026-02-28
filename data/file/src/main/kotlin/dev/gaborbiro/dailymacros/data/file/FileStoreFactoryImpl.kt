package dev.gaborbiro.dailymacros.data.file

import android.content.Context
import dev.gaborbiro.dailymacros.data.file.domain.FileStore
import dev.gaborbiro.dailymacros.data.file.domain.FileStoreFactory

class FileStoreFactoryImpl constructor(
    private val appContext: Context,
) : FileStoreFactory {

    override fun getStore(
        folder: String,
        keepFiles: Boolean,
    ): FileStore {
        if (folder.isBlank()) throw IllegalArgumentException("folder cannot be blank")

        val documentProvider = if (keepFiles) {
            FileDestinationProviderPermanent(appContext, folder)
        } else {
            FileDestinationProviderVolatile(appContext, folder)
        }
        return FileStoreImpl(
            context = appContext,
            destinationProvider = documentProvider,
        )
    }
}
