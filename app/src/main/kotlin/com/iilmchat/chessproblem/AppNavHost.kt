package com.iilmchat.chessproblem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.Level
import com.iilmchat.chessproblem.ui.components.ChessBottomBar
import com.iilmchat.chessproblem.ui.screens.AboutScreen
import com.iilmchat.chessproblem.ui.screens.ChapterScreen
import com.iilmchat.chessproblem.ui.screens.HomeScreen
import com.iilmchat.chessproblem.ui.screens.LevelScreen
import com.iilmchat.chessproblem.ui.screens.ProgressScreen
import com.iilmchat.chessproblem.ui.screens.PuzzleScreen
import com.iilmchat.chessproblem.ui.screens.SettingsScreen
import com.iilmchat.chessproblem.ui.viewmodel.CatalogViewModel
import com.iilmchat.chessproblem.ui.viewmodel.PuzzleViewModel
import com.iilmchat.chessproblem.ui.viewmodel.SettingsViewModel

@Composable
fun AppNavHost(
    container: AppContainer,
    catalogViewModel: CatalogViewModel,
    settingsViewModel: SettingsViewModel,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    val catalog by catalogViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route == null || route in setOf("home", "progress", "settings")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                ChessBottomBar(
                    currentRoute = route,
                    onNavigate = { target ->
                        navController.navigate(target) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    state = catalog,
                    onPuzzleClick = { puzzleId -> navController.navigate("puzzle/$puzzleId") },
                    onLevelClick = { level -> navController.navigate("level/${level.id}") },
                    onProgressClick = {
                        navController.navigate("progress") { launchSingleTop = true }
                    },
                    onSettingsClick = {
                        navController.navigate("settings") { launchSingleTop = true }
                    },
                    onRetry = catalogViewModel::load,
                )
            }

            composable("progress") {
                ProgressScreen(
                    state = catalog,
                    onPuzzleClick = { puzzleId -> navController.navigate("puzzle/$puzzleId") },
                    onPlay = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRetry = catalogViewModel::load,
                )
            }

            composable("settings") {
                SettingsScreen(
                    state = settings,
                    appVersion = BuildConfig.VERSION_NAME,
                    onLanguage = onLanguageChanged,
                    onTheme = settingsViewModel::setTheme,
                    onBoardTheme = settingsViewModel::setBoardTheme,
                    onCoordinates = settingsViewModel::setCoordinates,
                    onAutoFlip = settingsViewModel::setAutoFlip,
                    onReduceMotion = settingsViewModel::setReduceMotion,
                    onSound = settingsViewModel::setSound,
                    onHaptics = settingsViewModel::setHaptics,
                    onResetProgress = settingsViewModel::resetProgress,
                    onResetDone = settingsViewModel::clearResetDone,
                    onAbout = { navController.navigate("about") },
                )
            }

            composable("about") {
                AboutScreen(
                    appVersion = BuildConfig.VERSION_NAME,
                    contentVersion = container.contentVersionProvider.current(),
                    onBack = { navController.popBackStack() },
                )
            }

            composable("level/{level}") { entry ->
                val level = entry.arguments?.getString("level")
                    ?.let { value -> runCatching { Level.fromId(value) }.getOrNull() }
                if (level == null) {
                    Text(
                        text = "Unknown level",
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    LevelScreen(
                        level = level,
                        state = catalog,
                        onBack = { navController.popBackStack() },
                        onChapterClick = { chapter ->
                            navController.navigate("chapter/${level.id}/$chapter")
                        },
                        onRetry = catalogViewModel::load,
                    )
                }
            }

            composable("chapter/{level}/{chapter}") { entry ->
                val level = entry.arguments?.getString("level")
                    ?.let { value -> runCatching { Level.fromId(value) }.getOrNull() }
                val chapter = entry.arguments?.getString("chapter")
                if (level == null || chapter == null) {
                    Text(
                        text = "Unknown chapter",
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    ChapterScreen(
                        level = level,
                        chapterId = chapter,
                        state = catalog,
                        onBack = { navController.popBackStack() },
                        onPuzzleClick = { puzzleId -> navController.navigate("puzzle/$puzzleId") },
                        onRetry = catalogViewModel::load,
                    )
                }
            }

            composable("puzzle/{puzzleId}") { entry ->
                val puzzleId = entry.arguments?.getString("puzzleId") ?: return@composable
                val puzzle = catalog.puzzles.firstOrNull { it.id == puzzleId }
                val nextPuzzleId = puzzle?.let { catalog.nextPuzzleAfter(it)?.id }
                val puzzleViewModel: PuzzleViewModel = viewModel(
                    key = "puzzle_$puzzleId",
                    factory = AppViewModelFactory { savedStateHandle ->
                        PuzzleViewModel(
                            puzzleId = puzzleId,
                            savedStateHandle = savedStateHandle,
                            puzzleRepository = container.puzzleRepository,
                            progressRepository = container.progressRepository,
                            hintRepository = container.hintRepository,
                            settingsRepository = container.settingsRepository,
                            rules = container.rules,
                        )
                    },
                )

                PuzzleScreen(
                    viewModel = puzzleViewModel,
                    nextPuzzleId = nextPuzzleId,
                    onBack = { navController.popBackStack() },
                    onNext = { nextId ->
                        val currentId = navController.currentDestination?.id
                        navController.navigate("puzzle/$nextId") {
                            if (currentId != null) {
                                popUpTo(currentId) { inclusive = true }
                            }
                        }
                    },
                    onHome = {
                        if (!navController.popBackStack("home", inclusive = false)) {
                            navController.navigate("home") {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
        }
    }
}
