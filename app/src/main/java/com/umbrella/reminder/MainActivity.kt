package com.umbrella.reminder

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
import com.umbrella.reminder.databinding.ActivityMainBinding
import com.umbrella.reminder.location.LocationManager
import com.umbrella.reminder.notification.NotificationHelper
import com.umbrella.reminder.scheduler.NotificationScheduler
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
            notificationScheduler.scheduleDailyNotification()
            Snackbar.make(
                binding.root,
                R.string.notification_enabled,
                Snackbar.LENGTH_SHORT
            ).show()
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
                val locationResult = locationManager.getLastKnownLocation()
                if (locationResult.isFailure) {
                    binding.statusText.text = "Unable to get location"
                    return@launch
                }

                val location = locationResult.getOrThrow()
                
                // Use the same logic as WeatherCheckWorker
                val weatherRepository = com.umbrella.reminder.data.WeatherRepository()
                val weatherResult = weatherRepository.getWeatherForecast(location.latitude, location.longitude)
                
                if (weatherResult.isFailure) {
                    binding.statusText.text = "Unable to fetch weather data"
                    return@launch
                }

                val weatherResponse = weatherResult.getOrThrow()
                val shouldTakeUmbrella = weatherRepository.shouldTakeUmbrella(weatherResponse)
                
                // Send notification
                notificationHelper.showUmbrellaNotification(shouldTakeUmbrella)
                
                binding.statusText.text = "Weather check complete! Check your notifications."
                
            } catch (e: Exception) {
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
            binding.statusText.text = "Daily notifications enabled"
        } else {
            binding.statusText.text = "Notifications disabled"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }
}
