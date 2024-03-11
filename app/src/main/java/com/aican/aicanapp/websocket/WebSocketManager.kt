package com.aican.aicanapp.websocket

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

object WebSocketManager {

    public var WEBSOCKET_CONNECTED = false

    private var webSocketClient: WebSocketClient? = null
    private var messageListener: ((String) -> Unit)? = null
    private var errorListener: ((Exception) -> Unit)? = null
    private var openListener: (() -> Unit)? = null
    private var closeListener: ((Int, String?, Boolean) -> Unit)? = null
    private var forceDisconnect: Boolean = false
    fun initializeWebSocket(
        uri: URI,
        openListener: () -> Unit,
        closeListeners: (Int, String?, Boolean) -> Unit
    ) {
        if (webSocketClient == null) {
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    openListener.invoke()
                    WEBSOCKET_CONNECTED = true
                }

                override fun onClosing(code: Int, reason: String?, remote: Boolean) {
                    super.onClosing(code, reason, remote)
//                    disconnect()
                    Log.e("Disconnecting", "Closing.....")
                    clearListeners()
                    WEBSOCKET_CONNECTED = false

                    if (!forceDisconnect) {
                        reconnect()
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    closeListeners.invoke(code, reason, remote)
                    closeListener!!.invoke(code, reason, remote)
                    Log.e("OnClose", "OnClose")
                    WEBSOCKET_CONNECTED = false
                    setCloseListener(closeListeners)
//                    disconnect(false)
                    clearListeners()
                }

                override fun onMessage(message: String?) {
                    message?.let {
                        WEBSOCKET_CONNECTED = true
                        messageListener?.invoke(it)
                    }
                }

                override fun onError(ex: Exception?) {
                    ex?.let {
                        Log.e("ExceptionError", "Error " + it)
                        WEBSOCKET_CONNECTED = false
                        errorListener?.invoke(it)
                    }
                }

            }
            webSocketClient?.connect()
        } else {
            // WebSocketClient is already initialized, reset listeners
            setOpenListener(openListener)
            setCloseListener(closeListener!!)
        }
    }

    fun setMessageListener(listener: (String) -> Unit) {
        messageListener = listener
    }

    fun setOpenListener(listener: () -> Unit) {
        openListener = listener
    }

    fun setCloseListener(listener: (Int, String?, Boolean) -> Unit) {
        closeListener = listener
    }

    fun sendMessage(message: String) {
        if (webSocketClient?.isOpen == true) {
            webSocketClient?.send(message)
        } else {
            errorListener?.invoke(Exception("WebSocket is not connected"))
        }
    }

    fun setErrorListener(listener: (Exception) -> Unit) {
        errorListener = listener
    }

    fun disconnect(forceDisconnect: Boolean) {
        this.forceDisconnect = forceDisconnect
        webSocketClient?.close()

        // Clear existing listeners
        clearListeners()
    }


    private fun clearListeners() {
        webSocketClient = null

        messageListener = null
        openListener = null
        closeListener = null
        errorListener = null
    }
}
