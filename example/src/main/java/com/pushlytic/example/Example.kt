package com.pushlytic.example

import android.app.Application
import com.pushlytic.sdk.Pushlytic
import com.pushlytic.sdk.model.ConnectionStatus
import com.pushlytic.sdk.model.PushlyticListener

/**
 * Example application demonstrating basic Pushlytic SDK setup and configuration
 */
class Example : Application() {
    override fun onCreate() {
        super.onCreate()
        // Step 1: Configure the Pushlytic SDK
        // - Provide the application context and configuration with your API key
        // - Replace "YOUR_API_KEY" with your actual API key from the Pushlytic dashboard
        Pushlytic.configure(
            application = this,
            configuration = Pushlytic.Configuration(
                apiKey = "YOUR_API_KEY"
            )
        )

        // Step 2: Set up the listener to handle incoming messages and connection status updates
        // - Implement the PushlyticListener interface to receive SDK callbacks
        Pushlytic.setListener(object : PushlyticListener {
            override fun onConnectionStatusChanged(status: ConnectionStatus) {
                AppState.updateConnectionStatus(status)
            }

            override fun onMessageReceived(message: String) {
                AppState.updateMessage(message)
            }
        })
    }
}
