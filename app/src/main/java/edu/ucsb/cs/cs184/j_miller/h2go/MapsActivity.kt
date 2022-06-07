package edu.ucsb.cs.cs184.j_miller.h2go


import android.Manifest
import edu.ucsb.cs.cs184.j_miller.h2go.R
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
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
import kotlin.concurrent.fixedRateTimer


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    lateinit var auth: FirebaseAuth
    private lateinit var viewModel: MapsViewModel
    private lateinit var mMap: GoogleMap
    private lateinit var mLocProvider: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher : ActivityResultLauncher<String>
    private var mLocation: LatLng? = null
    private var mLocMarker: Marker? = null
    private var mMarkers: MutableList<Marker> = mutableListOf()
    private var toolbarTitle: String = ""
    private val reso = if (android.os.Build.VERSION.SDK_INT >= 24) 1500 else 900

    private val LOCATION_REQUEST_CODE = 101
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fab: FloatingActionButton
    private val ucsbBounds : LatLngBounds  = LatLngBounds(
        LatLng(34.403852, -119.854348),
        LatLng(34.419395, -119.839153)
    )
    private val maxBounds : LatLngBounds = LatLngBounds(
        LatLng(34.406254, -119.884921),
        LatLng(34.419395, -119.839153)
    )
    private val startMapZoom : Float = 16.0f
    private val minMapZoom : Float = 14.0f
    private val showLabelStyle = MapStyleOptions("[\n" +
            "  {\n" +
            "    \"elementType\": \"labels\",\n" +
            "    \"stylers\": [\n" +
            "      {\n" +
            "        \"visibility\": \""+"on"+"\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]")
    private val hideLabelStyle = MapStyleOptions("[\n" +
            "  {\n" +
            "    \"elementType\": \"labels\",\n" +
            "    \"stylers\": [\n" +
            "      {\n" +
            "        \"visibility\": \""+"off"+"\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]")
    private lateinit var mapImageButton: ImageButton
    private lateinit var mapOverlay: GroundOverlay
    private val db = Firebase.firestore

    private lateinit var filterViewModel: FilterViewModel


    private var locationPermissionGranted = false

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

            /* Basically, don't do anything if a fragment's already up.
            * Same code for the other cases; there may be a prettier way of
            * accomplishing this. */
            if (this.supportFragmentManager.backStackEntryCount != 0)
                true

            val loginFragment = LoginFragment()
            this.supportFragmentManager.beginTransaction()
                .add(R.id.frameLayout, loginFragment, "addLoginFragment")
                .addToBackStack(null).commit()

            true
        }

        R.id.action_logout -> {

            if (this.supportFragmentManager.backStackEntryCount != 0)
                true

            auth.signOut()
            updateUI()
            true
        }

        R.id.action_filter -> {

            if (this.supportFragmentManager.backStackEntryCount != 0)
                true

            val bundle = Bundle()
            if (auth.currentUser != null)
                bundle.putString("userID",auth.currentUser!!.uid)
            else
                bundle.putString("userID","")
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
        viewModel = ViewModelProvider(this)[MapsViewModel::class.java]

        auth = Firebase.auth

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        filterViewModel = ViewModelProvider(this)[FilterViewModel::class.java]

        fab = findViewById(R.id.fab)
        fab.hide()

        mapImageButton = findViewById(R.id.map_image_toggle)

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

        } else { // if don't have location permission, hide fab
            fab.hide()
        }
        // if have location, set FAB to open fragment to add sources to database
        fab.setOnClickListener {
            if(locationPermissionGranted && mLocation != null) {
                getLocation()
                val bundle = Bundle()
                bundle.putDouble("latitude", mLocation!!.latitude)
                bundle.putDouble("longitude", mLocation!!.longitude)

                val addSourceFragment = AddSourceFragment()
                addSourceFragment.arguments = bundle
                this.supportFragmentManager.beginTransaction()
                    .add(R.id.frameLayout, addSourceFragment, "addSourceFragment")
                    .addToBackStack(null).commit()
            }
        }

        mapImageButton.setOnClickListener {
            viewModel.showMapImage = !viewModel.showMapImage
            updateUI()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(::mMap.isInitialized) {
            viewModel.cameraPosition = mMap.cameraPosition
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
            if(::mMap.isInitialized) {
                mapOverlay.transparency = if (viewModel.showMapImage) 0.0f else 1.0f
                val labelEnable = if (viewModel.showMapImage) hideLabelStyle else showLabelStyle
                mMap.setMapStyle(
                    labelEnable
                )
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
        if (locationPermissionGranted) {
            mLocProvider = LocationServices.getFusedLocationProviderClient(this)
            startLocationUpdates()
        }
        updateUI()
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
                        mLocMarker = mMap.addMarker(
                            MarkerOptions()
                                .zIndex(9999f)
                                .position(mLocation!!)
                                .title("You are Here")
                                .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.location_icon_small))
                        )
                    } else { // otherwise just change its position to the new location
                        if(mLocMarker != null && mLocation != null) {
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

    private fun resetMarkers() {
        for (marker in mMarkers)
            marker.remove()
        mMarkers.clear()
    }

    /* update the cache of current user's favorites list */
    fun updateFavorites() {
        if (auth.currentUser != null) {
            Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get()
                .addOnSuccessListener { result ->
                    if (result.data != null) {
                        filterViewModel.favorites = result.data!!["favorites"] as List<String>
                    }
                }
                .addOnFailureListener { exception ->
                    Log.i("firebase_read", "get failed with ", exception)
                }
        }
    }

    /* returns true if the current user has the source with locID as a favorite */
    fun isFavorite(locID: String): Boolean {
        if (filterViewModel.favorites != null)
            return filterViewModel.favorites!!.contains(locID)
        return false
    }

    /* returns true iff the location matches all applied filters */
    private fun checkFilters(location: QueryDocumentSnapshot): Boolean {
        if (!filterViewModel.hydrationFilter || location.data["hydration_station"] as Boolean) {
            if (!filterViewModel.drinkingFilter || location.data["drinking_fountain"] as Boolean) {
                if (!filterViewModel.ratingsFilter
                    || location.data["rating"].toString().toDouble() >= filterViewModel.rating) {
                    if (!filterViewModel.favoritesFilter
                        || (filterViewModel.favorites != null
                                && filterViewModel.favorites!!.contains(location.id))) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /* Display the locations of all filling locations in the database */
    private fun showFillingLocations() {
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
                    if ((location.data["approved"] as Boolean ) && checkFilters(location)) {
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

            if (marker == mLocMarker)
                return@setOnMarkerClickListener true
            /* If there's already a fragment up, don't do anything. */
            if (this.supportFragmentManager.backStackEntryCount!=0)
                return@setOnMarkerClickListener true

            val bundle = Bundle()
            bundle.putDouble("latitude",marker.position.latitude)
            bundle.putDouble("longitude",marker.position.longitude)

            if (auth.currentUser != null) {
                updateFavorites()
            }
            val waterInfoFragment = WaterInfoFragment()
            waterInfoFragment.arguments = bundle
            this.supportFragmentManager.beginTransaction()
                .add(R.id.frameLayout, waterInfoFragment, "waterInfoFragment")
                .addToBackStack(null).commit()
            true
        }
        val startBounds = ucsbBounds
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startBounds.center, startMapZoom))

        mMap.setLatLngBoundsForCameraTarget(maxBounds)

        mMap.setMinZoomPreference(minMapZoom)
        if(viewModel.cameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(viewModel.cameraPosition!!))
        }

        if(viewModel.mapImage == null) {
            //viewModel.mapImage = decodeSampledBitmapFromResource(resources, R.drawable.campus_map, 1500, 1500)
            viewModel.mapImage = BitmapDescriptorFactory.fromBitmap(decodeSampledBitmapFromResource(resources, R.drawable.campus_map, reso, reso))
        }
        val ucsbAnchorLatLng = LatLng(34.416020, -119.848047)
        val mapOverlayOptions = GroundOverlayOptions()
            .image(viewModel.mapImage!!)
            .position(ucsbAnchorLatLng, 1897.5f, 2415f)
        //mapOverlayOptions.anchor(0.493412f,0.477640f)
        mapOverlayOptions.anchor(0.493017f,0.477329f)
        mapOverlay = mMap.addGroundOverlay(mapOverlayOptions)!!

        if (locationPermissionGranted) {
            getLocation()
        }
        updateUI()
    }

    override fun onMapLoaded() {
        showFillingLocations()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun decodeSampledBitmapFromResource(
        res: Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            BitmapFactory.decodeResource(res, resId, this)
        }
    }
}
