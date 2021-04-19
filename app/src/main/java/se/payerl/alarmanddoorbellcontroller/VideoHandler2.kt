package se.payerl.alarmanddoorbellcontroller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ProgressBar
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import se.payerl.alarmanddoorbellcontroller.datatypes.CameraPreference
import kotlin.collections.ArrayList

class VideoHandler2(context: Context, private val camera: CameraPreference): MediaPlayer.EventListener {
    private val mLibVlc: LibVLC
    private val mMediaPlayer: MediaPlayer
    private val thumbnailListeners: MutableList<(bitmap: Bitmap) -> Unit> = mutableListOf()
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

        fun initializeThumbnailer(context: Context, camera: CameraPreference, textureView: TextureView, callback: (bitmap: Bitmap) -> Unit): MediaPlayer {
            var args = mutableListOf<String>()//ArrayList<String>()
            if(camera.requireCredentials()) {
                args.add("--rtsp-user=${camera.getUsername()}")
                args.add("--rtsp-pwd=${camera.getPassword()}")
            }
            val libVlc = LibVLC(context, args)
            val player = MediaPlayer(libVlc)
            val mMedia = Media(libVlc, Uri.parse(camera.getVideoUrl()))
            player.media = mMedia
            mMedia.release()
            val vout = player.vlcVout
            vout.setVideoView(textureView)
            vout.setWindowSize(textureView.width, textureView.height)
            Log.e("TimerTask-initThumb", "Width: ${textureView.width}, Height: ${textureView.height}")
            vout.attachViews()
//            player.setEventListener {
//                Log.e("TimerTask", "recieved event for ${camera.cameraId}!")
//                if (!isOneColor(textureView.bitmap)) {
//                    callback.invoke(textureView.bitmap)
//                    player.stop()
//                }
//            }
            return player
        }

        fun addThumbnailListener(context: Context, camera: CameraPreference, textureView: TextureView, callback: (bitmap: Bitmap) -> Unit): MediaPlayer {
//            thread(start = true) {
//                var keepRunning = true

                Log.e("TimerTask", "started thread ${Thread.currentThread()}!")
                var args = mutableListOf<String>()//ArrayList<String>()
                if(camera.requireCredentials()) {
                    args.add("--rtsp-user=${camera.getUsername()}")
                    args.add("--rtsp-pwd=${camera.getPassword()}")
                }
                val libVlc = LibVLC(context, args)
                val player = MediaPlayer(libVlc)
                val mMedia = Media(libVlc, Uri.parse(camera.getVideoUrl()))
                player.media = mMedia
                mMedia.release()
                val vout = player.vlcVout
                vout.setVideoView(textureView)
                vout.setWindowSize(textureView.width, textureView.height)
                vout.attachViews()
                player.setEventListener {
                    Log.e("TimerTask", "recieved event for ${camera.cameraId}!")
                    if (!isOneColor(textureView.bitmap)) {
                        callback.invoke(textureView.bitmap)
                        player.setEventListener(null)
                        player.stop()
//                        keepRunning = false
                    }
                }
                player.play()
            return player

//                while (keepRunning) {
//                    //nothing....
//                }
//            }
        }
    }

    private fun loadVideo(url: String) {
        val mMedia = Media(mLibVlc, Uri.parse(url))
        mMediaPlayer.media = mMedia
        mMedia.release()
    }

    private fun setView(client: VideoClient) {
        this.mMediaPlayer.vlcVout.detachViews()
        val vout = this.mMediaPlayer.vlcVout
        vout.setVideoView(client.view)
        vout.setWindowSize(client.width, client.height)
        vout.attachViews()
    }

    fun isPaused(): Boolean {
        return this.mMediaPlayer.playerState == MediaPlayer.Event.Paused
    }

    fun play() {
        this.mMediaPlayer.play()
    }

    fun play(width: Int, height: Int, view: TextureView, url: String, progressBar: ProgressBar?) {
        synchronized(this.clients) {
            this.clients.add(VideoClient(width, height, view, progressBar))
            if(this.mMediaPlayer.vlcVout.areViewsAttached()) {
                this.mMediaPlayer.stop()
            } else {
                loadVideo(url)
            }
            setView(this.clients.last())
            this.mMediaPlayer.setEventListener(this)
            this.mMediaPlayer.play()
        }
    }

    fun pause() {
        this.mMediaPlayer.pause()
    }

    fun detach() {
        this.mMediaPlayer.stop()
        synchronized(this.clients) {
            this.clients.removeAt(this.clients.size - 1)
            if(this.clients.isNotEmpty()) {
                setView(this.clients.get(this.clients.size - 1))
                this.mMediaPlayer.play()
            }
        }
    }

    fun dismiss() {
        mMediaPlayer.release()
        mLibVlc.release()
    }

    override fun onEvent(event: MediaPlayer.Event) {
        synchronized(this.clients) {
            if(this.clients.isNotEmpty()) {
                val current = this.clients.last()
                if (!isOneColor(current.view.bitmap)) {
                    current.progressBar?.visibility = ProgressBar.GONE
                    synchronized(this.thumbnailListeners) {
                        while (this.thumbnailListeners.isNotEmpty()) {
                            this.thumbnailListeners.removeAt(0).invoke(current.view.bitmap)
                        }
                        this.mMediaPlayer.setEventListener(null)
                    }
                }
            }
        }
    }

    data class VideoClient(val width: Int, val height: Int, val view: TextureView, val progressBar: ProgressBar?)
}