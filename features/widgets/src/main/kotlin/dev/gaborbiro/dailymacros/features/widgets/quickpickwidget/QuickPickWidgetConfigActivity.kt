package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.utils.verticalScrollWithBar
import dev.gaborbiro.dailymacros.features.common.views.LocalImage
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.widgets.PersistenceMapper
import dev.gaborbiro.dailymacros.features.widgets.WidgetUiMapper
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickPickWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var imageStore: ImageStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            AppTheme {
                val viewModel: QuickPickConfigViewModel = hiltViewModel()
                val quickPicks by viewModel.quickPicks.collectAsState()

                CompositionLocalProvider(LocalImageStore provides imageStore) {
                    QuickPickConfigScreen(
                        quickPicks = quickPicks,
                        onSelected = { templateId ->
                            viewModel.selectQuickPick(
                                context = applicationContext,
                                appWidgetId = appWidgetId,
                                templateId = templateId,
                                onDone = {
                                    setResult(
                                        RESULT_OK,
                                        Intent().putExtra(
                                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                                            appWidgetId,
                                        )
                                    )
                                    finish()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@HiltViewModel
class QuickPickConfigViewModel @Inject constructor(
    private val recordsRepository: RecordsRepository,
    private val widgetUiMapper: WidgetUiMapper,
) : ViewModel() {

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    private val _quickPicks = MutableStateFlow<List<ListUiModelQuickPick>>(emptyList())
    val quickPicks: StateFlow<List<ListUiModelQuickPick>> = _quickPicks.asStateFlow()

    init {
        viewModelScope.launch {
            val templates = recordsRepository.getQuickPicks(count = 50)
            _templates.value = templates
            _quickPicks.value = widgetUiMapper.map(templates)
        }
    }

    fun selectQuickPick(
        context: Context,
        appWidgetId: Int,
        templateId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val template = _templates.value.find { it.dbId == templateId }
                ?: recordsRepository.getTemplate(templateId)
            val templateJson = PersistenceMapper.serializeTemplates(listOf(template))

            val manager = GlanceAppWidgetManager(context)
            val glanceId = manager.getGlanceIdBy(appWidgetId)

            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[QuickPickWidgetScreen.templateIdKey(appWidgetId)] = templateId
                prefs[QuickPickWidgetScreen.templateJsonKey(appWidgetId)] = templateJson
            }
            QuickPickWidgetScreen().update(context, glanceId)

            // Schedule a reload worker as a reliable backup for the initial render,
            // since update() above may be a no-op if the widget isn't placed yet.
            WorkManager.getInstance(context).enqueue(QuickPickReloadWorker.getWorkRequest())

            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPickConfigScreen(
    quickPicks: List<ListUiModelQuickPick>,
    onSelected: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Quick Pick") })
        }
    ) { padding ->
        if (quickPicks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No Quick Picks available yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScrollWithBar(),
            ) {
                quickPicks.forEach { item ->
                    QuickPickConfigItem(
                        item = item,
                        onClick = { onSelected(item.templateId) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun QuickPickConfigItem(
    item: ListUiModelQuickPick,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.imageFilename != null) {
            LocalImage(
                name = item.imageFilename,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = item.title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            item.nutrients?.calories?.let { kcal ->
                Text(
                    text = kcal,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
