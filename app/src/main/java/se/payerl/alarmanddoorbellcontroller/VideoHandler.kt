package se.payerl.alarmanddoorbellcontroller

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.TextureView
import android.widget.ProgressBar
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import se.payerl.alarmanddoorbellcontroller.datatypes.CameraPreference
import kotlin.collections.ArrayList

class VideoHandler(context: Context, private val camera: CameraPreference): MediaPlayer.EventListener {
    private val mLibVlc: LibVLC
    private val mMediaPlayer: MediaPlayer
    private val callbacks: MutableList<(bitmap: Bitmap) -> Unit> = mutableListOf()
    private val clients: MutableList<VideoClient> = mutableListOf()

    init {
        var args = ArrayList<String>()
        if(camera.requireCredentials()) {
            args.add("--rtsp-user=${camera.getUsername()}")
            args.add("--rtsp-pwd=${camera.getPassword()}")
        }
        mLibVlc = LibVLC(context.applicationContext, args)
        mMediaPlayer = MediaPlayer(mLibVlc)
    }

    companion object {
        fun isOneColor(bitmap: Bitmap): Boolean {
            val allpixels = IntArray(bitmap.height * bitmap.width)

            bitmap.getPixels(
                allpixels,
                0,
                bitmap.width,
                0,
                0,
                bitmap.width,
                bitmap.height
            )

            for (i in allpixels.indices) {
                if (allpixels[i] != allpixels[0]) {
                    return false
                }
            }

            return true
        }
    }

    private fun loadVideo(url: String) {
        val mMedia = Media(mLibVlc, Uri.parse(url))
        mMediaPlayer.media = mMedia
        mMedia.release()
    }

    fun addThumbnailListener(callback: (bitmap: Bitmap) -> Unit, textureView: TextureView, url: String): VideoHandler {
        synchronized(this.callbacks) {
            this.callbacks.add(callback)
            this.mMediaPlayer.setEventListener(this)
        }
        Log.e("bitmap", "Attached: ${this.mMediaPlayer.vlcVout.areViewsAttached()}, Playing: ${this.mMediaPlayer.isPlaying}")
        if(!this.mMediaPlayer.vlcVout.areViewsAttached()) {
            Log.e("bitmap", "Attached: ${this.mMediaPlayer.vlcVout.areViewsAttached()}, Playing: ${this.mMediaPlayer.isPlaying}")
            show(textureView.width, textureView.height, textureView, url, null)
        }
        Log.e("bitmap", "Attached: ${this.mMediaPlayer.vlcVout.areViewsAttached()}, Playing: ${this.mMediaPlayer.isPlaying}")
        return this
    }

    private fun attach(client: VideoClient, url: String) {
        loadVideo(url)
        switchTo(client)
    }

    private fun switchTo(client: VideoClient) {
        Log.e("Camera", "switching")
        this.mMediaPlayer.stop()
        this.mMediaPlayer.vlcVout.detachViews()
        val vout = this.mMediaPlayer.vlcVout
        vout.setVideoView(client.view)
        vout.setWindowSize(client.width, client.height)
        vout.attachViews()
        this.mMediaPlayer.play()
    }

    fun show(width: Int, height: Int, view: TextureView, url: String, progressBar: ProgressBar?) {
        synchronized(this.clients) {
            this.clients.add(VideoClient(width, height, view, progressBar))
            Log.e("Camera", "Attached: ${this.mMediaPlayer.vlcVout.areViewsAttached()}, Queue: ${this.clients.size}")
            if(this.mMediaPlayer.vlcVout.areViewsAttached()) {
                switchTo(this.clients.last())
            } else {
                attach(this.clients.last(), url)
            }
        }

        mMediaPlayer.setEventListener(this)
//        mMediaPlayer.play()
    }

    fun stop() {
        synchronized(this.clients) {
            if(this.clients.size > 1) {
                this.clients.removeAt(this.clients.size - 1)
                switchTo(this.clients.get(this.clients.size - 1))
            } else {
                this.clients.clear()
                mMediaPlayer.stop()
            }
        }
    }

    fun dismiss() {
        mMediaPlayer.release()
        mLibVlc.release()
    }

    override fun onEvent(event: MediaPlayer.Event) {
        if(event.type == MediaPlayer.Event.Playing) {
            Log.e("Camera", "playing")
        }
        synchronized(this.clients) {
            if(this.clients.isNotEmpty()) {
                val current = this.clients.last()
                if (!isOneColor(current.view.bitmap)) {
                    current.progressBar?.visibility = ProgressBar.GONE
                    synchronized(this.callbacks) {
                        Log.v("callbacks", "contains ${this.callbacks.size} callbacks")
                        while (this.callbacks.isNotEmpty()) {
                            Log.v("callback", "sending to callback")
                            this.callbacks.removeAt(0).invoke(current.view.bitmap)
                        }
                        this.mMediaPlayer.setEventListener(null)
                    }
                }
            }
        }
    }

    data class VideoClient(val width: Int, val height: Int, val view: TextureView, val progressBar: ProgressBar?)
}