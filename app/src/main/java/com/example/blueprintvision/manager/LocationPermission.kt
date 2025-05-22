package com.example.blueprintvision.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Manages location permissions and retrieval of the user's current location.
 */
class LocationPermission private constructor() {
    private val TAG = "LocationPermission"
    
    // LiveData for location permission status
    private val _isLocationPermissionGranted = MutableLiveData<Boolean>(false)
    val isLocationPermissionGranted: LiveData<Boolean> = _isLocationPermissionGranted
    
    // LiveData for current location
    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation
    
    // Location client
    private var fusedLocationClient: FusedLocationProviderClient? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        
        @Volatile
        private var INSTANCE: LocationPermission? = null
        
        fun getInstance(): LocationPermission {
            return INSTANCE ?: synchronized(this) {
                val instance = LocationPermission()
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Initializes location services.
     */
    fun initialize(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        checkLocationPermission(context)
    }
    
    /**
     * Checks if location permission has been granted.
     */
    fun checkLocationPermission(context: Context): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _isLocationPermissionGranted.postValue(permissionStatus)
        return permissionStatus
    }
    
    /**
     * Requests location permission from the user.
     */
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * Handles the result of the permission request.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        context: Context
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _isLocationPermissionGranted.postValue(true)
                getCurrentLocation(context)
            } else {
                _isLocationPermissionGranted.postValue(false)
                Log.d(TAG, "Location permission denied")
            }
        }
    }
    
    /**
     * Gets the current location of the device.
     */
    fun getCurrentLocation(context: Context) {
        if (checkLocationPermission(context)) {
            val cancellationToken = CancellationTokenSource()
            
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                ?.addOnSuccessListener { location ->
                    if (location != null) {
                        _currentLocation.postValue(location)
                        Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                    } else {
                        Log.d(TAG, "Location is null")
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Error getting location: ${e.message}")
                }
        } else {
            Log.d(TAG, "Location permission not granted")
        }
    }
    
    /**
     * Returns the current location or null if not available.
     */
    fun getLastKnownLocation(): Location? {
        return _currentLocation.value
    }
}
