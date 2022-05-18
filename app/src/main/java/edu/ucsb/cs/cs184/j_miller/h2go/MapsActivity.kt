package edu.ucsb.cs.cs184.j_miller.h2go

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.ucsb.cs.cs184.j_miller.h2go.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mLocProvider: FusedLocationProviderClient
    private var mLocation: LatLng? = null
    private var mLocMarker: Marker? = null

    private var locationPermissionGranted = false
    private val LOCATION_REQUEST_CODE = 101
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ask user for permission to use location data
        setupPermissions()

        if (locationPermissionGranted) {
            mLocProvider = LocationServices.getFusedLocationProviderClient(this)
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (locationPermissionGranted) {
            startLocationUpdates()
        }
    }

    /* Check if the app already has permission to access location
     * if it doesn't, prompt the user to give permission
     * After this function, locationPermissionGranted should be correctly set based on user decision
     */
    private fun setupPermissions() {
        val locationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            Log.i("MapsActivity", "Permission denied")
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Location permission is required to show your location on the map. " +
                        "You can still view the map without it.")
                    .setTitle("Permission required")
                builder.setPositiveButton("OK") { dialog, id ->
                    Log.i("MapsActivity", "Clicked")
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_REQUEST_CODE)
                }
                val dialog = builder.create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST_CODE)
            }
        } else {
            locationPermissionGranted = true
        }
    }

    /* Called when returning from asking user for permission to access location */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                locationPermissionGranted =
                    (!grantResults.isEmpty()) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    /* Gets the device's current position and updates the location marker accordingly
     * If location marker has not been initialized yet, initialize it
     */
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (locationPermissionGranted) {
            // get the new location of the device
            val newLocation = mLocProvider.lastLocation

            // once the location call is complete, update the location marker
            newLocation.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mLocation = LatLng(task.result.latitude, task.result.longitude)

                    // if location marker has not been initialized, initialize it now
                    if (mLocation != null) {
                        if (mLocMarker == null) {
                            mLocMarker = mMap.addMarker(
                                MarkerOptions()
                                    .position(mLocation!!)
                                    .title("You are Here")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_icon_small))
                            )
                        } else { // otherwise just change its position to the new location
                            mLocMarker!!.position = mLocation!!
                        }
                    }
                }
            }
        }
    }

    /* Initializes periodic updates on the device location */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 100
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (loc in locationResult.locations) {
                    getLocation()
                }
            }
        }

        mLocProvider.requestLocationUpdates(locationRequest, locationCallback,
            Looper.getMainLooper())
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLoadedCallback(this)
        val db = Firebase.firestore
        var ucenBottleRefill : LatLng
        db.collection("filling_locations")
            .get()
            .addOnSuccessListener { result ->
                for (location in result) {
                    ucenBottleRefill = LatLng(location.data["lat"] as Double, location.data["long"] as Double)
                    mMap.addMarker(MarkerOptions()
                        .position(ucenBottleRefill)
                        .title(location.data["title"] as String)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                    if (location.id == "primary_location") {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ucenBottleRefill, 12.0f))
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }


        if (locationPermissionGranted) {
            getLocation()
        }
    }

    override fun onMapLoaded() {
        val startBounds = LatLngBounds(
            LatLng(34.403852, -119.854348),
            LatLng(34.419395, -119.839153)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(startBounds,1))

        val maxBounds = LatLngBounds(
            LatLng(34.406254, -119.884921),
            LatLng(34.419395, -119.839153)
        )
        mMap.setLatLngBoundsForCameraTarget(maxBounds)
    }
}