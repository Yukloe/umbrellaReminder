package com.umbrellareminder.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.umbrellareminder.app.databinding.ActivityMainBinding
import com.umbrellareminder.app.location.LocationManager
import com.umbrellareminder.app.notification.NotificationHelper
import com.umbrellareminder.app.scheduler.NotificationScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var locationManager: LocationManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupUI()
        checkAndRequestPermissions()
    }

    private fun initializeComponents() {
        notificationScheduler = NotificationScheduler(this)
        notificationHelper = NotificationHelper(this)
        locationManager = LocationManager(this)
    }

    private fun setupUI() {
        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableNotifications()
            } else {
                disableNotifications()
            }
        }

        binding.checkNowButton.setOnClickListener {
            checkWeatherNow()
        }

        // Update UI based on current state
        updateUIState()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!locationManager.hasLocationPermission()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationHelper.hasNotificationPermission()) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        
        if (!allGranted) {
            showPermissionDeniedDialog()
        } else {
            updateUIState()
            Snackbar.make(
                binding.root,
                R.string.notification_enabled,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_location_title)
            .setMessage(R.string.permission_location_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.deny_permission, null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun enableNotifications() {
        if (hasAllPermissions()) {
            // Get location name for the status message
            lifecycleScope.launch {
                try {
                    val locationResult = locationManager.getLastKnownLocation()
                    if (locationResult.isSuccess) {
                        val location = locationResult.getOrThrow()
                        val weatherRepository = com.umbrellareminder.app.data.WeatherRepository()
                        val locationNameResult = weatherRepository.getLocationName(location.latitude, location.longitude)
                        val locationName = locationNameResult.getOrNull() ?: "your area"
                        
                        val message = "Daily notifications enabled at 7:30 AM in $locationName"
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, R.string.notification_enabled, Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, R.string.notification_enabled, Snackbar.LENGTH_SHORT).show()
                }
            }
            
            notificationScheduler.scheduleDailyNotification()
        } else {
            binding.toggleButton.isChecked = false
            checkAndRequestPermissions()
        }
    }

    private fun disableNotifications() {
        notificationScheduler.cancelDailyNotification()
        Snackbar.make(
            binding.root,
            R.string.notification_disabled,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun checkWeatherNow() {
        if (!locationManager.hasLocationPermission()) {
            checkAndRequestPermissions()
            return
        }

        binding.checkNowButton.isEnabled = false
        binding.statusText.setText(R.string.checking_weather)

        lifecycleScope.launch {
            try {
                android.util.Log.d("MainActivity", "Starting weather check...")
                
                val locationResult = locationManager.getLastKnownLocation()
                if (locationResult.isFailure) {
                    android.util.Log.e("MainActivity", "Location failed: ${locationResult.exceptionOrNull()?.message}")
                    binding.statusText.text = "Unable to get location"
                    return@launch
                }

                val location = locationResult.getOrThrow()
                android.util.Log.d("MainActivity", "Location obtained: ${location.latitude}, ${location.longitude}")
                
                // Use the same logic as WeatherCheckWorker
                val weatherRepository = com.umbrellareminder.app.data.WeatherRepository()
                val weatherResult = weatherRepository.getWeatherForecast(location.latitude, location.longitude)
                
                if (weatherResult.isFailure) {
                    android.util.Log.e("MainActivity", "Weather API failed: ${weatherResult.exceptionOrNull()?.message}")
                    binding.statusText.text = "Unable to fetch weather data"
                    return@launch
                }

                val weatherResponse = weatherResult.getOrThrow()
                android.util.Log.d("MainActivity", "Weather response received")
                
                // Get location name
                val locationNameResult = weatherRepository.getLocationName(
                    location.latitude,
                    location.longitude
                )
                val locationName = locationNameResult.getOrNull()
                android.util.Log.d("MainActivity", "Location name: $locationName")
                
                // Get current temperature
                val currentTemperature = weatherResponse.currentWeather?.temperature 
                    ?: weatherResponse.hourly.temperatures.firstOrNull()
                android.util.Log.d("MainActivity", "Current temperature: $currentTemperature")
                
                val shouldTakeUmbrella = weatherRepository.shouldTakeUmbrella(weatherResponse)
                android.util.Log.d("MainActivity", "Should take umbrella: $shouldTakeUmbrella")
                
                // Send notification with location and temperature
                notificationHelper.showUmbrellaNotification(
                    shouldTakeUmbrella,
                    locationName,
                    currentTemperature
                )
                
                binding.statusText.text = "Weather check complete! Check your notifications."
                android.util.Log.d("MainActivity", "Weather check completed successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in weather check: ${e.message}", e)
                binding.statusText.text = "Error: ${e.message}"
            } finally {
                binding.checkNowButton.isEnabled = true
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return locationManager.hasLocationPermission() && notificationHelper.hasNotificationPermission()
    }

    private fun updateUIState() {
        val hasPermissions = hasAllPermissions()
        val isScheduled = notificationScheduler.isNotificationScheduled()
        
        binding.toggleButton.isEnabled = hasPermissions
        binding.toggleButton.isChecked = isScheduled
        binding.checkNowButton.isEnabled = hasPermissions
        
        if (!hasPermissions) {
            binding.statusText.text = "Permissions required"
        } else if (isScheduled) {
            // Try to get location name for the status
            lifecycleScope.launch {
                try {
                    val locationResult = locationManager.getLastKnownLocation()
                    if (locationResult.isSuccess) {
                        val location = locationResult.getOrThrow()
                        val weatherRepository = com.umbrellareminder.app.data.WeatherRepository()
                        val locationNameResult = weatherRepository.getLocationName(location.latitude, location.longitude)
                        val locationName = locationNameResult.getOrNull()
                        
                        if (locationName != null) {
                            binding.statusText.text = "Daily notifications enabled in $locationName"
                        } else {
                            binding.statusText.text = getString(R.string.notification_enabled)
                        }
                    } else {
                        binding.statusText.text = getString(R.string.notification_enabled)
                    }
                } catch (e: Exception) {
                    binding.statusText.text = getString(R.string.notification_enabled)
                }
            }
        } else {
            binding.statusText.text = getString(R.string.notification_disabled)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }
}
