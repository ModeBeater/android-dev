package kz.kbtu.releaseflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kz.kbtu.releaseflow.ui.components.AppSnackbarHost
import kz.kbtu.releaseflow.ui.components.AppTopBar
import kz.kbtu.releaseflow.ui.components.BarItem
import kz.kbtu.releaseflow.ui.components.FloatingTabBar
import kz.kbtu.releaseflow.ui.navigation.Routes
import kz.kbtu.releaseflow.ui.navigation.TopLevelDestination
import kz.kbtu.releaseflow.ui.screens.AddAppScreen
import kz.kbtu.releaseflow.ui.screens.AppDetailsScreen
import kz.kbtu.releaseflow.ui.screens.AppsScreen
import kz.kbtu.releaseflow.ui.screens.BuildLogScreen
import kz.kbtu.releaseflow.ui.screens.ChecklistScreen
import kz.kbtu.releaseflow.ui.screens.DashboardScreen
import kz.kbtu.releaseflow.ui.screens.GitHubReleasesScreen
import kz.kbtu.releaseflow.ui.screens.NewReleaseScreen
import kz.kbtu.releaseflow.ui.screens.ReleaseScreen
import kz.kbtu.releaseflow.ui.screens.SettingsScreen
import kz.kbtu.releaseflow.ui.screens.StatsScreen
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * Root of the UI: one Scaffold (top bar, floating tab bar, snackbar) around a NavHost.
 *
 * - The shared [ReleaseViewModel] is created here, above the NavHost, with the app's
 *   factory, so it is scoped to the Activity and survives rotation; screens that have
 *   network state of their own get a ViewModel scoped to their back-stack entry.
 * - `rememberNavController()` keeps the back stack across configuration changes.
 * - Screens receive state and callbacks; navigation calls live only in this file.
 * - The tab bar floats over the content: screens add their own bottom padding ([PageListPadding])
 *   instead of taking the Scaffold's, so lists scroll underneath the bar.
 */
@Composable
fun ReleaseFlowApp(viewModel: ReleaseViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = ReleaseFlowTheme.colors

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }

    // One-shot messages: show, then acknowledge exactly this message (also when cancelled by rotation).
    LaunchedEffect(state.message?.id) {
        val message = state.message ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(message.text)
        } finally {
            viewModel.onMessageShown(message.id)
        }
    }

    // One-shot navigation requested by the ViewModel (after a "create" that had to wait for the database).
    LaunchedEffect(state.navigation?.id) {
        val event = state.navigation ?: return@LaunchedEffect
        navController.navigate(event.route) {
            event.popUpTo?.let { popUpTo(it) { inclusive = true } }
            launchSingleTop = true
        }
        viewModel.onNavigated(event.id)
    }

    val appIdArg = backStackEntry?.arguments?.getInt(Routes.APP_ID_ARG)
    val releaseIdArg = backStackEntry?.arguments?.getInt(Routes.RELEASE_ID_ARG)
    val releaseArg = releaseIdArg?.let { state.release(it) }
    val appArg = appIdArg?.let { state.app(it) }
    val title: String = when (currentRoute) {
        Routes.DASHBOARD -> "Overview"
        Routes.APPS -> "Apps"
        Routes.STATS -> "Statistics"
        Routes.SETTINGS -> "Settings"
        Routes.ADD_APP -> "New app"
        Routes.APP -> appArg?.name ?: "App"
        Routes.NEW_RELEASE -> "New release"
        Routes.GITHUB -> "GitHub releases"
        Routes.RELEASE -> releaseArg?.version ?: "Release"
        Routes.CHECKLIST -> "Checklist"
        Routes.BUILD_LOG -> "Build log"
        else -> "ReleaseFlow"
    }
    // Where the screen came from — shown as a quiet line under the title on detail screens.
    val subtitle: String? = when (currentRoute) {
        Routes.ADD_APP -> "Apps"
        Routes.APP -> "Apps"
        Routes.NEW_RELEASE -> appArg?.name ?: "App"
        Routes.GITHUB -> appArg?.githubRepo ?: appArg?.name ?: "App"
        Routes.RELEASE -> releaseArg?.appName ?: "Release"
        Routes.CHECKLIST -> releaseArg?.let { "${it.appName} ${it.version}" } ?: "Release"
        Routes.BUILD_LOG -> releaseArg?.let { "${it.appName} ${it.version}" } ?: "Release"
        else -> null
    }

    Scaffold(
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        // Insets are handled where they matter (top bar, floating bar, FABs), not by the Scaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = title,
                subtitle = subtitle,
                showBack = !isTopLevel,
                onBack = { navController.navigateUp() },
            )
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (!state.loaded) {
                // The database is being opened (and seeded on the first launch).
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                    strokeWidth = 2.dp,
                    color = colors.accent,
                )
            }

            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        state = state,
                        onOpenRelease = { id -> navController.navigate(Routes.release(id)) },
                        onOpenApp = { id -> navController.navigate(Routes.app(id)) },
                        onOpenApps = { navController.navigateTopLevel(Routes.APPS) },
                        onOpenStats = { navController.navigateTopLevel(Routes.STATS) },
                    )
                }

                composable(Routes.APPS) {
                    AppsScreen(
                        state = state,
                        onOpenApp = { id -> navController.navigate(Routes.app(id)) },
                        onAddApp = { navController.navigate(Routes.ADD_APP) },
                    )
                }

                composable(Routes.STATS) {
                    StatsScreen(state = state)
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(onShowMessage = { text -> snackbarHostState.showSnackbar(text) })
                }

                composable(Routes.ADD_APP) {
                    AddAppScreen(
                        onCreated = { id ->
                            // Replace the form with the new app; Back goes to the list, not to the form.
                            navController.navigate(Routes.app(id)) {
                                popUpTo(Routes.ADD_APP) { inclusive = true }
                            }
                        },
                    )
                }

                composable(
                    route = Routes.APP,
                    arguments = listOf(navArgument(Routes.APP_ID_ARG) { type = NavType.IntType }),
                ) { entry ->
                    val appId = entry.arguments?.getInt(Routes.APP_ID_ARG) ?: return@composable
                    AppDetailsScreen(
                        state = state,
                        appId = appId,
                        onOpenRelease = { id -> navController.navigate(Routes.release(id)) },
                        onNewRelease = { navController.navigate(Routes.newRelease(appId)) },
                        onOpenGithub = { navController.navigate(Routes.github(appId)) },
                        onDeleted = {
                            // Back to the list when it is on the stack (Apps tab), otherwise just back.
                            if (!navController.popBackStack(Routes.APPS, inclusive = false)) navController.popBackStack()
                        },
                    )
                }

                composable(
                    route = Routes.GITHUB,
                    arguments = listOf(navArgument(Routes.APP_ID_ARG) { type = NavType.IntType }),
                ) {
                    GitHubReleasesScreen(
                        state = state,
                        onOpenRelease = { id -> navController.navigate(Routes.release(id)) },
                    )
                }

                composable(
                    route = Routes.NEW_RELEASE,
                    arguments = listOf(navArgument(Routes.APP_ID_ARG) { type = NavType.IntType }),
                ) { entry ->
                    val appId = entry.arguments?.getInt(Routes.APP_ID_ARG) ?: return@composable
                    NewReleaseScreen(
                        state = state,
                        appId = appId,
                        onCreate = { version, builds, plannedAt, notes ->
                            viewModel.createRelease(appId, version, builds, plannedAt, notes)
                        },
                    )
                }

                composable(
                    route = Routes.RELEASE,
                    arguments = listOf(navArgument(Routes.RELEASE_ID_ARG) { type = NavType.IntType }),
                ) { entry ->
                    val releaseId = entry.arguments?.getInt(Routes.RELEASE_ID_ARG) ?: return@composable
                    ReleaseScreen(
                        state = state,
                        releaseId = releaseId,
                        onAction = { platform, action, percent -> viewModel.runAction(releaseId, platform, action, percent) },
                        onOpenChecklist = { navController.navigate(Routes.checklist(releaseId)) },
                        onOpenLog = { platform -> navController.navigate(Routes.buildLog(releaseId, platform)) },
                        onOpenApp = {
                            state.release(releaseId)?.let { navController.navigate(Routes.app(it.appId)) }
                        },
                    )
                }

                composable(
                    route = Routes.CHECKLIST,
                    arguments = listOf(navArgument(Routes.RELEASE_ID_ARG) { type = NavType.IntType }),
                ) { entry ->
                    val releaseId = entry.arguments?.getInt(Routes.RELEASE_ID_ARG) ?: return@composable
                    ChecklistScreen(
                        state = state,
                        releaseId = releaseId,
                        onToggle = { itemId -> viewModel.toggleChecklistItem(releaseId, itemId) },
                        onAdd = { title, platform -> viewModel.addChecklistItem(releaseId, title, platform) },
                    )
                }

                composable(
                    route = Routes.BUILD_LOG,
                    arguments = listOf(
                        navArgument(Routes.RELEASE_ID_ARG) { type = NavType.IntType },
                        navArgument(Routes.PLATFORM_ARG) { type = NavType.StringType },
                    ),
                ) {
                    BuildLogScreen()
                }
            }

            if (isTopLevel) {
                FloatingTabBar(
                    items = TopLevelDestination.entries.map { BarItem(it.route, it.title, it.icon) },
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.navigateTopLevel(route) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * Standard tab navigation: pop to the start destination (saving its state),
 * avoid duplicate copies of the same screen, restore the tab's previous state.
 */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
