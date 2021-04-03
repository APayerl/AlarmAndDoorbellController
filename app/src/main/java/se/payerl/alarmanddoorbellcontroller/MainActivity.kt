package se.payerl.alarmanddoorbellcontroller

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import se.payerl.alarmanddoorbellcontroller.popups.PasswordPopup
import se.payerl.alarmanddoorbellcontroller.popups.PasswordPopupCallbacks
import se.payerl.alarmanddoorbellcontroller.popups.VideoPopup
import se.payerl.haws.types.Client
import se.payerl.haws.types.Result
import se.payerl.haws.types.ServerCallback
import java.net.URI
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var hac: HAConnection
    var videoPopup: VideoPopup? = null
    lateinit var pager: ViewPager2
    lateinit var pagerPageIndicator: LinearLayout
    val viewPagerFragmentAdapter: ViewPagerFragmentAdapter = ViewPagerFragmentAdapter(this)
    lateinit var videoHandler: VideoHandler
    private val doorPreviews: MutableList<AppCompatImageView> = mutableListOf()
    private lateinit var textureView: TextureView

    private fun prefsString(number: Int, def: String): String {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(resources.getString(number), def)!!
    }

    private fun prefsHas(number: Int): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).contains(resources.getString(number))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        this.textureView = findViewById<TextureView>(R.id.doorbellPreviewTexture)
        this.textureView.visibility = TextureView.INVISIBLE
        this.doorPreviews.add(findViewById<AppCompatImageView>(R.id.doorbellPreviewImage))
        this.doorPreviews[0].visibility = AppCompatImageView.VISIBLE

        findViewById<AppCompatImageView>(R.id.menuBtn).setOnClickListener {
            this.showMenu((it.parent as ViewGroup).getChildAt(0), PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.action_settings -> {
                        val passwordPopup = PasswordPopup(this@MainActivity, R.string.menu_password, listOf<String>("1234", "5678", "9012"))
                        passwordPopup.show(object: PasswordPopupCallbacks {
                            override fun onSuccess(code: String) {
                                startActivity(Intent().setClassName(this@MainActivity, "$packageName.SettingsActivity"))
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

        this.videoHandler = VideoHandler(this@MainActivity)
        hac = HAConnection.getInstance(URI(prefsString(R.string.settings_home_assistant_ws_url, "")), prefsString(R.string.settings_home_assistant_token, ""))

//        this.videoHandler.addThumbnailListener(object: BitmapCallback{
//            override fun newFrame(bitmap: Bitmap) {
//                this@MainActivity.doorPreviews[0].setImageBitmap(bitmap)
//            }
//        })

        pagerPageIndicator = findViewById<LinearLayout>(R.id.fragmentIndicators)
        pager = findViewById<ViewPager2>(R.id.pager)

        pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        pager.adapter = this.viewPagerFragmentAdapter;
        pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
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
                systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }
    }

    fun setFlagsOnThePeekView() {
        try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val wmgInstance = wmgClass.getMethod("getInstance").invoke(null)
            val viewsField = wmgClass.getDeclaredField("mViews")
            viewsField.isAccessible = true

            val views = viewsField.get(wmgInstance) as ArrayList<View>
            // When the popup appears, its decorView is the peek of the stack aka last item
            views.last().apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                setOnSystemUiVisibilityChangeListener {
                    systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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

    fun showMenu(view: View, clickListener: PopupMenu.OnMenuItemClickListener, menu: Int) {
        val popup = PopupMenu(this@MainActivity, view)
        popup.menuInflater.inflate(menu, popup.menu)
        popup.setOnMenuItemClickListener(clickListener)
        popup.show()
        setFlagsOnThePeekView()
    }

    override fun onResume() {
        super.onResume()
//        this.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
        if(!hac.isOpen) {
            hac.connect()
            hac.queueRequest(Client.SubscribeMessage().setEventType("state_changed"), ServerCallback { })
            hac.queueRequest(Client.GetStatesMessage(), ServerCallback {
                it.result.forEach { x: Result ->
                    if(AlarmFragment.isAlarmEntity(x.entityId)) {
                        runOnUiThread {
                            Log.v("Add fragment", "${this.viewPagerFragmentAdapter.itemCount}")
                            this.viewPagerFragmentAdapter.addFragment(AlarmFragment.newInstance(x, prefsString(R.string.settings_home_assistant_ws_url, ""), prefsString(R.string.settings_home_assistant_token, "")))
                            fixPagerIndicators(this.pager.currentItem)
                        }
                    }
                }
            })
        }

        if(prefsHas(R.string.settings_video_url)) {
            this.doorPreviews[0].setOnClickListener {
                videoPopup = VideoPopup(this@MainActivity, prefsString(R.string.settings_video_url, ""), this.videoHandler)
                videoPopup?.show()
            }
            this.videoHandler.addThumbnailListener(object: BitmapCallback{
                override fun newFrame(bitmap: Bitmap) {
                    this@MainActivity.doorPreviews[0].setImageBitmap(bitmap)
                    Log.e("bitmap", "new")
                }
            }, textureView, prefsString(R.string.settings_video_url, ""))
        }
    }

    override fun onPause() {
        super.onPause()
        this.videoPopup?.hide()
        this.videoHandler.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.hac.close()
        this.videoPopup?.dismiss()
        this.videoHandler.dismiss()
    }
}