/**
 * Configuration constants for the Pushlytic SDK API client.
 *
 * These constants define the core configuration values for network communication,
 * connection management, and client identification. All time values are in seconds.
 *
 * Usage:
 * ```kotlin
 * val heartbeatDelay = ApiConstants.HEARTBEAT_INTERVAL_SECONDS * 1000L // Convert to milliseconds
 * val host = ApiConstants.SERVER_HOST
 * ```
 *
 * @property HEARTBEAT_INTERVAL_SECONDS Interval between heartbeat signals (60 seconds)
 * @property HEARTBEAT_TIMEOUT_SECONDS Time until connection is considered dead without heartbeat (300 seconds)
 * @property RECONNECTION_DELAY_SECONDS Delay before attempting reconnection (10 seconds)
 * @property MAX_RECONNECTION_DELAY_SECONDS Caps the delay between reconnection attempts to avoid excessively long waits.
 * @property DEBUG_SERVER_HOST Host address for the Debug Pushlytic server
 * @property RELEASE_SERVER_HOST Host address for the Release Pushlytic server
 * @property SERVER_PORT Port number for the Pushlytic server
 * @property KEEP_ALIVE_TIME Frequency of keep-alive pings to maintain connection (in seconds)
 * @property KEEP_ALIVE_TIMEOUT Time after which connection is terminated if no response (in seconds)
 *
 * @since 1.0.0
 */
object ApiConstants {
    const val HEARTBEAT_INTERVAL_SECONDS: Long = 300 // 5 minutes
    const val HEARTBEAT_TIMEOUT_SECONDS: Long = 600 // 10 minutes, 2x HEARTBEAT_INTERVAL
    const val RECONNECTION_DELAY_SECONDS: Long = 10 // Initial delay for reconnection attempts
    const val MAX_RECONNECTION_DELAY_SECONDS: Long = 60 // Cap for exponential backoff
    const val RELEASE_SERVER_HOST: String = "stream.pushlytic.com"
    const val SERVER_PORT: Int = 443 // Default server port

    /**
     * Frequency of keep-alive pings sent to maintain the connection.
     * Used to ensure the connection remains active.
     */
    const val KEEP_ALIVE_TIME: Long = 30 // Adjust as needed

    /**
     * Time after which the connection is terminated if no keep-alive response is received.
     * Ensures timely closure of unresponsive connections.
     */
    const val KEEP_ALIVE_TIMEOUT: Long = 10 // Adjust as needed

}
