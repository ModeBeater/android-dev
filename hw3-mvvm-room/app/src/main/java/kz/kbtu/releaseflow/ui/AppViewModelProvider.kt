package kz.kbtu.releaseflow.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.kbtu.releaseflow.ReleaseFlowApplication
import kz.kbtu.releaseflow.ui.screens.AddAppViewModel
import kz.kbtu.releaseflow.ui.screens.EditAppViewModel
import kz.kbtu.releaseflow.ui.screens.EditReleaseViewModel

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
            AddAppViewModel(container().releaseRepository)
        }
        initializer {
            EditAppViewModel(this.createSavedStateHandle(), container().releaseRepository)
        }
        initializer {
            EditReleaseViewModel(this.createSavedStateHandle(), container().releaseRepository)
        }
    }
}

/** The Application that owns the container; `APPLICATION_KEY` is provided by the Activity's ViewModelStore. */
private fun CreationExtras.container() = (this[APPLICATION_KEY] as ReleaseFlowApplication).container
