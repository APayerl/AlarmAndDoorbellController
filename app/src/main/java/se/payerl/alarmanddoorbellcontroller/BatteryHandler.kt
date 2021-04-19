package se.payerl.alarmanddoorbellcontroller

import android.content.*
import android.os.BatteryManager
import android.util.Log
import androidx.preference.PreferenceManager
import se.payerl.haws.types.Client
import se.payerl.haws.types.ServerCallback
import se.payerl.haws.types.ServiceData

object BatteryHandler {
    private lateinit var pref: SharedPreferences
    private var batteryPercentage: Float = -1.0f
    private var isCharging: Boolean = false
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if(key.startsWith("overcharge_protection", true)) {
            decideOnBatteryAction()
        }
    }
    private val broadcastReciever = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var changeHasOccurred = false
            val chargingValue = isCharging(intent!!)
            val batteryPercentage = getBatteryPercentage(intent!!)

            synchronized(this@BatteryHandler.isCharging) {
                if(chargingValue != this@BatteryHandler.isCharging) {
                    this@BatteryHandler.isCharging = chargingValue
                    changeHasOccurred = true
                }
            }
            synchronized(this@BatteryHandler.batteryPercentage) {
                if(batteryPercentage != this@BatteryHandler.batteryPercentage) {
                    this@BatteryHandler.batteryPercentage = batteryPercentage
                    changeHasOccurred = true
                }
            }
            if(changeHasOccurred) decideOnBatteryAction()
        }
    }
    private lateinit var hac: HAConnection

    fun start(context: Context, hac: HAConnection): BatteryHandler {
        this.hac = hac
        this.batteryPercentage = getBatteryPercentage(context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!)
        this.isCharging = isCharging(context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!)

        this.pref = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

        this.pref.registerOnSharedPreferenceChangeListener(this.preferenceListener)

        context.applicationContext.registerReceiver(this.broadcastReciever, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        return this
    }

    fun stop(context: Context) {
        this.pref.unregisterOnSharedPreferenceChangeListener(this.preferenceListener)
        context.applicationContext.unregisterReceiver(this.broadcastReciever)
    }

    private fun isCharging(intent: Intent): Boolean {
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status != BatteryManager.BATTERY_STATUS_DISCHARGING
    }

    private fun getBatteryPercentage(intent: Intent): Float {
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return level * 100 / scale.toFloat()
    }

    private fun decideOnBatteryAction() {
        val entity: String = this.pref.getString("overcharge_protection_entity", "")!!
        val enabled: Boolean = this.pref.getBoolean("overcharge_protection_enabled", false)
        if(enabled && entity.isNotBlank()) {
            synchronized(this.batteryPercentage) {
                synchronized(this.isCharging) {
                    if(this.batteryPercentage <= this.pref.getInt("overcharge_protection_min", -1) && !this.isCharging) {
                        // Enable charging
                        this.hac.queueRequest(Client.CallServiceMessage("switch", "turn_on").setServiceData(ServiceData().add("entity_id", entity)), ServerCallback {  })
                    } else if(this.batteryPercentage >= this.pref.getInt("overcharge_protection_max", 101) && this.isCharging) {
                        // Disable charging
                        this.hac.queueRequest(Client.CallServiceMessage("switch", "turn_off").setServiceData(ServiceData().add("entity_id", entity)), ServerCallback {  })
                    }
                }
            }
        }
    }

//    override fun onReceive(context: Context?, intent: Intent?) {
//        var changeHasOccurred = false
//        val chargingValue = isCharging(intent!!)
//        val batteryPercentage = getBatteryPercentage(intent!!)
//
//        synchronized(this.isCharging) {
//            if(chargingValue != this.isCharging) {
//                this.isCharging = chargingValue
//                changeHasOccurred = true
//            }
//        }
//        synchronized(this.batteryPercentage) {
//            if(batteryPercentage != this.batteryPercentage) {
//                this.batteryPercentage = batteryPercentage
//                changeHasOccurred = true
//            }
//        }
//        if(changeHasOccurred) decideOnBatteryAction()
//    }
}