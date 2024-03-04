package com.aican.aicanapp.interfaces

interface WebSocketData {

    fun onMessageReceive(message: String)
    fun onErrorReceive(error: String)
}