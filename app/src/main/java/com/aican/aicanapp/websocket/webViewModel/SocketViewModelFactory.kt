package com.aican.aicanapp.websocket.webViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SocketViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SocketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocketViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
