package edu.ucsb.cs.cs184.j_miller.h2go

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.ucsb.cs.cs184.j_miller.h2go.databinding.ActivityMapsBinding
import kotlin.reflect.typeOf

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    lateinit var auth: FirebaseAuth
    private lateinit var mMap: GoogleMap
    private lateinit var mLocProvider: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher : ActivityResultLauncher<String>
    private var mLocation: LatLng? = null
    private var mLocMarker: Marker? = null
    private var mMarkers: MutableList<Marker> = mutableListOf()
    private var toolbarTitle: String = ""

    private val LOCATION_REQUEST_CODE = 101
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fab: FloatingActionButton
    private val ucsbBounds : LatLngBounds  = LatLngBounds(
        LatLng(34.403852, -119.854348),
        LatLng(34.419395, -119.839153)
    )
    private val mapZoom : Float = 15.0f
    private val db = Firebase.firestore

    private lateinit var filterViewModel: FilterViewModel


    private var locationPermissionGranted = false
        set(value) {
            field = value
            updateUI()
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.toolbar_menu, menu)
        findViewById<Toolbar>(R.id.my_toolbar).title = toolbarTitle
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if(::auth.isInitialized) {
            menu?.findItem(R.id.action_logout)?.isVisible = auth.currentUser != null
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_login -> {

            val loginFragment = LoginFragment()
            this.supportFragmentManager.beginTransaction()
                .add(R.id.frameLayout, loginFragment, "addLoginFragment")
                .addToBackStack(null).commit()

            true
        }

        R.id.action_logout -> {
            auth.signOut()
            updateUI()
            true
        }

        R.id.action_filter -> {
            val bundle = Bundle()
            bundle.putBoolean("loggedIn",auth.currentUser != null)
            val filterFragment = FilterFragment()
            filterFragment.arguments = bundle
            this.supportFragmentManager.beginTransaction()
                .add(R.id.frameLayout, filterFragment, "addFilterFragment")
                .addToBackStack(null).commit()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        filterViewModel = ViewModelProvider(this).get(FilterViewModel::class.java)

        fab = findViewById(R.id.fab)
        fab.hide()

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

            // when backStack is empty, no fragments are open so show FAB
            // and update filling locations in case one was just added by fragment
            this.supportFragmentManager.addOnBackStackChangedListener {
                if (this.supportFragmentManager.backStackEntryCount == 0) {
                    fab.show()
                    showFillingLocations()
                } else { // when backStack is not empty, fragment is open so hide FAB
                    fab.hide()
                }
            }

            // if have location, set FAB to open fragment to add sources to database
            fab.setOnClickListener {
                getLocation()
                val bundle = Bundle()
                bundle.putDouble("latitude",mLocation!!.latitude)
                bundle.putDouble("longitude",mLocation!!.longitude)

                val addSourceFragment = AddSourceFragment()
                addSourceFragment.arguments = bundle
                this.supportFragmentManager.beginTransaction()
                    .add(R.id.frameLayout, addSourceFragment, "addSourceFragment")
                    .addToBackStack(null).commit()
            }
        } else { // if don't have location permission, hide fab
            fab.hide()
        }
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    fun updateUI() {
        if (locationPermissionGranted && this.supportFragmentManager.backStackEntryCount==0)
            fab.show()
        else
            fab.hide()

        // Check if user is signed in (non-null) and update UI accordingly.
        //auth = Firebase.auth
        val currentUser = auth.currentUser
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
            if (currentUser != null) {
                toolbarTitle = getString(R.string.app_name) + " -  " + currentUser.email
            } else {
                toolbarTitle = getString(R.string.app_name) + " - not signed in "
            }
        toolbar.title = toolbarTitle
        invalidateOptionsMenu()
    }

    /* Check if the app already has permission to access location
     * if it doesn't, prompt the user to give permission
     * After this function, locationPermissionGranted should be correctly set based on user decision
     */
    private fun setupPermissions() {
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                locationPermissionGranted = isGranted
            }
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
                if (task.isSuccessful && task.result != null) {
                    mLocation = LatLng(task.result.latitude, task.result.longitude)

                    // if location marker has not been initialized, initialize it now
                    if (mLocMarker == null && ::mMap.isInitialized) {
                        mLocMarker = mMap.addMarker( //lateinit property mMap has not been initialized
                            MarkerOptions()
                                .position(mLocation!!)
                                .title("You are Here")
                                .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.location_icon_small))
                        )
                    } else { // otherwise just change its position to the new location
                        mLocMarker!!.position = mLocation!!
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

    private fun resetMarkers() {
        for (marker in mMarkers)
            marker.remove()
        mMarkers.clear()
    }

    /* returns true iff the location matches all applied filters */
    private fun checkFilters(location: QueryDocumentSnapshot): Boolean {
        if (!filterViewModel.hydrationFilter || location.data["hydration_station"] as Boolean) {
            if (!filterViewModel.drinkingFilter || location.data["drinking_fountain"] as Boolean) {
                if (!filterViewModel.ratingsFilter
                    || location.data["rating"].toString().toDouble() >= filterViewModel.rating) {
                    if (!filterViewModel.favoritesFilter || true) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /* Display the locations of all filling locations in the database */
    fun showFillingLocations() {
        resetMarkers()
        db.collection("filling_locations")
            .get()
            .addOnSuccessListener { result ->
                for (location in result) {
                    if (checkFilters(location)) {
                        val fillingLoc = LatLng(
                            location.data["lat"] as Double,
                            location.data["long"] as Double
                        )
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(fillingLoc)
                                .title(location.data["title"] as String)
                                .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_BLUE
                                    )
                                )
                        )
                        if (marker != null) {
                            mMarkers.add(marker)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }
        db.collection("user_filling_locations")
            .get()
            .addOnSuccessListener { result ->
                for (location in result) {
                    if (checkFilters(location)) {
                        val fillingLoc = LatLng(
                            location.data["lat"] as Double,
                            location.data["long"] as Double
                        )
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(fillingLoc)
                                .title(location.data["title"] as String)
                                .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_BLUE
                                    )
                                )
                        )
                        if (marker != null) {
                            mMarkers.add(marker)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }
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
        mMap.setOnMarkerClickListener { marker ->

            /* If there's already a fragment up, don't do anything. */
            if (this.supportFragmentManager.backStackEntryCount!=0)
                return@setOnMarkerClickListener true

            val bundle = Bundle()
            bundle.putDouble("latitude",marker.position.latitude)
            bundle.putDouble("longitude",marker.position.longitude)

            val waterInfoFragment = WaterInfoFragment()
            waterInfoFragment.arguments = bundle
            this.supportFragmentManager.beginTransaction()
                .add(R.id.frameLayout, waterInfoFragment, "waterInfoFragment")
                .addToBackStack(null).commit()
            true
        }
        val startBounds = ucsbBounds
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startBounds.center, mapZoom))

        val maxBounds = ucsbBounds
        mMap.setLatLngBoundsForCameraTarget(maxBounds)

        if (locationPermissionGranted) {
            getLocation()
        }
    }

    override fun onMapLoaded() {
        showFillingLocations()
    }
}