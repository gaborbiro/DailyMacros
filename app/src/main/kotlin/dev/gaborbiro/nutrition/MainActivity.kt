package dev.gaborbiro.nutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.gaborbiro.feature.home.HomeScreenNavHost
import dev.gaborbiro.nutrition.core.navigation.ComposeNavigationDispatcher
import dev.gaborbiro.nutrition.prefs.AppPrefsImpl
import dev.gaborbiro.nutrition.prefs.domain.SampleDataItem
import dev.gaborbiro.nutrition.core.compose.theme.NutriTheme
import kotlinx.coroutines.flow.flowOf

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

                    val navController = rememberNavController()
                    val navDispatcher = remember {
                        ComposeNavigationDispatcher(
                            navController
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = HomeScreenNavHost.NAV_ROUTE,
                    ) {
                        HomeScreenNavHost.buildGraph(
                            this,
                            navDispatcher,
                            Modifier.padding(innerPadding)
                        )
                    }
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
        }
    }
}
