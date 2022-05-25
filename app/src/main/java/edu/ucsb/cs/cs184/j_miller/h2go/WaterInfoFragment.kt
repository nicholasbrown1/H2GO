package edu.ucsb.cs.cs184.j_miller.h2go

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class WaterInfoFragment: Fragment() {

    private lateinit var viewModel: WaterInfoViewModel
    private lateinit var titleField: TextView
    private lateinit var latitude: TextView
    private lateinit var longitude: TextView
    private lateinit var typeField: TextView
    private lateinit var floorField: TextView
    private lateinit var closeButton: ImageButton
    private var db = Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.water_info_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(WaterInfoViewModel::class.java)
        titleField = view.findViewById<TextView>(R.id.title_text)
        latitude = view.findViewById<TextView>(R.id.latitude_value)
        longitude = view.findViewById<TextView>(R.id.longitude_value)
        typeField = view.findViewById<TextView>(R.id.type_value)
        floorField = view.findViewById<TextView>(R.id.floor_value)
        closeButton = view.findViewById<ImageButton>(R.id.close_button)

        if (this.arguments != null) {
            viewModel.latitude = this.requireArguments().getDouble("latitude")
            viewModel.longitude = this.requireArguments().getDouble("longitude")
            loadData()
        }
        latitude.text = viewModel.latitude.toString()
        longitude.text = viewModel.longitude.toString()
        viewModel.titleText.observe(viewLifecycleOwner) {
            titleField.text = it
        }
        viewModel.floorText.observe(viewLifecycleOwner) {
            floorField.text = it
        }
        viewModel.typeText.observe(viewLifecycleOwner) {
            typeField.text = it
        }


        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadData() {
        db.collection("filling_locations")
            .get()
            .addOnSuccessListener { result ->
                for (location in result) {
                    if(location.data["lat"] == viewModel.latitude && location.data["long"] == viewModel.longitude){
                        viewModel.editTitle(location.data["title"] as String)
                        viewModel.editFloor(location.data["floor"] as String)
                        viewModel.editType(location.data["type"] as String)
                        break
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
                    if(location.data["approved"] as Boolean) {
                        if (location.data["lat"] == viewModel.latitude && location.data["long"] == viewModel.longitude) {
                            viewModel.editTitle(location.data["title"] as String)
                            viewModel.editFloor(location.data["floor"] as String)
                            viewModel.editType(location.data["type"] as String)
                            break
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }
    }
}