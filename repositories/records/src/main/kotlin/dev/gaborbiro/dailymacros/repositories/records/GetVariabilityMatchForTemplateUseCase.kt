package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.buildTemplateVariabilityPreviewLines

/**
 * Produces a multi-line summary of variability archetypes/slots/variants linked to [templateId]
 * via variant evidence [templateId] fields.
 */
class GetVariabilityMatchForTemplateUseCase(
    private val variabilityRepository: VariabilityRepository,
    private val profileMapper: VariabilityProfileMapper,
) {

    suspend fun execute(templateId: Long): String {
        val snapshot = variabilityRepository.getLatestProfile()
            ?: return "No variability profile is loaded yet. Run meal variability mining from Settings to see archetypes here."
        val profile = profileMapper.parseProfileJson(
            profileJson = snapshot.profileJson,
            minedAtEpochMs = snapshot.minedAtEpochMs,
        )
        val lines = buildTemplateVariabilityPreviewLines(profile.archetypes, templateId)
        return if (lines.isEmpty()) {
            "No archetypes include this template in variant evidence yet. After the next mine, rows that reference this template id will appear here."
        } else {
            lines.joinToString("\n")
        }
    }
}
