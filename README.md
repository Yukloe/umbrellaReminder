# Umbrella Reminder Android App

A simple Android app that sends daily notifications at 7:30 AM Paris time to inform you whether you need to take an umbrella based on rain forecasts for your current location.

## Features

- **Daily Notifications**: Automatically checks weather at 7:30 AM Paris time every day
- **Location-Based**: Uses your current location to get accurate weather forecasts
- **Smart Detection**: Analyzes rain probability between 8 AM - 6 PM
- **Free Weather Data**: Uses Open-Meteo API for free weather information
- **Material Design**: Clean, modern UI following Material 3 design principles
- **Boot Persistent**: Automatically reschedules notifications after device restart

## How It Works

1. **Location**: The app gets your current location using GPS/network services
2. **Weather API**: Fetches 24-hour weather forecast from Open-Meteo API
3. **Rain Analysis**: Checks precipitation probability and weather codes for rain between 8 AM - 6 PM
4. **Notification**: Sends a notification at 7:30 AM Paris time with the recommendation
5. **Scheduling**: Uses WorkManager for reliable background execution

## Requirements

- Android 8.0 (API level 26) or higher
- Location permission (GPS/Network)
- Notification permission (Android 13+)
- Internet connection for weather data

## Permissions Required

- `ACCESS_FINE_LOCATION` - Get precise location for weather forecasts
- `ACCESS_COARSE_LOCATION` - Get approximate location as fallback
- `INTERNET` - Fetch weather data from API
- `POST_NOTIFICATIONS` - Send daily notifications (Android 13+)
- `RECEIVE_BOOT_COMPLETED` - Reschedule notifications after device restart

## Weather Data

The app uses the Open-Meteo API which provides:
- Free weather data with no API key required
- Hourly precipitation probability forecasts
- Global coverage
- Reliable weather information

## Installation

1. Clone this repository
2. Open in Android Studio
3. Build and run on your Android device or emulator
4. Grant location and notification permissions when prompted
5. Enable daily notifications using the toggle in the app

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

## Configuration

The app is configured to:
- Send notifications at 7:30 AM Paris time (Europe/Paris timezone)
- Check for rain between 8 AM - 6 PM
- Consider rain likely if precipitation probability > 30% or rain weather codes detected
- Reschedule automatically after device restart

## Weather Codes for Rain

The app considers the following WMO weather codes as rain:
- 51, 53, 55: Slight/Moderate/Dense drizzle
- 56, 57: Freezing drizzle
- 61, 63, 65: Slight/Moderate/Heavy rain
- 66, 67: Freezing rain
- 80, 81, 82: Slight/Moderate/Heavy rain showers

## Troubleshooting

**Notifications not appearing:**
- Check notification permissions in app settings
- Ensure the app is not blocked from showing notifications
- Verify the toggle is enabled in the app

**Location issues:**
- Ensure GPS is enabled
- Check location permissions for the app
- Try moving to an area with better GPS reception

**Weather data not loading:**
- Check internet connection
- Verify location services are working
- Try the "Check Weather Now" button for immediate testing

## License

This project is open source and available under the MIT License.
