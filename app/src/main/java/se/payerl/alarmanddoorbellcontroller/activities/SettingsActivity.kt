package se.payerl.alarmanddoorbellcontroller.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import se.payerl.alarmanddoorbellcontroller.BatteryHandler
import se.payerl.alarmanddoorbellcontroller.HAConnection
import se.payerl.alarmanddoorbellcontroller.R
import se.payerl.alarmanddoorbellcontroller.datatypes.Preferences
import se.payerl.alarmanddoorbellcontroller.fragments.SettingsFragment
import java.net.URI

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}