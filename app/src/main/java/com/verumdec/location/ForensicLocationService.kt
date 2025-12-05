package com.verumdec.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Forensic Location Service for Verum Omnis
 *
 * Captures GPS location data for forensic evidence with high accuracy.
 * Follows offline-first principle - no network requests for location.
 */
class ForensicLocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Gets current location with high accuracy for forensic evidence
     */
    suspend fun getCurrentLocation(): ForensicLocation = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) {
            cont.resumeWithException(SecurityException("Location permission not granted"))
            return@suspendCancellableCoroutine
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = result.lastLocation
                if (location != null && cont.isActive) {
                    cont.resume(location.toForensicLocation())
                } else if (cont.isActive) {
                    cont.resumeWithException(Exception("Unable to get location"))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            cont.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: SecurityException) {
            cont.resumeWithException(e)
        }
    }

    /**
     * Gets last known location (may be null or outdated)
     */
    suspend fun getLastKnownLocation(): ForensicLocation? = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (cont.isActive) {
                    cont.resume(location?.toForensicLocation())
                }
            }.addOnFailureListener {
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        } catch (_: SecurityException) {
            cont.resume(null)
        }
    }

    /**
     * Checks if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Extension function to convert Android Location to ForensicLocation
     */
    private fun Location.toForensicLocation(): ForensicLocation = ForensicLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else null,
        accuracy = if (hasAccuracy()) accuracy else null,
        bearing = if (hasBearing()) bearing else null,
        speed = if (hasSpeed()) speed else null,
        timestamp = Instant.ofEpochMilli(time),
        provider = provider ?: "unknown"
    )
}

/**
 * Represents a forensic location capture
 */
data class ForensicLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val bearing: Float?,
    val speed: Float?,
    val timestamp: Instant,
    val provider: String
) {
    /**
     * Converts location to map for reporting
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "latitude" to latitude,
        "longitude" to longitude,
        "altitude" to altitude,
        "accuracy" to accuracy,
        "bearing" to bearing,
        "speed" to speed,
        "timestamp" to timestamp.toString(),
        "provider" to provider
    )

    /**
     * Returns formatted coordinates string
     */
    fun toCoordinatesString(): String =
        "%.6f, %.6f".format(latitude, longitude)

    /**
     * Returns Google Maps URL for the location
     */
    fun toMapsUrl(): String =
        "https://maps.google.com/?q=$latitude,$longitude"
}
