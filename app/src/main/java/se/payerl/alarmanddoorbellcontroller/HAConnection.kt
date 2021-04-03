package se.payerl.alarmanddoorbellcontroller

import android.util.Log
import org.greenrobot.eventbus.EventBus
import se.payerl.alarmanddoorbellcontroller.datatypes.Jackson
import se.payerl.haws.HomeAssistantWS
import se.payerl.haws.types.Client
import se.payerl.haws.types.Server.AuthInvalidMessage
import se.payerl.haws.types.Server.ResultMessage
import se.payerl.haws.types.Server.ServerMessage
import se.payerl.haws.types.Server.SubscriptionMessage
import se.payerl.haws.types.ServerCallback
import java.net.URI

class HAConnection private constructor(uri: URI, token: String): HomeAssistantWS(uri, token) {
    companion object {
        private var instance: HAConnection? = null

        fun getInstance(uri: URI, token: String): HAConnection {
            if(instance == null) instance = HAConnection(uri, token)
            return instance!!
        }
    }
    private val myRequests = mutableMapOf<Int, ServerCallback<ServerMessage>>()
    private var queue = mutableListOf<Client.ClientMessage>()
    private var ready = false

//    fun callService(domain: String, service: String, serviceData: ServiceData) {
//        send(Client.CallServiceMessage(domain, service).setServiceData(serviceData))
//    }

    fun queueRequest(message: Client.ClientMessage, callback: ServerCallback<ResultMessage>) {
        message.setId(this.nextMessageId)
        synchronized(this.ready) {
            if(this.ready) {
                send(message)
            } else {
                queue.add(message)
            }
            myRequests.put(message.id, callback as ServerCallback<ServerMessage>)
        }
    }

    override fun onAuthInvalid(message: AuthInvalidMessage?) {
        Log.e("HAConn/AuthInvalid", message?.message)
    }

    override fun onAuthOk() {
        Log.v("HAConn/AuthOk", "AuthOk")
        synchronized(this.ready) {
            this.ready = true
        }
        var removeObj = mutableListOf<Client.ClientMessage>()
        queue.forEach {
            send(it)
            removeObj.add(it)
        }
        queue.removeAll(removeObj)
    }

    override fun onResult(resultMessage: ResultMessage?) {
        System.out.println("recived in onResult")
        Log.d("HAConn/Result", Jackson.get(true).writeValueAsString(resultMessage))
        (myRequests.get(resultMessage?.id) as ServerCallback<ResultMessage>).onReceived(resultMessage)
    }

    override fun onSubscriptionMessage(message: SubscriptionMessage?) {
        if(message?.event?.data?.entityId?.startsWith("alarm_control_panel")!!) {
            Log.d("HAConn/Subscription", Jackson.get(true).writeValueAsString(message))
            EventBus.getDefault().post(message)
        }
    }

    override fun onPong(message: ServerMessage?) {
        Log.v("HAConn/Pong", Jackson.get(true).writeValueAsString(message))
    }
}