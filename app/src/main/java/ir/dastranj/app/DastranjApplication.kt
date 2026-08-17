package ir.dastranj.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import ir.dastranj.app.ui.util.Money

@HiltAndroidApp
class DastranjApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // CLAUDE.md §2: a stored amount that is not a multiple of 10 means something wrote to the
        // database outside the intended path. Report it as an error condition — never with the
        // amount, which must not be logged even in debug. R8 strips this call in release anyway.
        Money.setIntegrityReporter { site ->
            Log.e(TAG, "Stored amount is not a multiple of 10 at: $site")
        }
    }

    private companion object {
        const val TAG = "Dastranj"
    }
}
