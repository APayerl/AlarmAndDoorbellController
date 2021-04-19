package se.payerl.alarmanddoorbellcontroller.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.*
import se.payerl.alarmanddoorbellcontroller.BatteryHandler
import se.payerl.alarmanddoorbellcontroller.HAConnection
import se.payerl.alarmanddoorbellcontroller.R
import se.payerl.alarmanddoorbellcontroller.datatypes.CameraPreference
import se.payerl.alarmanddoorbellcontroller.datatypes.Flags
import se.payerl.alarmanddoorbellcontroller.datatypes.Preferences
import se.payerl.haws.types.Client
import se.payerl.haws.types.Result
import se.payerl.haws.types.ServerCallback
import java.net.URI

class SettingsFragment : PreferenceFragmentCompat() {
    private var nextCameraIndex = 0
    private lateinit var cameras: PreferenceCategory
    private var contextMenu: View? = null
    private var latestSelected: PreferenceCategory? = null
    private lateinit var prefs: Preferences
    private lateinit var batteryHandler: BatteryHandler

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

//    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
//        super.onCreateContextMenu(menu, v, menuInfo)
//        this.contextMenu = v
//        layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
//        menu.add("Delete")
//    }

//    override fun onContextItemSelected(item: MenuItem): Boolean {
//        return when(item.title) {
//            "Delete" -> this.latestSelected?.parent?.removePreference(latestSelected)!!
//            else -> super.onContextItemSelected(item)
//        }
//    }

    override fun onResume() {
        super.onResume()
        this.activity?.window?.decorView?.systemUiVisibility = Flags.HIDE_NAVBAR_AND_STATUSBAR

        this.cameras = findPreference<PreferenceCategory>("cameras")!!
        val context: Context? = this.activity?.applicationContext
        this.prefs = Preferences(context!!)

        while (prefs.sp.contains("camera${this.nextCameraIndex}")) {
            createVideoPreference(
                    context!!,
                    this.cameras,
                    prefs.sp.getString("camera${this.nextCameraIndex}.${CameraPreference.VIDEO_URL}", ""),
                    prefs.sp.getString("camera${this.nextCameraIndex}.${CameraPreference.CREDENTIALS_USERNAME}", ""),
                    prefs.sp.getString("camera${this.nextCameraIndex}.${CameraPreference.CREDENTIALS_PASSWORD}", ""))
        }

        if(this.cameras.findPreference<Preference>("camera") == null) {
            createEmptyPreference(context, this.cameras)
        }

        val hac = HAConnection.getInstance(URI(prefs.getString(R.string.settings_home_assistant_ws_url, "")), prefs.getString(R.string.settings_home_assistant_token, ""))
        hac.queueRequest(Client.GetStatesMessage(), ServerCallback { resultMessage ->
            val mutableList = mutableListOf<Result>()
            resultMessage.result.forEach { x: Result ->
                if (x.entityId.startsWith("switch.", true) && (x.state.toLowerCase() == "on" || x.state.toLowerCase() == "off")) {
                    mutableList.add(x)
                }
            }
            with(this@SettingsFragment.findPreference<DropDownPreference>("overcharge_protection_entity")!!, {
                activity?.runOnUiThread {
                    this.entries = mutableList.map { result -> result.attributes.friendlyName }.toTypedArray()
                    this.entryValues = mutableList.map { result -> result.entityId }.toTypedArray()
                    this.isEnabled = true
                }
            })
        })

        val overchargeEnabled = findPreference<SwitchPreference>("overcharge_protection_enabled")!!
        overchargeEnabled.setOnPreferenceChangeListener { preference, newValue ->
            Log.w("preference", "${overchargeEnabled.key}: $newValue")
            prefs.sp.edit().putBoolean(overchargeEnabled.key, newValue as Boolean).apply()
            true
        }
        val overchargeEntity = findPreference<DropDownPreference>("overcharge_protection_entity")!!
        overchargeEntity.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        overchargeEntity.setOnPreferenceChangeListener { preference, newValue ->
            Log.w("preference", "${overchargeEntity.key}: $newValue")
            prefs.sp.edit().putString(overchargeEntity.key, newValue as String).apply()
            true
        }
        val overchargeMin = findPreference<SeekBarPreference>("overcharge_protection_min")!!
        overchargeMin.setOnPreferenceChangeListener { preference, newValue ->
            Log.w("preference", "${overchargeMin.key}: $newValue")
            prefs.sp.edit().putInt(overchargeMin.key, newValue as Int).apply()
            true
        }
        val overchargeMax = findPreference<SeekBarPreference>("overcharge_protection_max")!!
        overchargeMax.setOnPreferenceChangeListener { preference, newValue ->
            Log.w("preference", "${overchargeMax.key}: $newValue")
            prefs.sp.edit().putInt(overchargeMax.key, newValue as Int).apply()
            true
        }

        activity.let {
            this.batteryHandler = BatteryHandler.start(it!!, hac)
        }
//        registerForContextMenu(listView)
    }

    override fun onPause() {
        super.onPause()
        activity.let {
            this.batteryHandler.stop(it!!)
        }
    }

    private fun reCalculateSwitches(key: String) {
        Log.e("preferences:reCalc:key", key)
        val category = findPreference<PreferenceCategory>(key)!!

        for(i in (category.preferenceCount - 1) downTo 0) {
            Log.e("preferences:reCalc:cont", category.getPreference(i).key)
        }

        val doorbellAvailablePreference = category.findPreference<SwitchPreference>(key.plus(".${CameraPreference.CAMERA_AVAILABLE}"))!!
        val cameraAddressPreference = category.findPreference<EditTextPreference>(key.plus(".${CameraPreference.VIDEO_URL}"))!!
        val credentialsRequiredPreference = category.findPreference<SwitchPreference>(key.plus(".${CameraPreference.CREDENTIALS_REQUIRED}"))!!
        val usernamePreference = category.findPreference<EditTextPreference>(key.plus(".${CameraPreference.CREDENTIALS_USERNAME}"))!!
        val passwordPreference = category.findPreference<EditTextPreference>(key.plus(".${CameraPreference.CREDENTIALS_PASSWORD}"))!!

        cameraAddressPreference.text.let {
            doorbellAvailablePreference.isChecked = true
        }
        usernamePreference.text.let {
            credentialsRequiredPreference.isChecked = true
        }
        passwordPreference.text.let {
            credentialsRequiredPreference.isChecked = true
        }

        this.prefs.sp.edit()
                ?.putBoolean(doorbellAvailablePreference.key, doorbellAvailablePreference.isChecked)
                ?.putString(cameraAddressPreference.key, cameraAddressPreference.text)
                ?.putBoolean(credentialsRequiredPreference.key, credentialsRequiredPreference.isChecked)
                ?.putString(usernamePreference.key, usernamePreference.text)
                ?.putString(passwordPreference.key, passwordPreference.text)
                ?.putStringSet(
                    key, mutableSetOf<String>(
                        doorbellAvailablePreference.isChecked.toString(),
                        cameraAddressPreference.text,
                        credentialsRequiredPreference.isChecked.toString(),
                        usernamePreference.text,
                        passwordPreference.text))
                ?.apply()
    }

    private fun createEmptyPreference(context: Context, base: PreferenceCategory) {
        val preference = Preference(context)
        base.addPreference(preference)
        preference.key = "camera"
        preference.title = "Add new Camera"
        preference.order = Int.MAX_VALUE
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            this.activity?.applicationContext.let { context ->
                if(context != null) {
                    createVideoPreference(context, this@SettingsFragment.cameras)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun createVideoPreference(context: Context, base: PreferenceCategory, videoUrl: String? = null, username: String? = null, password: String? = null) {
        val CAMERA = "camera"

        val camera = PreferenceCategory(context)
        base.addPreference(camera)
        camera.order = 1
        camera.key = "$CAMERA${this.nextCameraIndex}"
        camera.title = "Camera ${this.nextCameraIndex + 1}"
//        camera.isPersistent = true
        camera.setOnPreferenceClickListener {
            this.latestSelected = camera
            this@SettingsFragment.contextMenu?.performLongClick()
            true
        }

        val x = this.prefs.sp.getString("${camera.key}.${CameraPreference.VIDEO_URL}", "")
        Log.e("preferences::create", x)

        val doorbellAvailablePreference = SwitchPreference(context)
        camera.addPreference(doorbellAvailablePreference)
        doorbellAvailablePreference.key = "$CAMERA${this.nextCameraIndex}.${CameraPreference.CAMERA_AVAILABLE}"
        doorbellAvailablePreference.isIconSpaceReserved = true
        doorbellAvailablePreference.setTitle(R.string.preference_doorbell_enabled)
        doorbellAvailablePreference.isChecked = this.prefs.sp.getBoolean(doorbellAvailablePreference.key, false)
        doorbellAvailablePreference.setOnPreferenceChangeListener { preference, newValue ->
            Log.e("preferences::changed", "key: ${doorbellAvailablePreference.key} to: $newValue")
            this.prefs.sp.edit().putBoolean(doorbellAvailablePreference.key, newValue as Boolean).apply()
            reCalculateSwitches(camera.key)
            true }
        doorbellAvailablePreference.isPersistent = false

        val cameraAddressPreference = EditTextPreference(context)
        cameraAddressPreference.dialogLayoutResource = R.layout.edit_text_fix
        camera.addPreference(cameraAddressPreference)
        cameraAddressPreference.key = "$CAMERA${this.nextCameraIndex}.${CameraPreference.VIDEO_URL}"
        cameraAddressPreference.isIconSpaceReserved = true
        cameraAddressPreference.dependency = doorbellAvailablePreference.key
        cameraAddressPreference.title = context.getString(R.string.preference_url_field_title)
        cameraAddressPreference.summary = context.getString(R.string.preference_url_field_summary)
        cameraAddressPreference.text = this.prefs.sp.getString(cameraAddressPreference.key, "")
        cameraAddressPreference.setOnPreferenceChangeListener { preference, newValue ->
            Log.e("preferences::changed", "key: ${cameraAddressPreference.key} to: $newValue")
            this.prefs.sp.edit().putString(cameraAddressPreference.key, newValue as String).apply()
            reCalculateSwitches(camera.key)
            true
        }
        cameraAddressPreference.isPersistent = false

        val credentialsRequiredPreference = SwitchPreference(context)
        camera.addPreference(credentialsRequiredPreference)
        credentialsRequiredPreference.key = "$CAMERA${this.nextCameraIndex}.${CameraPreference.CREDENTIALS_REQUIRED}"
        credentialsRequiredPreference.isIconSpaceReserved = true
        credentialsRequiredPreference.dependency = doorbellAvailablePreference.key
        credentialsRequiredPreference.title = "Authentication required"
        credentialsRequiredPreference.isChecked = this.prefs.sp.getBoolean(credentialsRequiredPreference.key, false)
        credentialsRequiredPreference.setOnPreferenceChangeListener { preference, newValue ->
            Log.e("preferences::changed", "key: ${credentialsRequiredPreference.key} to: $newValue")
            this.prefs.sp.edit().putBoolean(credentialsRequiredPreference.key, newValue as Boolean).apply()
            reCalculateSwitches(camera.key)
            true
        }
        credentialsRequiredPreference.isPersistent = false

        val usernamePreference = EditTextPreference(context)
        usernamePreference.dialogLayoutResource = R.layout.edit_text_fix
        camera.addPreference(usernamePreference)
        usernamePreference.isIconSpaceReserved = true
        usernamePreference.dependency = credentialsRequiredPreference.key
        usernamePreference.key = "$CAMERA${this.nextCameraIndex}.${CameraPreference.CREDENTIALS_USERNAME}"
        usernamePreference.title = context.getString(R.string.preference_username_field_title)
        usernamePreference.text = this.prefs.sp.getString(usernamePreference.key, "admin")
        usernamePreference.setOnPreferenceChangeListener { preference, newValue ->
            Log.e("preferences::changed", "key: ${usernamePreference.key} to: $newValue")
            this.prefs.sp.edit().putString(usernamePreference.key, newValue as String).apply()
            reCalculateSwitches(camera.key)
            true
        }
        usernamePreference.isPersistent = false

        val passwordPreference = EditTextPreference(context)
        passwordPreference.dialogLayoutResource = R.layout.edit_text_fix
        camera.addPreference(passwordPreference)
        passwordPreference.isIconSpaceReserved = true
        passwordPreference.dependency = credentialsRequiredPreference.key
        passwordPreference.key = "$CAMERA${this.nextCameraIndex}.${CameraPreference.CREDENTIALS_PASSWORD}"
        passwordPreference.title = context.getString(R.string.preference_password_field_title)
        passwordPreference.text = this.prefs.sp.getString(passwordPreference.key, "admin")
        passwordPreference.setOnPreferenceChangeListener { preference, newValue ->
            Log.e("preferences::changed", "key: ${passwordPreference.key} to: $newValue")
            this.prefs.sp.edit().putString(passwordPreference.key, newValue as String).apply()
            reCalculateSwitches(camera.key)
            true }
        passwordPreference.isPersistent = false

        videoUrl?.let {
            cameraAddressPreference.text = videoUrl
            doorbellAvailablePreference.isChecked = true
        }
        username?.let {
            credentialsRequiredPreference.isChecked = true
            usernamePreference.text = username
        }
        password?.let {
            credentialsRequiredPreference.isChecked = true
            passwordPreference.text = password
        }

        this.prefs.sp.edit()
                .putBoolean(doorbellAvailablePreference.key, doorbellAvailablePreference.isChecked)
                .putString(cameraAddressPreference.key, cameraAddressPreference.text)
                .putBoolean(credentialsRequiredPreference.key, credentialsRequiredPreference.isChecked)
                .putString(usernamePreference.key, usernamePreference.text)
                .putString(passwordPreference.key, passwordPreference.text)
                .putStringSet(camera.key, mutableSetOf<String>(
                        doorbellAvailablePreference.isChecked.toString(),
                        cameraAddressPreference.text,
                        credentialsRequiredPreference.isChecked.toString(),
                        usernamePreference.text,
                        passwordPreference.text))
                .apply()

        this.nextCameraIndex += 1
    }
}