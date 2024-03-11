package com.aican.aicanapp.websocket

import android.util.Log
import com.aican.aicanapp.websocket.webViewModel.SocketViewModel
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
    private var uri: URI? = null

    fun initializeWebSocket(
        uri: URI,
        openListener: () -> Unit,
        closeListeners: (Int, String?, Boolean) -> Unit
    ) {



        if (webSocketClient == null) {
            createWebSocketClient()
        } else {
            // WebSocketClient is already initialized, reset listeners
            setOpenListener(openListener)
            setCloseListener(closeListeners)
        }
    }

    private fun createWebSocketClient() {
        uri?.let {
            webSocketClient = object : WebSocketClient(it) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    openListener?.invoke()
                    WEBSOCKET_CONNECTED = true
                }

                override fun onClosing(code: Int, reason: String?, remote: Boolean) {
                    super.onClosing(code, reason, remote)
                    Log.e("Disconnecting", "Closing.....")
                    clearListeners()
                    WEBSOCKET_CONNECTED = false
                    if (!forceDisconnect) {
                        reconnect()
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    closeListener?.invoke(code, reason, remote)
                    WEBSOCKET_CONNECTED = false
                    clearListeners()
                }

                override fun onMessage(message: String?) {
                    message?.let {
//                        viewModel.setMessage(it)
                        WEBSOCKET_CONNECTED = true
                        messageListener?.invoke(it)
                    }
                }

                override fun onError(ex: Exception?) {
                    ex?.let {
                        WEBSOCKET_CONNECTED= false
                        Log.e("ExceptionError", "Error " + it)
                        errorListener?.invoke(it)
                    }
                }
            }
            webSocketClient?.connect()
        }
    }

    fun reconnecting() {
        // Check if WebSocketClient exists and WebSocket is not connected
        if (webSocketClient != null && !webSocketClient!!.isOpen) {
            // Attempt to reconnect
            webSocketClient!!.connect()
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


    public fun clearListeners() {
        webSocketClient = null

        messageListener = null
        openListener = null
        closeListener = null
        errorListener = null
    }
}
