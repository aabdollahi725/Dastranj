package ir.dastranj.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ir.dastranj.app.ui.DastranjApp
import ir.dastranj.app.ui.theme.DastranjTheme

/**
 * The app's only Activity. Navigation happens entirely in Compose (PRD §13.1).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PRD §12 / CLAUDE.md §10: FLAG_SECURE on every Activity. Keeps balances out of the
        // recents thumbnail and blocks screenshots of financial data.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // The design draws its own status-bar row and floats the tab bar over the page background,
        // so the app needs the full window rather than an inset-padded one.
        enableEdgeToEdge()

        setContent {
            DastranjTheme {
                DastranjApp()
            }
        }
    }
}
