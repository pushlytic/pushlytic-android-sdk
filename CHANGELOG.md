# Changelog
All notable changes to the Pushlytic Android SDK will be documented in this file.

## [0.1.1] - 2025-01-11
### Added
- Device ID tracking in gRPC metadata using ANDROID_ID
- Cache mechanism for device ID with lazy initialization
- UUID fallback for edge cases
- Parity with iOS's identifierForVendor implementation

## [0.1.0] - 2024-12-01
### Beta Release
First beta release of Pushlytic Android SDK! 🎉

### Added
- Initial SDK release with core functionality
- Real-time bidirectional gRPC communication
- User targeting system with IDs, tags, and metadata
- Customizable push notifications with dynamic templates
- Message parsing utilities for type-safe message handling
- Android 5.0+ compatibility (API Level 21)
- Kotlin and Java support

### Developer Experience
- Complete documentation with usage examples
- Type-safe message parsing API
- Thread-safe message handling
- Automatic reconnection handling
- Example app showcasing SDK features
- Gradle dependency for easy integration

> Note: This is a beta release. Minor version updates (0.x.0) may include breaking changes as we refine the API based on developer feedback.

---