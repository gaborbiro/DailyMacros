package dev.gaborbiro.dailymacros.repositories.records.domain.model

/**
 * Result of [RecordsRepository.retrofillParentTemplateIdsFromSharedImages].
 */
data class RetrofillParentLineageResult(
    /** Rows where [parentTemplateId] was set from shared-image inference. */
    val templatesUpdated: Int,
    /** Templates with null parent but no earliest record time (unexpected). */
    val skippedNoRecordTimestamp: Int,
    /** Had images but no other template referenced the same filename. */
    val skippedNoSharedImageParent: Int,
    /** Shared filenames existed but no candidate parent had strictly earlier first log time. */
    val skippedNoEligibleParent: Int,
)
