package kz.kbtu.furnistore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.kbtu.furnistore.ui.FurniStoreApp
import kz.kbtu.furnistore.ui.theme.FurniStoreTheme

/**
 * The single Activity of the app. On rotation Android destroys and recreates it
 * (there is no `configChanges` override on purpose) — that is what the
 * "Rotation demo" card on the Orders tab demonstrates.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FurniStoreTheme {
                FurniStoreApp()
            }
        }
    }
}
