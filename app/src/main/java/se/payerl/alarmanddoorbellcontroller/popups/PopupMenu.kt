package se.payerl.alarmanddoorbellcontroller.popups

import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import se.payerl.alarmanddoorbellcontroller.datatypes.Flags
import java.util.ArrayList

class PopupMenu {
    companion object {
        fun show(context: Context, options: List<String>, anchor: View, handler: PopupMenu.OnMenuItemClickListener) {
            val popup = PopupMenu(context, anchor)
            options.forEach { popup.menu.add(it) }
            popup.setOnMenuItemClickListener(handler)
            popup.show()
            setFlagsOnThePeekView()
        }

        private fun setFlagsOnThePeekView() {
            try {
                val wmgClass = Class.forName("android.view.WindowManagerGlobal")
                val wmgInstance = wmgClass.getMethod("getInstance").invoke(null)
                val viewsField = wmgClass.getDeclaredField("mViews")
                viewsField.isAccessible = true

                val views = viewsField.get(wmgInstance) as ArrayList<View>
                // When the popup appears, its decorView is the peek of the stack aka last item
                views.last().apply {
                    systemUiVisibility = Flags.HIDE_NAVBAR_AND_STATUSBAR
                    setOnSystemUiVisibilityChangeListener {
                        systemUiVisibility = Flags.HIDE_NAVBAR_AND_STATUSBAR
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}