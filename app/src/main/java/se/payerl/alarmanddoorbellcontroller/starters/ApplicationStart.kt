package se.payerl.alarmanddoorbellcontroller.starters

import android.app.Application
import androidx.preference.PreferenceManager
import se.payerl.alarmanddoorbellcontroller.R

class ApplicationStart: Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceManager.setDefaultValues(applicationContext, R.xml.root_preferences, true);
    }
}