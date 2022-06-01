package edu.ucsb.cs.cs184.j_miller.h2go

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
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
    private lateinit var ratingField: TextView
    private lateinit var ratingButton: Button
    private lateinit var ratingEntry: Spinner
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
        ratingField = view.findViewById<TextView>(R.id.rating)
        ratingButton = view.findViewById<Button>(R.id.rate_button)
        ratingEntry = view.findViewById<Spinner>(R.id.rate_entry)
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
        viewModel.ratingText.observe(viewLifecycleOwner) {
            ratingField.text = it
        }
        if((requireActivity() as MapsActivity).auth.currentUser == null) {
            ratingEntry.isVisible = false
            ratingButton.isVisible = false

        }
        val ratings = arrayOf("1","2","3","4","5")
        val ratingsAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ratings)
        ratingEntry.adapter = ratingsAdapter
        ratingButton.setOnClickListener {
            db.collection("ratings")
                .get()
                .addOnSuccessListener { result ->
                    var hasRated = false
                    for (rating in result) {
                        if(rating.data["lat"] == viewModel.latitude && rating.data["long"] == viewModel.longitude && rating.data["user"] ==(requireActivity() as MapsActivity).auth.currentUser?.email) {
                            hasRated = true
                            rating.reference.update("rating", ratingEntry.selectedItem.toString().toDouble())
                        }
                    }
                    if(!hasRated) {
                        val entry = hashMapOf(
                            "lat" to viewModel.latitude,
                            "long" to viewModel.longitude,
                            "rating" to ratingEntry.selectedItem.toString().toDouble(),
                            "user" to (requireActivity() as MapsActivity).auth.currentUser?.email
                        )
                        db.collection("ratings").document().set(entry)
                            .addOnFailureListener { exception ->
                                Log.i("firebase_write", "set failed with ", exception)
                            }
                    }
                    updateRating()
                }
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
        updateRating()
    }
    private fun updateRating() {
        db.collection("ratings")
            .get()
            .addOnSuccessListener { result ->
                var sum: Double = 0.0
                var numRatings = 0
                for (rating in result) {
                    if(rating.data["lat"] == viewModel.latitude && rating.data["long"] == viewModel.longitude) {
                        numRatings += 1
                        sum += rating.data["rating"] as Double
                    }
                }
                if(numRatings > 0) {
                    viewModel.editRating(String.format("Rating: %.1f ($numRatings review${if (numRatings != 1) "s" else ""})", sum / numRatings))
                } else {
                    viewModel.editRating("Rating: N/A (0 reviews)")
                }
            }
    }
}