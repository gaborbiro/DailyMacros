package dev.gaborbiro.nutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.nutrition.prefs.AppPrefsImpl
import dev.gaborbiro.nutrition.prefs.domain.SampleDataItem
import dev.gaborbiro.nutrition.ui.theme.NutriTheme
import dev.gaborbiro.nutrition.ui.theme.Padding
import dev.gaborbiro.nutrition.utils.epochMillisToLocal
import dev.gaborbiro.nutrition.utils.epochMillisToUTC
import dev.gaborbiro.nutrition.utils.toEpochMillis
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val store = remember {
                AppPrefsImpl(context, scope)
            }

            NutriTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize(),
                ) { innerPadding ->
                    val text by store.sampleString.get().collectAsStateWithLifecycle(
                        initialValue = "Android",
                        minActiveState = Lifecycle.State.RESUMED
                    )

                    Content(
                        modifier = Modifier
                            .padding(innerPadding)
                    )
                }
            }

            store.sampleString.set(flowOf("blah"))
            store.sampleString.set("blah2")
            store.sampleInt.set(flowOf(1))
            store.sampleDouble.set(flowOf(null))
            store.sampleObject.set(flowOf(SampleDataItem(sampleValue = "blah")))
            store.sampleBooleanMap.set("key", flowOf(false))
            store.sampleBooleanMap.set("key", true)
            store.sampleObjectMap.set("key", flowOf(SampleDataItem(sampleValue = "blah")))

            println(LocalDateTime.now())
            println(LocalDateTime.now().toEpochMillis())
            println(LocalDateTime.now().toEpochMillis().epochMillisToLocal())
            println(LocalDateTime.now().toEpochMillis().epochMillisToUTC())
        }
    }
}

@Composable
fun Content(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val focuser = remember { FocusRequester() }
        var backingText by remember { mutableStateOf("") }
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding.medium)
                .focusRequester(focuser),
            singleLine = false,
            label = {
                Text(text = "What did you eat today")
            },
            value = backingText,
            onValueChange = {
                backingText = it
            }
        )

        LaunchedEffect(Unit) {
            focuser.requestFocus()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NutriTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Content(Modifier.padding(innerPadding))
        }
    }
}