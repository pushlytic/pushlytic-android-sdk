package com.pushlytic.example

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pushlytic.sdk.Pushlytic
import com.pushlytic.sdk.model.ConnectionStatus
import org.json.JSONObject

data class UserInfo(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val premiumStatus: Boolean
)

data class MessageData(
    val address: Address?,
    val age: Int,
    val email: String,
    val id: Int,
    val marketing: Marketing,
    val name: String,
    val preferences: Preferences
)

data class Address(
    val city: String,
    val state: String,
    val street: String,
    val zip: String
)

data class Marketing(
    val email: String,
    val message: String,
    val name: String
)

data class Preferences(
    val newsletter: Boolean,
    val notifications: Boolean
)

class MainViewModel : ViewModel() {
    private val _isStreamOpen = MutableLiveData(false)
    val isStreamOpen: LiveData<Boolean> = _isStreamOpen

    private val _connectionStatus = MutableLiveData("Disconnected")
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _currentMessage = MutableLiveData<MessageData?>()
    val currentMessage: LiveData<MessageData?> = _currentMessage

    private val _showingMessage = MutableLiveData(false)
    val showingMessage: LiveData<Boolean> = _showingMessage

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    init {
        observeAppState()
    }

    private fun observeAppState() {
        AppState.connectionStatus.observeForever { status ->
            handleConnectionStatusChanged(status)
        }

        AppState.message.observeForever { message ->
            handleMessageReceived(message)
        }
    }

    fun openStream() {
        _isStreamOpen.value = true
        openMessageStream()
    }

    fun closeStream() {
        // Closes the Pushlytic message stream and clears all connection data.
        // Setting `clearState` to true:
        // - Prevents automatic reconnection on app foregrounding.
        // - Removes stored metadata, user ID, and tags.
        Pushlytic.endStream(clearState = true)
        _isStreamOpen.value = false
        _connectionStatus.value = "Disconnected"
    }

    private fun openMessageStream() {
        // Opens the Pushlytic message stream to start receiving real-time updates.
        Pushlytic.openMessageStream()
    }

    // Handles connection status updates from the listener
    private fun handleConnectionStatusChanged(status: ConnectionStatus) {
        when (status) {
            is ConnectionStatus.Connected -> {
                _connectionStatus.postValue("Connected")
                userInfo.value?.let {
                    setEarlyMetadata(it)
                    registerUserWithMobileHooks(it)
                }
            }
            is ConnectionStatus.Disconnected -> {
                _connectionStatus.postValue("Disconnected")
            }
            is ConnectionStatus.Error -> {
                _connectionStatus.postValue("Error: ${status.error.localizedMessage}")
            }
            is ConnectionStatus.Timeout -> {
                _connectionStatus.postValue("Connection timeout")
            }
        }
    }

    // Handles incoming JSON messages from Pushlytic stream
    // Converts JSON to MessageData using type-safe deserialization
    // Runs on background thread pool with main thread callbacks
    private fun handleMessageReceived(jsonString: String) {
        Pushlytic.parseMessage(
            message = jsonString,
            type = MessageData::class.java,
            completion = { messageData ->
                _currentMessage.postValue(messageData)
                _showingMessage.postValue(true)
            },
            errorHandler = { error ->
                error.printStackTrace()
            }
        )
    }

    fun fetchEarlyLifecycleData() {
        // Simulate API call for user info (this would typically be replaced with actual backend logic).
        val userInfo = UserInfo(
            userId = "12345",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            premiumStatus = true
        )
        _userInfo.value = userInfo
        openMessageStream()
    }

    /// Sets initial metadata for the Pushlytic connection
    ///
    /// This demonstrates comprehensive metadata setup including:
    /// - User information
    /// - Device details
    /// - App configuration
    /// - A/B test assignments
    ///
    /// Example metadata payload:
    /// ```json
    /// {
    ///     "treatment": "treatment_one",
    ///     "first_name": "Doug",
    ///     "last_name": "Jones",
    ///     "email": "doug@example.com",
    ///     "premium_status": true,
    ///     "app_version": "1.0.0",
    ///     "device_type": "iPhone",
    ///     "os_version": "17.0",
    ///     "device_model": "iPhone"
    /// }
    /// ```
    private fun setEarlyMetadata(userInfo: UserInfo) {
        val metadata = mapOf(
            "treatment" to "treatment_one",
            "first_name" to userInfo.firstName,
            "last_name" to userInfo.lastName,
            "email" to userInfo.email,
            "premium_status" to userInfo.premiumStatus,
            "device_type" to "Android"
        )
        // Set initial connection metadata
        // Note: Subsequent calls to setMetadata will upsert rather than replace
        // Use clearMetadata() first if you need to reset all metadata
        Pushlytic.setMetadata(metadata)
    }

    private fun registerUserWithMobileHooks(userInfo: UserInfo) {
        // Register unique user identifier with Pushlytic.
        Pushlytic.registerUserID(userInfo.userId)
        // Add segmentation tags for user grouping.
        Pushlytic.registerTags(listOf("Android", "Treatment 2"))
    }

    fun dismissMessage() {
        _showingMessage.value = false
        _currentMessage.value = null
    }
}
