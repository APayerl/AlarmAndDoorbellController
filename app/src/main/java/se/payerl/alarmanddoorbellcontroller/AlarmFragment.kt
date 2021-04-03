package se.payerl.alarmanddoorbellcontroller

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import se.payerl.alarmanddoorbellcontroller.datatypes.AlarmStates
import se.payerl.alarmanddoorbellcontroller.datatypes.IconColor
import se.payerl.alarmanddoorbellcontroller.datatypes.Jackson
import se.payerl.alarmanddoorbellcontroller.popups.PasswordPopup
import se.payerl.alarmanddoorbellcontroller.popups.PasswordPopupCallbacks
import se.payerl.haws.types.Client
import se.payerl.haws.types.Result
import se.payerl.haws.types.Server.ResultMessage
import se.payerl.haws.types.Server.SubscriptionMessage
import se.payerl.haws.types.ServerCallback
import se.payerl.haws.types.ServiceData
import java.net.URI
import java.util.*

private const val ALARM_DATA = "param1"
private const val URI_STRING = "param2"
private const val TOKEN = "param3"

class AlarmFragment : Fragment() {
    private lateinit var data: Result
    private lateinit var hac: HAConnection
    private var currentState = ""
    private var oddOrEven = false
    private lateinit var alarmStateImage: AppCompatImageView
    private lateinit var alarmStateTitle: AppCompatTextView
    private lateinit var alarmStateHeadline: AppCompatTextView


    private val closedGray = IconColor(R.drawable.ic_lock_circle_closed_gray, R.color.alarm_gray)
    private val closedYellow = IconColor(R.drawable.ic_lock_circle_closed_yellow, R.color.alarm_yellow)
    private val closedRed = IconColor(R.drawable.ic_lock_circle_closed_red, R.color.alarm_red)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.data = Jackson.get(true).readValue(it.getString(ALARM_DATA), Result::class.java)
            this.hac = HAConnection.getInstance(URI(it.getString(URI_STRING)), it.getString(TOKEN)!!)
        }
        synchronized(this.currentState) {
            this.currentState = this.data.state
        }
    }

    private fun showAsPending(coming: String?, text: Int) {
        if(AlarmStates.ARMED_HOME == coming) {
            showAsUndecided(listOf(closedGray, closedYellow), text)
        } else if(AlarmStates.ARMED_AWAY == coming) {
            showAsUndecided(listOf(closedGray, closedRed), text)
        } else {
            showAsUndecided(listOf(closedGray, closedGray), text)
        }
    }

    private fun showAsUndecided(displays: List<IconColor>, text: Int) {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    synchronized(currentState) {
                        if (AlarmStates.PENDING == currentState || AlarmStates.ARMING == currentState) {
                            this@AlarmFragment.alarmStateImage.setImageResource(if (oddOrEven) displays[0].icon else displays[1].icon)
                            this@AlarmFragment.alarmStateTitle.setTextColor(resources.getColor(if (oddOrEven) displays[0].color else displays[1].color))
                            this@AlarmFragment.alarmStateTitle.setText(text)
                            this@AlarmFragment.alarmStateHeadline.setTextColor(resources.getColor(if (oddOrEven) displays[0].color else displays[1].color))
                            oddOrEven = !oddOrEven
                        } else {
                            this.cancel()
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionMessage(message: SubscriptionMessage) {
        if(message.event.data.entityId == data.entityId) {
            synchronized(currentState) {
                currentState = message.event.data.newState.state
                if(message.event.data.newState.attributes.hasAttribute("nextState")) {
                    changeStatusOfAlarmTo(currentState, message.event.data.newState.attributes.getAttribute("nextState") as String)
                } else if(message.event.data.newState.attributes.hasAttribute("postPendingState")) {
                    changeStatusOfAlarmTo(currentState, message.event.data.newState.attributes.getAttribute("postPendingState") as String)
                } else {
                    changeStatusOfAlarmTo(currentState, null)
                }
            }
        }
    }

    private fun switchStateTo(state: String, coming: String?) {
        if(state == AlarmStates.DISARMED) {
            this.alarmStateImage.setImageResource(R.drawable.ic_lock_circle_open_green)
            this.alarmStateTitle.setText(R.string.alarm_disarmed)
            this.alarmStateTitle.setTextColor(resources.getColor(R.color.alarm_green))
            this.alarmStateHeadline.setTextColor(resources.getColor(R.color.alarm_green))
        }
        if((state == AlarmStates.PENDING) or (AlarmStates.ARMING == state)) {
            showAsPending(coming, if(AlarmStates.ARMING == state) R.string.alarm_arming else R.string.alarm_pending)
        }
        if(state == AlarmStates.ARMED_HOME) {
            this.alarmStateImage.setImageResource(R.drawable.ic_lock_circle_closed_yellow)
            this.alarmStateTitle.setText(R.string.alarm_armed_home)
            this.alarmStateTitle.setTextColor(resources.getColor(R.color.alarm_yellow))
            this.alarmStateHeadline.setTextColor(resources.getColor(R.color.alarm_yellow))
        }
        if(state == AlarmStates.ARMED_AWAY) {
            this.alarmStateImage.setImageResource(R.drawable.ic_lock_circle_closed_red)
            this.alarmStateTitle.setText(R.string.alarm_armed_away)
            this.alarmStateTitle.setTextColor(resources.getColor(R.color.alarm_red))
            this.alarmStateHeadline.setTextColor(resources.getColor(R.color.alarm_red))
        }
        if(state == (AlarmStates.TRIGGERED)) {
            this.alarmStateImage.setImageResource(R.drawable.ic_notifications_active)
            this.alarmStateImage.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.shake))
            this.alarmStateTitle.setText(R.string.alarm_triggered)
            this.alarmStateTitle.setTextColor(resources.getColor(R.color.alarm_red))
            this.alarmStateHeadline.setTextColor(resources.getColor(R.color.alarm_red))
        }
    }

    private fun changeStatusOfAlarmTo(state: String, coming: String?) {
        this.alarmStateImage.animation?.setAnimationListener(object: Animation.AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                switchStateTo(state, coming)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        if(this.alarmStateImage.animation == null) {
            switchStateTo(state, coming)
        } else {
            this.alarmStateImage.animation?.repeatCount = 0
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    private fun createArmEntry(image: Int, text: Int, color: Int): View {
        val armEntry = View.inflate(context, R.layout.arm_layout_entry, null)
        val alarmStateImage = armEntry.findViewById<AppCompatImageView>(R.id.alarm_state_image)
        val alarmStateText = armEntry.findViewById<AppCompatTextView>(R.id.alarm_state_title)
        alarmStateText.text = context?.resources?.getString(text)
        alarmStateText.setTextColor(color)
        alarmStateImage.setImageResource(image)
        return armEntry
    }

    private fun createArmDialog(homeAction: () -> Unit, awayAction: () -> Unit): AlertDialog {
        val armLayout = View.inflate(context, R.layout.arm_layout, null)
        val grid = armLayout.findViewById<GridLayout>(R.id.arm_alternatives)
        val homeView = createArmEntry(R.drawable.ic_lock_circle_closed_yellow, R.string.alarm_arm_home, R.color.alarm_yellow)
        val awayView = createArmEntry(R.drawable.ic_lock_circle_closed_red, R.string.alarm_arm_away, R.color.alarm_red)
        grid.addView(homeView)
        grid.addView(awayView)
        val adb = AlertDialog.Builder(this@AlarmFragment.requireContext())
        adb.setView(armLayout)
        val ad = adb.create()
        ad.window?.attributes?.windowAnimations = R.style.DialogAnimation
        homeView.setOnClickListener {
            homeAction()
            ad.dismiss()
        }
        awayView.setOnClickListener {
            awayAction()
            ad.dismiss()
        }
        return ad
    }

    private fun createPasswordPopup(title: Int, doAction: (code: Int) -> Unit) {
        var pop: PasswordPopup? = null
        synchronized(currentState) {
            pop = PasswordPopup(this.requireContext(),
                resources.getString(title) + ": " + this.data.attributes.friendlyName, listOf<String>())
        }
        pop?.show(object: PasswordPopupCallbacks {
            override fun onSuccess(code: String) {
                doAction(Integer.parseInt(code))
                pop?.hide()
            }

            override fun onWrongPassword(enteredPass: String, validCodes: List<String>) {}

            override fun onCancel() {
                pop?.hide()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.alarmStateImage = view.findViewById<AppCompatImageView>(R.id.alarm_state_image)
        this.alarmStateImage.setOnClickListener {
            synchronized(currentState) {
                if(this@AlarmFragment.currentState == AlarmStates.DISARMED) {
                    createArmDialog(homeAction = {
                        createPasswordPopup(R.string.alarm_arm_home, { arm(true, it)})
                    }, awayAction = {
                        createPasswordPopup(R.string.alarm_arm_away, { arm(false, it)})
                    }).show()
                } else {
                    createPasswordPopup(R.string.alarm_disarm, { disarm(it) })
                }
            }
        }
        this.alarmStateTitle = view.findViewById<AppCompatTextView>(R.id.alarm_state_title)
        this.alarmStateHeadline = view.findViewById<AppCompatTextView>(R.id.alarm_state_headline)

        changeStatusOfAlarmTo(currentState, null)
        this.alarmStateHeadline.text = data.attributes.friendlyName
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onStart() {
        super.onStart()
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    fun arm(home: Boolean, code: Int) {
        when(home) {
            true -> hac.queueRequest(Client.CallServiceMessage("alarm_control_panel", "alarm_arm_home").setServiceData(ServiceData().setEntityId(this.data.entityId).setCode(code)), ServerCallback { it: ResultMessage -> })
            false -> hac.queueRequest(Client.CallServiceMessage("alarm_control_panel", "alarm_arm_away").setServiceData(ServiceData().setEntityId(this.data.entityId).setCode(code)), ServerCallback { it: ResultMessage ->  })
        }
    }

    fun disarm(code: Int) {
        Log.d("disarm", this.data.entityId)
        hac.queueRequest(Client.CallServiceMessage("alarm_control_panel", "alarm_disarm").setServiceData(ServiceData().setEntityId(this.data.entityId).setCode(code)), ServerCallback { it: ResultMessage ->  })
    }

    companion object {
        fun isAlarmEntity(entityId: String): Boolean {
            return entityId.split(".")[0] == "alarm_control_panel"
        }

        @JvmStatic
        fun newInstance(data: Result, uri: String, token: String) =
                AlarmFragment().apply {
                    arguments = Bundle().apply {
                        putString(ALARM_DATA, Jackson.get(true).writeValueAsString(data))
                        putString(URI_STRING, uri)
                        putString(TOKEN, token)
                    }
                }
    }
}