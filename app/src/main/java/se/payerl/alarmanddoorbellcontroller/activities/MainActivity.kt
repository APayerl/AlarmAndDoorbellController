package se.payerl.alarmanddoorbellcontroller.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import se.payerl.alarmanddoorbellcontroller.*
import se.payerl.alarmanddoorbellcontroller.datatypes.Camera
import se.payerl.alarmanddoorbellcontroller.datatypes.CameraPreference
import se.payerl.alarmanddoorbellcontroller.datatypes.Flags
import se.payerl.alarmanddoorbellcontroller.datatypes.Preferences
import se.payerl.alarmanddoorbellcontroller.fragments.AlarmFragment
import se.payerl.alarmanddoorbellcontroller.popups.PasswordPopup
import se.payerl.alarmanddoorbellcontroller.popups.PasswordPopupCallbacks
import se.payerl.haws.types.Client
import se.payerl.haws.types.Result
import se.payerl.haws.types.ServerCallback
import java.net.URI
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var hac: HAConnection
    lateinit var pager: ViewPager2
    lateinit var pagerPageIndicator: LinearLayout
    val viewPagerFragmentAdapter: ViewPagerFragmentAdapter = ViewPagerFragmentAdapter(this)
    private val cameras: MutableList<Camera> = mutableListOf()
    private lateinit var prefs: Preferences
    private lateinit var cameraPreviews: LinearLayoutCompat

    private var batteryHandler: BatteryHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_main, null)
        view.keepScreenOn = true
        setContentView(view)
        supportActionBar?.hide()

        this.prefs = Preferences(this)
        this.cameraPreviews = findViewById<LinearLayoutCompat>(R.id.cameraPreviews)

        //Get all currently available
        CameraPreference.getAllCameraIds(prefs.sp).let { cameraIds ->
            for(cameraId in cameraIds) {
                addCamera(cameraId)
            }
        }
        //Listener to get cameras when they get added
        this.prefs.onCameraListener({ cameraId: String ->
            addCamera(cameraId)
        })

//        this.textureView = findViewById<TextureView>(R.id.doorbellPreviewTexture)
//        this.textureView.visibility = TextureView.INVISIBLE
//        this.doorPreviews.add(findViewById<AppCompatImageView>(R.id.doorbellPreviewImage))
//        this.doorPreviews[0].visibility = AppCompatImageView.VISIBLE

        this.findViewById<AppCompatImageView>(R.id.menuBtn).setOnClickListener {
            this.showMenu((it.parent as ViewGroup).getChildAt(0), PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_settings -> {
                        val passwordPopup = PasswordPopup(this@MainActivity, R.string.menu_password, listOf<String>("1234", "5678", "9012"))
                        passwordPopup.show(object : PasswordPopupCallbacks {
                            override fun onSuccess(code: String) {
                                startActivity(Intent().setClassName(this@MainActivity, "$packageName.activities.SettingsActivity"))
                                passwordPopup.hide()
                            }

                            override fun onWrongPassword(enteredPass: String, validCodes: List<String>) {
                                Toast.makeText(this@MainActivity, R.string.menu_password_invalid, Toast.LENGTH_SHORT).show()
                                passwordPopup.resetCode()
                            }

                            override fun onCancel() {
                                passwordPopup.hide()
                            }
                        })
                        true
                    }
                    R.id.action_about -> {
                        startActivity(Intent().setClassName(this@MainActivity, "$packageName.AboutActivity"))
                        true
                    }
                    else -> false
                }
            }, R.menu.menu);
        }
        hac = HAConnection.getInstance(URI(prefs.getString(R.string.settings_home_assistant_ws_url, "")), prefs.getString(R.string.settings_home_assistant_token, ""))
//        this.videoHandler.addThumbnailListener(object: BitmapCallback{
//            override fun newFrame(bitmap: Bitmap) {
//                this@MainActivity.doorPreviews[0].setImageBitmap(bitmap)
//            }
//        })

        pagerPageIndicator = findViewById<LinearLayout>(R.id.fragmentIndicators)
        pager = findViewById<ViewPager2>(R.id.pager)

        pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        pager.adapter = this.viewPagerFragmentAdapter;
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                fixPagerIndicators(position)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) {
            window.decorView.apply {
                systemUiVisibility = Flags.HIDE_NAVBAR_AND_STATUSBAR
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = Flags.HIDE_NAVBAR_AND_STATUSBAR
        if(!hac.isOpen) {
            hac.connect()
            hac.queueRequest(Client.SubscribeMessage().setEventType("state_changed"), ServerCallback { })
            hac.queueRequest(Client.GetStatesMessage(), ServerCallback {
                it.result.forEach { x: Result ->
                    if (AlarmFragment.isAlarmEntity(x.entityId)) {
                        runOnUiThread {
                            Log.v("Add fragment", "${this.viewPagerFragmentAdapter.itemCount}")
                            this.viewPagerFragmentAdapter.addFragment(AlarmFragment.newInstance(x, prefs.getString(R.string.settings_home_assistant_ws_url, ""), prefs.getString(R.string.settings_home_assistant_token, "")))
                            fixPagerIndicators(this.pager.currentItem)
                        }
                    }
                }
            })
        }

        cameras.forEach { camera ->
            camera.onResume()
        }

        this.batteryHandler = BatteryHandler.start(this, hac)
//        videoHandlers.forEach { videoHandler: VideoHandler ->
//            val index = videoHandlers.indexOf(videoHandler)
//            videoHandler.addThumbnailListener(object: BitmapCallback {
//                override fun newFrame(bitmap: Bitmap) {
//                    this@MainActivity.doorPreviews[index].setImageBitmap(bitmap)
//                    this@MainActivity.doorPreviews[index].setOnClickListener { preview: View ->
//                        videoPopup = VideoPopup(this@MainActivity, CameraPreference("camera$index").getVideoUrl(this@MainActivity.prefs.sp), videoHandler)
//                        videoPopup?.show()
//                    }
//                }
//            }, textureView, CameraPreference("camera$index").getVideoUrl(this.prefs.sp))
//        }
    }

    override fun onPause() {
        super.onPause()
//        this.videoPopup?.hide()
        this.cameras.forEach { camera ->
            camera.onPause()
        }
        this.batteryHandler!!.stop(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.hac.close()
//        this.videoPopup?.dismiss()
        this.cameras.forEach { camera ->
            camera.onDestroy()
        }
    }

    private fun addCamera(cameraId: String) {
        Log.e("Sizes-preview", "$cameraId")
        synchronized(this.cameras) {
            this.cameras.add(Camera(this@MainActivity, cameraId))
            synchronized(this.cameraPreviews) {
                this.cameraPreviews.addView(this.cameras.last().preview)
                this.cameraPreviews.addView(this.cameras.last().textureView)
            }
        }
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

    fun fixPagerIndicators(activeIndex: Int) {
        pagerPageIndicator.removeAllViews()
        val icon = "•"
        if(this.viewPagerFragmentAdapter.itemCount > 1) {
            for (i in 0 until this.viewPagerFragmentAdapter.itemCount) {
                var view = AppCompatTextView(this)
                view.text = icon
                view.setTextColor(resources.getColor(if (i == activeIndex) R.color.pager_indicator_is_selected else R.color.pager_indicator_not_selected))
                view.textSize = 60.0f
                pagerPageIndicator.addView(view)
            }
        }
    }

    private fun showMenu(view: View, clickListener: PopupMenu.OnMenuItemClickListener, menu: Int) {
        val popup = PopupMenu(this@MainActivity, view)
        popup.menuInflater.inflate(menu, popup.menu)
        popup.setOnMenuItemClickListener(clickListener)
        popup.show()
        setFlagsOnThePeekView()
    }
}