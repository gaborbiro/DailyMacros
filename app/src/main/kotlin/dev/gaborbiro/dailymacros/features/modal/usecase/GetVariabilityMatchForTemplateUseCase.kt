package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent

/**
 * Loads mined variability for [templateId] (evidence match) for the quick-add preview dialog.
 */
internal class GetVariabilityMatchForTemplateUseCase(
    private val variabilityRepository: VariabilityRepository,
    private val profileMapper: VariabilityProfileMapper,
    private val previewMapper: TemplateVariabilityPreviewMapper = TemplateVariabilityPreviewMapper(),
) {

    suspend fun execute(templateId: Long): TemplateVariabilityPreviewContent {
        val snapshot = variabilityRepository.getLatestProfile()
            ?: return TemplateVariabilityPreviewContent(
                bannerText = "No variability profile is loaded yet. Run meal variability mining from Settings to see variants here.",
                slots = emptyList(),
            )
        val profile = profileMapper.parseProfileJson(
            profileJson = snapshot.profileJson,
            minedAtEpochMs = snapshot.minedAtEpochMs,
        )
        val slots = previewMapper.slotPreviewsForTemplate(profile.archetypes, templateId)
        return if (slots.isEmpty()) {
            TemplateVariabilityPreviewContent(
                bannerText = "No slots in the profile cite this template yet. After the next mine, variants that reference this template id will appear here.",
                slots = emptyList(),
            )
        } else {
            TemplateVariabilityPreviewContent(
                bannerText = "Pick a variant per slot (demo — not saved yet).",
                slots = slots,
            )
        }
    }
}
