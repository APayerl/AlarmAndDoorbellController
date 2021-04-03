package se.payerl.alarmanddoorbellcontroller

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*

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

    class SettingsFragment : PreferenceFragmentCompat() {
        private var nextCameraIndex = 0
        private var cameras: PreferenceCategory? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        private fun createVideoPreference(context: Context): PreferenceCategory {
            val camera = PreferenceCategory(context)
            val CAMERA = "camera"
            camera.key = "$CAMERA${this.nextCameraIndex++}"
            camera.title = "Camera ${this.nextCameraIndex}"

            val doorbellAvailablePreference = SwitchPreference(context)
            doorbellAvailablePreference.setTitle(R.string.preference_doorbell_enabled)
            doorbellAvailablePreference.setDefaultValue(true)
            doorbellAvailablePreference.key = "$CAMERA${this.nextCameraIndex}.doorbellEnabled"
            camera.addPreference(doorbellAvailablePreference)

            val cameraAddressPreference = EditTextPreference(context)
            cameraAddressPreference.dependency = doorbellAvailablePreference.key
            cameraAddressPreference.key = "$CAMERA${this.nextCameraIndex}.${context.getString(R.string.settings_video_url)}"
            cameraAddressPreference.title = context.getString(R.string.preference_url_field_title)
            cameraAddressPreference.summary = context.getString(R.string.preference_url_field_summary)
            cameraAddressPreference.setDefaultValue("")
            camera.addPreference(cameraAddressPreference)

            val credentialsRequiredPreference = SwitchPreference(context)
            credentialsRequiredPreference.dependency = doorbellAvailablePreference.key
            credentialsRequiredPreference.key = "$CAMERA${this.nextCameraIndex}.authenticationRequired"
            credentialsRequiredPreference.title = "Authentication required"
            credentialsRequiredPreference.setDefaultValue(true)
            camera.addPreference(credentialsRequiredPreference)

            val usernamePreference = EditTextPreference(context)
            usernamePreference.dependency = credentialsRequiredPreference.key
            usernamePreference.key = "$CAMERA${this.nextCameraIndex}.username}"
            usernamePreference.title = context.getString(R.string.preference_username_field_title)
            usernamePreference.setDefaultValue("admin")
            camera.addPreference(usernamePreference)


            val passwordPreference = EditTextPreference(context)
            passwordPreference.dependency = credentialsRequiredPreference.key
            passwordPreference.key = "$CAMERA${this.nextCameraIndex}.password}"
            passwordPreference.title = context.getString(R.string.preference_password_field_title)
            passwordPreference.setDefaultValue("admin")
            camera.addPreference(passwordPreference)
            return camera
        }

        override fun onResume() {
            this.cameras = findPreference<PreferenceCategory>("cameras")

            super.onResume()
            val sp = PreferenceManager.getDefaultSharedPreferences(this.activity?.applicationContext)
            while (!sp.contains("camera${this.nextCameraIndex}")) {
                this.nextCameraIndex += 1
            }

            PreferenceManager.getDefaultSharedPreferences(this.activity?.applicationContext).registerOnSharedPreferenceChangeListener { sharedPreferences, key ->

            }
        }
    }
}