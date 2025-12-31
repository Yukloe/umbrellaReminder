# Contributing to Umbrella Reminder

Thank you for your interest in contributing to Umbrella Reminder! This document provides guidelines for contributors.

## How to Contribute

### Reporting Issues
- Use the GitHub issue tracker to report bugs
- Provide detailed information about your environment (Android version, device model)
- Include steps to reproduce the issue
- Add screenshots if applicable

### Feature Requests
- Open an issue with the "enhancement" label
- Describe the feature and its use case
- Consider if it aligns with the app's core purpose

### Code Contributions

#### Development Setup
1. Fork the repository
2. Clone your fork locally
3. Open in Android Studio
4. Create a new branch for your feature

#### Coding Standards
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Ensure code compiles without warnings

#### Testing
- Test on multiple Android versions if possible
- Verify the app works with and without permissions
- Test edge cases (no internet, no GPS, etc.)

#### Submitting Changes
1. Commit your changes with clear messages
2. Push to your fork
3. Create a pull request
4. Describe your changes in the PR description
5. Wait for code review

## Project Structure

```
app/src/main/java/com/umbrella/reminder/
├── MainActivity.kt              # Main UI and permission handling
├── BootReceiver.kt             # Handles device boot to reschedule notifications
├── data/
│   ├── WeatherApi.kt          # API interface and data models
│   └── WeatherRepository.kt    # Weather data management and rain detection
├── location/
│   └── LocationManager.kt     # Location services wrapper
├── notification/
│   └── NotificationHelper.kt   # Notification management
├── scheduler/
│   └── NotificationScheduler.kt # WorkManager scheduling
└── work/
    └── WeatherCheckWorker.kt   # Background weather checking
```

## Guidelines

- Keep the app simple and focused on its core purpose
- Ensure all user-facing text is properly internationalized
- Follow Material Design 3 guidelines
- Test thoroughly before submitting PRs
- Be respectful and constructive in all communications

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
