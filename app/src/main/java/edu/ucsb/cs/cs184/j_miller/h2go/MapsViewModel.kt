package edu.ucsb.cs.cs184.j_miller.h2go

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition

class MapsViewModel : ViewModel() {
    var cameraPosition: CameraPosition? = null
    var mapImage : BitmapDescriptor? = null
    var showMapImage: Boolean = true
}