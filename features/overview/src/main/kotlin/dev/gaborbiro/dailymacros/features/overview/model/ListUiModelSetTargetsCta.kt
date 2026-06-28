package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase

internal data class ListUiModelSetTargetsCta(
    override val listItemId: Long,
) : ListUiModelBase(listItemId = listItemId, contentType = "set targets cta")
