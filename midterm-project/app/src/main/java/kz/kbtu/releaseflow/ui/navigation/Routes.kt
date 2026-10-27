package kz.kbtu.releaseflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All routes of the navigation graph in one place. Parameterised routes use the
 * `{argument}` placeholder syntax of Navigation Compose; the helpers build concrete routes.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val APPS = "apps"
    const val STATS = "stats"

    const val APP_ID_ARG = "appId"
    const val APP = "app/{$APP_ID_ARG}"
    fun app(appId: Int) = "app/$appId"

    const val NEW_RELEASE = "app/{$APP_ID_ARG}/new"
    fun newRelease(appId: Int) = "app/$appId/new"

    const val RELEASE_ID_ARG = "releaseId"
    const val RELEASE = "release/{$RELEASE_ID_ARG}"
    fun release(releaseId: Int) = "release/$releaseId"

    const val CHECKLIST = "release/{$RELEASE_ID_ARG}/checklist"
    fun checklist(releaseId: Int) = "release/$releaseId/checklist"
}

/** Destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(val route: String, val title: String, val icon: ImageVector) {
    Dashboard(Routes.DASHBOARD, "Overview", Icons.Default.Home),
    Apps(Routes.APPS, "Apps", Icons.AutoMirrored.Filled.List),
    Stats(Routes.STATS, "Stats", Icons.Default.Info),
}
