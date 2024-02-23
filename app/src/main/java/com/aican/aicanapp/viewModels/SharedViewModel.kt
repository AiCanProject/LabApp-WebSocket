package com.aican.aicanapp.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val messageLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()
}
