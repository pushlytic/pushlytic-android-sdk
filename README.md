# Pushlytic Android SDK

A powerful Android SDK for real-time communication, enabling seamless push messages and real-time interactions in your Android applications.

## Key Features
- Real-time bidirectional streaming
- User targeting with IDs, tags, and metadata
- Customizable push messages with dynamic templates
- Automatic connection management
- Support for experiments and A/B testing

## Requirements
- Android 6.0 (API level 24) and above
- Java 11 or Kotlin 1.5+
- Gradle 7.4 or later

## Installation

Get started with Pushlytic in your Android app - it's quick and easy!

#### Available on Maven Central
The Pushlytic Android SDK is published on [Maven Central](https://search.maven.org/). You can add it to your project using Gradle or Maven.

#### Gradle
1. Add the Pushlytic Android SDK dependency to your `build.gradle` file:
   ```kotlin
   dependencies {
       implementation("com.pushlytic.sdk:sdk:0.1.0")
   }
   ```
2. Sync your project with Gradle files.

> **Note**: We're rapidly improving Pushlytic! 🚀 During our pre-1.0 phase:
> - Minor version updates (0.x.0) may include exciting new features and improvements that could have breaking changes.
> - Using "Up to Next Minor" ensures you get all bug fixes while maintaining stability.
> - Once we hit 1.0.0, we'll follow strict semantic versioning with "Up to Next Major Version."
>
> Join us early and help shape the future of push infrastructure! Your feedback and use cases are invaluable as we move toward our 1.0.0 release.

---

## Quick Start

1. Configure the SDK in your `Application` class:
   ```kotlin
   import android.app.Application
   import com.pushlytic.sdk.Pushlytic

   class YourApplication : Application() {
       override fun onCreate() {
           super.onCreate()

           // Configure the SDK with your API key
           Pushlytic.configure(
               this,
               Pushlytic.Configuration(apiKey = "YOUR_API_KEY")
           )
       }
   }
   ```

2. Set up a `PushlyticListener` to handle connection status and messages:
   ```kotlin
   Pushlytic.setListener(object : PushlyticListener {
       override fun onConnectionStatusChanged(status: ConnectionStatus) {
           println("Pushlytic Status: $status")
       }

       override fun onMessageReceived(message: String) {
           println("Pushlytic Message: $message")
       }
   })
   ```

---

## Basic Usage

### Stream Management
Open a connection to start receiving messages:
```kotlin
// Open a message stream
Pushlytic.openMessageStream()

// Later, when you want to stop receiving messages:
// - Set clearState to false to allow automatic reconnection on app foreground
// - Set clearState to true to clear all connection metadata and prevent automatic reconnection
Pushlytic.endStream(clearState = false)
```

### Set Up User Information
Register user data such as user ID, tags, and metadata:
```kotlin
// Register user ID
Pushlytic.registerUserID("unique_user_id")

// Add tags for targeting
Pushlytic.registerTags(listOf("premium_user", "electronics"))

// Set user metadata
Pushlytic.setMetadata(
    mapOf(
        "first_name" to "John",
        "account_type" to "premium"
    )
)
```

### Handle Messages
To receive real-time updates from the SDK, implement `PushlyticListener` and handle connection statuses and incoming messages:
```kotlin
// Define message types
data class CustomMessage(val id: String, val content: String)

Pushlytic.setListener(object : PushlyticListener {
    override fun onConnectionStatusChanged(status: ConnectionStatus) {
        println("Connection status: $status")
    }

    override fun onMessageReceived(message: String) {
        Pushlytic.parseMessage(
            message,
            CustomMessage::class.java,
            completion = { parsedMessage ->
                println("Received message: ${parsedMessage.content}")
            },
            errorHandler = { error ->
                println("Error parsing message: ${error.message}")
            }
        )
    }
})
```

---

## Advanced Usage

### Custom Events
Send custom analytics events:
```kotlin
Pushlytic.sendCustomEvent(
    name = "purchase",
    metadata = mapOf("item_id" to "12345", "price" to 19.99)
)
```

---

## Example App
An example app demonstrating usage of Pushlytic SDK features is available in the `example/` directory. It showcases:
- Stream connection
- Message handling
- User segmentation
- Metadata-driven personalization

---

## Repository Structure
- **sdk/**: Core SDK functionality
- **example/**: Fully functional example app demonstrating SDK usage
- **tests/**: Unit tests for SDK components

---

## Contributing
Contributions are welcome! Please see the `CONTRIBUTING.md` file for guidelines on submitting issues, feature requests, and pull requests.

---

## License
Pushlytic Android SDK is available under the MIT License. See the `LICENSE` file for more information.

---

## Security & Support
- For security vulnerabilities, contact our security team at [security@pushlytic.com](mailto:security@pushlytic.com)
- For general support, reach out to [support@pushlytic.com](mailto:support@pushlytic.com) or visit our [documentation site](https://pushlytic.com/docs)

---

## Related Resources
- [Pushlytic iOS SDK](https://github.com/pushlytic/pushlytic-ios-sdk)
- [Pushlytic API Documentation](https://pushlytic.com/docs)
```