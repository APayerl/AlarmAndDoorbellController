package se.payerl.alarmanddoorbellcontroller.datatypes

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import se.payerl.alarmanddoorbellcontroller.R
import se.payerl.alarmanddoorbellcontroller.VideoHandler
import se.payerl.alarmanddoorbellcontroller.VideoHandler2
import se.payerl.alarmanddoorbellcontroller.popups.VideoPopup
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Camera(val context: Context, val cameraId: String, private val thumbnailRefreshInterval: Int = 5) {
    val preview: AppCompatImageView
    var handler: VideoHandler2? = null
    val preference: CameraPreference
    private var videoPopup: VideoPopup
    var textureView: TextureView
    private var thumbnailTimer: Timer? = null
    private var thumbnailPlayer: MediaPlayer? = null

    private var availableListener: (newState: Boolean) -> Unit
    private val evalViews: () -> Unit
    private var thumbnailStarted: Boolean = false
    val callback: (bitmap: Bitmap) -> Unit
    val event: MediaPlayer.EventListener

    init {
        preview = AppCompatImageView(context)
        preference = CameraPreference(cameraId, context)
        handler = VideoHandler2(context, preference)
        videoPopup = VideoPopup(context, preference)
        textureView = TextureView(context)
        availableListener = { newState ->
            val videoUrl = this.preference.getVideoUrl()
            if(newState && videoUrl.isNotBlank()) {
                handler = VideoHandler2(context, preference)
                preview.visibility = View.VISIBLE
                handler?.play(this.preview.width, this.preview.height, this.textureView, videoUrl, null)
            } else {
                preview.visibility = View.GONE
                handler?.dismiss()
            }
        }

        callback = { bitmap: Bitmap ->
            this@Camera.preview.setImageBitmap(bitmap)
            Log.e("TimerTask", "setting thumbnail for ${this@Camera.cameraId}!")
        }

        event = MediaPlayer.EventListener {
            Log.e("TimerTask", "recieved event for ${this@Camera.cameraId}!")
            if (!VideoHandler2.isOneColor(textureView.bitmap)) {
                Log.e("TimerTask", "It wasn't blank!")
                callback.invoke(textureView.bitmap)
                thumbnailPlayer!!.stop()
            }
        }

        evalViews = {
            if(preview.height > 0 && preview.width > 0 && textureView.width > 0 && textureView.height > 0 && !thumbnailStarted) {
                thumbnailStarted = !thumbnailStarted
                initThumnail()
                setThumbnailRefreshInterval(this.thumbnailRefreshInterval)
            }
        }

        preview.layoutParams = ViewGroup.LayoutParams(UnitTranslator.dpToPx(240, context).toInt(), UnitTranslator.dpToPx(135, context).toInt())
        preview.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            Log.e("Sizes-preview", "$cameraId - Width: ${preview.width}, Height: ${preview.height}")
            evalViews.invoke()
        }
        preview.visibility = if(preference.available()) View.VISIBLE else View.GONE
        preview.elevation = UnitTranslator.dpToPx(1, context)
        preview.setImageResource(R.drawable.ic_videocam_black_36dp)
        preview.setOnClickListener { preview: View ->
            this@Camera.videoPopup.show()
        }
        textureView.layoutParams = ViewGroup.LayoutParams(UnitTranslator.dpToPx(240, context).toInt(), UnitTranslator.dpToPx(135, context).toInt())
        textureView.visibility = View.INVISIBLE
        textureView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            Log.e("Sizes-texture", "Width: ${textureView.width}, Height: ${textureView.height}")
            evalViews.invoke()
        }

//        preference.availableChangeListener { newState ->
//            val videoUrl = preference.getVideoUrl()
//            if(newState && videoUrl.isNotBlank()) {
//                handler = VideoHandler2(context, preference)
//                preview.visibility = View.VISIBLE
//                handler?.play(this.preview.width, this.preview.height, this.textureView, videoUrl, null)
//            } else {
//                preview.visibility = View.GONE
//                handler?.dismiss()
//            }
//        }
        preference.availableChangeListener(availableListener)
    }

    private fun initThumnail() {
        if (this.thumbnailPlayer == null) {
            Log.e("thumbnail", "needs to init thumbnail")
            this.thumbnailPlayer = VideoHandler2.initializeThumbnailer(
                    context.applicationContext,
                    this@Camera.preference,
                    this@Camera.textureView,
                    callback
            )
        }
    }

    private fun setThumbnailRefreshInterval(interval: Int) {
        if(this.thumbnailRefreshInterval > 0) {
            this.thumbnailTimer = fixedRateTimer("", true, 0, interval*1000*60L) {
                Log.e("TimerTask", "getting thumbnail for ${this@Camera.cameraId}. Is the handler null? ${this@Camera.handler == null}!")
                Log.e("TimerTask", "Camera: ${this@Camera.cameraId} is playing? ${this@Camera.thumbnailPlayer?.isPlaying}")
                this@Camera.thumbnailPlayer!!.setEventListener(event)
                this@Camera.thumbnailPlayer!!.play()
                Log.e("TimerTask", "Camera: ${this@Camera.cameraId} is playing? ${this@Camera.thumbnailPlayer?.isPlaying}")
            }
        }
    }

    fun onResume() {
    }

    fun onPause() {
        this.thumbnailTimer?.cancel()
        this.handler?.pause()
    }

    fun onDestroy() {
        this.handler?.dismiss()
    }
}