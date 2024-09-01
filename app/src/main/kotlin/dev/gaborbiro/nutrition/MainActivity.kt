package dev.gaborbiro.nutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.feature.home.navigation.HomeNavHost
import dev.gaborbiro.nutrition.core.compose.theme.NutriTheme
import dev.gaborbiro.nutrition.core.navigation.ComposeNavigationDispatcher


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            NutriTheme {
                val navController = rememberNavController()
                val navDispatcher = remember {
                    ComposeNavigationDispatcher(
                        navController
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeNavHost.NAV_ROUTE,
                ) {
                    HomeNavHost.buildGraph(
                        builder = this,
                        navDispatcher = navDispatcher,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
