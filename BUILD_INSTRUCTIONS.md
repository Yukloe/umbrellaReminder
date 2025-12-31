# Build Instructions for APK

## Method 1: Using Android Studio (Recommended)
1. Open Android Studio
2. Open the project folder
3. Wait for Gradle sync to complete
4. Select your device from the dropdown
5. Click Run (▶️) or Build → Build Bundle(s)/APK(s) → Build APK(s)
6. Find the APK in `app/build/outputs/apk/debug/`

## Method 2: Using Command Line
```bash
# Navigate to project directory
cd c:\Users\hugop\CascadeProjects\windsurf-project

# Build debug APK
./gradlew assembleDebug

# Find APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Install APK on Phone
1. Transfer the APK file to your phone (USB, email, etc.)
2. Enable "Install from unknown sources" in phone settings
3. Tap the APK file to install
4. Grant permissions when prompted
5. Open the app and enable notifications

## Troubleshooting
- If build fails, check Android SDK installation
- Ensure your phone has Android 8.0+ (API 26+)
- Make sure all permissions are granted
- Check that location services are enabled on your phone
