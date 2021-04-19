package se.payerl.alarmanddoorbellcontroller.datatypes

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.regex.Pattern

class Preferences(val context: Context) {
    val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val cameraListeners: MutableList<(cam: String) -> Unit> = mutableListOf()

    init {
        val cameraPattern = Pattern.compile("^(camera\\d+)")
        this.sp.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            key.matches(cameraPattern.toRegex())
            val matcher = cameraPattern.matcher(key)
            while (matcher.find()) {
                val camKey = matcher.group(1)
                this.cameraListeners.forEach {
                    it.invoke(camKey)
                }
            }
        }
    }

    fun getString(number: Int, def: String): String {
        return sp.getString(context.resources.getString(number), def)!!
    }

    fun contains(number: Int): Boolean {
        return this.sp.contains(context.resources.getString(number))
    }

    fun onCameraListener(camListener: (cam: String) -> Unit) {
        this.cameraListeners.add(camListener)
    }
}