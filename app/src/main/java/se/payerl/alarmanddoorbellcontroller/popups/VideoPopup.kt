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
import se.payerl.alarmanddoorbellcontroller.VideoHandler2
import se.payerl.alarmanddoorbellcontroller.datatypes.CameraPreference
import se.payerl.alarmanddoorbellcontroller.datatypes.Flags

class VideoPopup(context: Context, private val preference: CameraPreference) {
    private var alertDialog: AlertDialog
    private val videoView: TextureView
    private var mProgressBar: ProgressBar
    private var videoHandler: VideoHandler2

    init {
        val baseView = View.inflate(context, R.layout.video_layout, null)
        this.videoView = baseView.findViewById<TextureView>(R.id.video_view2)
        this.mProgressBar = baseView.findViewById<ProgressBar>(R.id.progressBar)
        this.videoHandler = VideoHandler2(context, preference)

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

    fun show() {
        alertDialog.window?.decorView?.systemUiVisibility = Flags.HIDE_NAVBAR_AND_STATUSBAR
        alertDialog.show()

        alertDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        );

        if(this.videoHandler.isPaused()) {
            this.videoHandler.play()
        } else {
            val dm = DisplayMetrics()
            alertDialog.window?.windowManager?.defaultDisplay?.getMetrics(dm)

            this.videoHandler.play(dm.widthPixels, dm.heightPixels, this.videoView, preference.getVideoUrl(), mProgressBar)
        }
    }

    fun hide() {
        this.videoHandler.pause()
        alertDialog.hide()
    }

    fun dismiss() {
        this.videoHandler.dismiss()
        alertDialog.dismiss()
    }
}