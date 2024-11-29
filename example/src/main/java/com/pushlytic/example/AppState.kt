package com.pushlytic.example

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pushlytic.sdk.model.ConnectionStatus

object AppState {
    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _heartbeatStatus = MutableLiveData<String>()

    fun updateConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.postValue(status)
    }

    fun updateMessage(message: String) {
        _message.postValue(message)
    }

    fun updateHeartbeatStatus(status: String) {
        _heartbeatStatus.postValue(status)
    }
}
