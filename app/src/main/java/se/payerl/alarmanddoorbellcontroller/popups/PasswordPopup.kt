package se.payerl.alarmanddoorbellcontroller.popups

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import se.payerl.alarmanddoorbellcontroller.R

class PasswordPopup(context: Context, title: String, codes: List<String>) {
    private var dialog: AlertDialog
    private var code: String
    private val codes: List<String> = codes
    private val pinViewPasswordField: AppCompatTextView
    private val baseView: View = View.inflate(context, R.layout.pin_layout, null)

    constructor(context: Context, title: Int, codes: List<String>) : this(context, context.getString(title), codes)

    init {
        pinViewPasswordField = baseView.findViewById<AppCompatTextView>(R.id.textViewNumberPassword)

        var builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setView(baseView)
        code = ""
        dialog = builder.create()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn0).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn1).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn2).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn3).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn4).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn5).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn6).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn7).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn8).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewBtn9).setOnClickListener {
            numberButtonPressed(it)
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewNegative).setOnClickListener {
            hide()
        }
    }

    fun hide() {
        dialog.hide()
    }

    fun show(callback: PasswordPopupCallbacks) {
        baseView.findViewById<AppCompatButton>(R.id.pinViewPositive).setOnClickListener {
            if((code in this.codes) or this.codes.isEmpty()) {
                callback.onSuccess(code)
            } else {
                callback.onWrongPassword(code, this.codes)
            }
        }
        baseView.findViewById<AppCompatButton>(R.id.pinViewNegative).setOnClickListener {
            callback.onCancel()
        }
        dialog.show()
        code = ""
        pinViewPasswordField.text = code
    }

    private fun numberButtonPressed(view: View) {
        code = code.plus((view as AppCompatButton).text as String)
        pinViewPasswordField.text = code
    }

    fun resetCode() {
        this.code = ""
    }
}