package com.mysecondrain

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mysecondrain.presentation.navigation.*
import com.mysecondrain.presentation.theme.MySecondBrainTheme
import com.mysecondrain.presentation.ui.splash.SplashScreen
import com.mysecondrain.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Activity তৈরি হওয়ার আগেই সঠিক locale apply করে — recreate() করলে এটা আবার call হয়
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "English") ?: "English"
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode   by rememberSaveable { mutableStateOf(false) }
            var showSplash   by rememberSaveable { mutableStateOf(true) }

            MySecondBrainTheme(darkTheme = isDarkMode) {
                AnimatedContent(
                    targetState   = showSplash,
                    transitionSpec = {
                        fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                    },
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        MainScreen(
                            isDarkMode   = isDarkMode,
                            onToggleDark = { isDarkMode = !isDarkMode }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onToggleDark: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    val bottomNavRoutes = bottomNavItems.map { it.screen.route }.toSet()
    val showBottomNav   = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = {
                                Icon(
                                    imageVector        = if (isSelected) item.selectedIcon
                                    else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label           = { Text(item.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            paddingValues = paddingValues,
            isDarkMode    = isDarkMode,
            onToggleDark  = onToggleDark
        )
    }
}