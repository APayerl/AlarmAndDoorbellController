package se.payerl.alarmanddoorbellcontroller.datatypes

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class CameraPreference(val cameraId: String, context: Context) {
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val availableListeners: MutableList<(newState: Boolean) -> Unit> = mutableListOf()

    init {
        this.sp.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if(key == "$cameraId.${CameraPreference.CAMERA_AVAILABLE}") {
                val newBoolean = sharedPreferences.getBoolean(key, false)
                this.availableListeners.forEach { listener ->
                    listener.invoke(newBoolean)
                }
            }
        }
    }

    fun available(): Boolean {
        return sp.getBoolean("$cameraId.${CameraPreference.CAMERA_AVAILABLE}", false)
    }
    fun availableChangeListener(listener: (newState: Boolean) -> Unit) {
        availableListeners.add(listener)
    }

    fun requireCredentials(): Boolean {
        return sp.getBoolean("$cameraId.${CameraPreference.CREDENTIALS_REQUIRED}", false)
    }

    fun getUsername(): String {
        return sp.getString("$cameraId.${CameraPreference.CREDENTIALS_USERNAME}", "")!!
    }

    fun getPassword(): String {
        return sp.getString("$cameraId.${CameraPreference.CREDENTIALS_PASSWORD}", "")!!
    }

    fun getVideoUrl(): String {
        return sp.getString("$cameraId.${CameraPreference.VIDEO_URL}", "")!!
    }

    companion object {
        fun getAllCameraIds(sp: SharedPreferences): List<String> {
            val mList = mutableListOf<String>()
            var id = 0
            while (sp.contains("camera$id")) {
                mList.add("camera$id")
                id += 1
            }
            return mList.toList()
        }

        const val CAMERA_AVAILABLE = "cameraAvailable"
        const val VIDEO_URL = "videoUrl"
        const val CREDENTIALS_REQUIRED = "credentialsRequired"
        const val CREDENTIALS_USERNAME = "username"
        const val CREDENTIALS_PASSWORD = "password"
    }
}
