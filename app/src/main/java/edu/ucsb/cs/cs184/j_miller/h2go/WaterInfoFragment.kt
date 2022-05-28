package edu.ucsb.cs.cs184.j_miller.h2go

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
        typeField = view.findViewById<TextView>(R.id.building_value)
        floorField = view.findViewById<TextView>(R.id.location_value)
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

    private fun getType(hydration_station: Boolean, drinking_fountain: Boolean): String {
        if (hydration_station and drinking_fountain)
            return "Hydration Station and Drinking Fountain"
        if (hydration_station)
            return "Hydration Station"
        return "Drinking Fountain"
    }

    private fun loadData() {
        db.collection("filling_locations")
            .get()
            .addOnSuccessListener { result ->
                for (location in result) {
                    if(location.data["lat"] == viewModel.latitude && location.data["long"] == viewModel.longitude){
                        viewModel.editTitle(location.data["title"] as String)
                        viewModel.editFloor(location.data["floor"] as String)
                        viewModel.editType(getType(location.data["hydration_station"] as Boolean
                            , location.data["drinking_fountain"] as Boolean))
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
                            viewModel.editType(getType(location.data["hydration_station"] as Boolean
                                , location.data["drinking_fountain"] as Boolean))
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