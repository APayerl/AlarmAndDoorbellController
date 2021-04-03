package se.payerl.alarmanddoorbellcontroller.starters

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity

class MyAppStarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.action)) {
            startActivity(context, Intent().setClassName(context.applicationContext, "${context.packageName}.MainActivity"), null)
        }
    }
}