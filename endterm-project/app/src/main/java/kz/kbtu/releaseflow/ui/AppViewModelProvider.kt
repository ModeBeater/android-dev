package kz.kbtu.releaseflow.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kbtu.releaseflow.ReleaseFlowApplication
import kz.kbtu.releaseflow.ui.screens.AddAppViewModel
import kz.kbtu.releaseflow.ui.screens.AppDetailsViewModel
import kz.kbtu.releaseflow.ui.screens.BuildLogViewModel
import kz.kbtu.releaseflow.ui.screens.GitHubReleasesViewModel
import kz.kbtu.releaseflow.ui.screens.SettingsViewModel

/**
 * The factory every screen uses: `viewModel(factory = AppViewModelProvider.Factory)`.
 * Each initializer pulls its dependencies from the [ReleaseFlowApplication]'s container and,
 * where a screen has an argument, a `SavedStateHandle` that already holds the route arguments.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ReleaseViewModel(container().releaseRepository)
        }
        initializer {
            AddAppViewModel(container().releaseRepository, container().storeRepository, container().settingsRepository)
        }
        initializer {
            AppDetailsViewModel(
                this.createSavedStateHandle(),
                container().releaseRepository,
                container().storeRepository,
                container().settingsRepository,
            )
        }
        initializer {
            GitHubReleasesViewModel(this.createSavedStateHandle(), container().releaseRepository, container().storeRepository)
        }
        initializer {
            BuildLogViewModel(this.createSavedStateHandle(), container().releaseRepository, container().buildLogSocket)
        }
        initializer {
            SettingsViewModel(container().settingsRepository, container().syncScheduler, container().releaseRepository)
        }
    }
}

/** The Application that owns the container; `APPLICATION_KEY` is provided by the Activity's ViewModelStore. */
private fun CreationExtras.container() = (this[APPLICATION_KEY] as ReleaseFlowApplication).container
