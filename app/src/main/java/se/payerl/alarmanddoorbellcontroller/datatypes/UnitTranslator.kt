package se.payerl.alarmanddoorbellcontroller.datatypes

import android.content.Context
import android.util.Log

class UnitTranslator {
    companion object {
        fun dpToPx(dp: Int, context: Context): Float {
            return dp * context.resources.displayMetrics.density
        }
    }
}