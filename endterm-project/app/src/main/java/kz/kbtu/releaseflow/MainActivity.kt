package kz.kbtu.releaseflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.kbtu.releaseflow.ui.ReleaseFlowApp
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * The single Activity of the app. On rotation Android destroys and recreates it
 * (there is no `configChanges` override on purpose): the ViewModel, the Navigation back stack
 * and `rememberSaveable` state are what keep the screens consistent across that.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReleaseFlowTheme {
                ReleaseFlowApp()
            }
        }
    }
}
