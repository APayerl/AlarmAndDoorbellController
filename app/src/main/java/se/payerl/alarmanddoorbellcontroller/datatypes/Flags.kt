package se.payerl.alarmanddoorbellcontroller.datatypes

import android.view.View
import android.view.WindowManager

class Flags {
    companion object {
        val HIDE_NAVBAR_AND_STATUSBAR_NONSTICK: Int = ( View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN )
        val IMMERSIVE_STICKY: Int = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val HIDE_NAVBAR_AND_STATUSBAR: Int = ( HIDE_NAVBAR_AND_STATUSBAR_NONSTICK or IMMERSIVE_STICKY)
    }
}