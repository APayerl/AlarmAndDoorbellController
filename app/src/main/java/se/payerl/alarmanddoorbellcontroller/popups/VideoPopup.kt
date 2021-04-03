package se.payerl.alarmanddoorbellcontroller.popups

import android.content.Context
import android.util.DisplayMetrics
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import se.payerl.alarmanddoorbellcontroller.R
import se.payerl.alarmanddoorbellcontroller.VideoHandler


class VideoPopup(context: Context, private val mUrl: String, private val videoHandler: VideoHandler) {
    private var alertDialog: AlertDialog
    private val videoView: TextureView
//    private var mUrl = url
    private var mProgressBar: ProgressBar

    init {
        val baseView = View.inflate(context, R.layout.video_layout, null)
        this.videoView = baseView.findViewById<TextureView>(R.id.video_view2)
        this.mProgressBar = baseView.findViewById<ProgressBar>(R.id.progressBar)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        alertDialogBuilder.setView(baseView)
        alertDialog = alertDialogBuilder.create()
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        val closeBtn = baseView.findViewById<AppCompatImageView>(R.id.close_btn)
        closeBtn.setImageResource(R.drawable.ic_close_white_18dp)
        closeBtn.setOnClickListener(View.OnClickListener {
            hide()
        })
    }

    fun show(url: String = mUrl) {
        alertDialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
        alertDialog.show()

        alertDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        );

        val dm = DisplayMetrics()
        alertDialog.window?.windowManager?.defaultDisplay?.getMetrics(dm)

        this.videoHandler.show(dm.widthPixels, dm.heightPixels, this.videoView, url, mProgressBar)
    }

    fun hide() {
        this.videoHandler.stop()
        alertDialog.hide()
    }

    fun dismiss() {
        this.videoHandler.dismiss()
        alertDialog.dismiss()
    }
}