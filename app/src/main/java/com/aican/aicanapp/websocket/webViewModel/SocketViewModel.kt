package com.aican.aicanapp.websocket.webViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SocketViewModel  : ViewModel() {
    private val _messageLiveData = MutableLiveData<String>()
    val messageLiveData: LiveData<String> get() = _messageLiveData

    fun setMessage(message: String) {
        _messageLiveData.value = message
    }
}