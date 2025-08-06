package dev.gaborbiro.nutri.features.modal.usecase

import android.graphics.Bitmap
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.features.common.BaseUseCase
import dev.gaborbiro.nutri.store.bitmap.BitmapStore

class GetRecordImageUseCase(
    private val repository: RecordsRepository,
    private val bitmapStore: BitmapStore
) : BaseUseCase() {

    suspend fun execute(recordId: Long, thumbnail: Boolean): Bitmap? {
        return repository.getRecord(recordId)!!.template.image
            ?.let {
                bitmapStore.read(it, thumbnail)
            }
    }
}
