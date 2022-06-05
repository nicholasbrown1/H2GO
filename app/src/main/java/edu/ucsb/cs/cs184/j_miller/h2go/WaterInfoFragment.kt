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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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
    private lateinit var favoriteCheckbox: CheckBox
    private var db = Firebase.firestore

    private var user: FirebaseUser? = null
    private var srcID = ""
    private var collection = ""
    private var ratingValue = 0.0
    private var totalNumRatings = 0
    private var wasFavorite = false
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
        favoriteCheckbox = view.findViewById<CheckBox>(R.id.favorite_checkbox)

        user = (requireActivity() as MapsActivity).auth.currentUser

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
        if(user == null) {
            ratingEntry.isVisible = false
            ratingButton.isVisible = false
            favoriteCheckbox.isVisible = false
        }
        val ratings = arrayOf("1","2","3","4","5")
        val ratingsAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ratings)
        ratingEntry.adapter = ratingsAdapter
        ratingButton.setOnClickListener {
            var hasRated = false
            var oldUserRating = 0.0
            var currentRating = ratingEntry.selectedItem.toString().toDouble()
            db.collection("ratings")
                .get()
                .addOnSuccessListener { result ->
                    for (rating in result) {
                        if(rating.data["lat"] == viewModel.latitude && rating.data["long"] == viewModel.longitude && rating.data["user"] ==user?.email) {
                            hasRated = true
                            oldUserRating = rating.data["rating"] as Double
                            rating.reference.update("rating", currentRating)
                        }
                    }
                    if(!hasRated) {
                        val entry = hashMapOf(
                            "lat" to viewModel.latitude,
                            "long" to viewModel.longitude,
                            "rating" to ratingEntry.selectedItem.toString().toDouble(),
                            "user" to user?.email
                        )
                        db.collection("ratings").document().set(entry)
                            .addOnFailureListener { exception ->
                                Log.i("firebase_write", "set failed with ", exception)
                            }
                        var newRating = ratingValue*totalNumRatings
                        newRating += currentRating
                        newRating /= totalNumRatings + 1
                        db.collection(collection).document(srcID).update("rating", newRating)
                        db.collection(collection).document(srcID).update("num_ratings", totalNumRatings+1)
                    } else {
                        var newRating = ratingValue*totalNumRatings
                        newRating -= oldUserRating
                        newRating += currentRating
                        newRating /= totalNumRatings
                        db.collection(collection).document(srcID).update("rating", newRating)
                    }
                    updateRating()
                }
        }
        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            if (wasFavorite xor favoriteCheckbox.isChecked) {
                updateFavorite(favoriteCheckbox.isChecked)
            }
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun updateFavorite(add: Boolean) {
        db.collection("users").document(user!!.uid).get()
            .addOnSuccessListener { result ->
                if (result.data != null) {
                    val favs = result.data!!["favorites"] as MutableList<String>
                    if (add)
                        favs.add(srcID)
                    else
                        favs.remove(srcID)
                    db.collection("users").document(user!!.uid)
                        .set(hashMapOf("favorites" to favs as List<String>),
                            SetOptions.merge())
                } else {
                    val entry = hashMapOf(
                        "email" to user!!.email,
                        "favorites" to listOf(srcID)
                    )
                    db.collection("users").document(user!!.uid).set(entry)
                        .addOnFailureListener { exception ->
                            Log.i("firebase_write", "set failed with ", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "set failed with ", exception)
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
                        srcID = location.id
                        collection = "filling_locations"
                        viewModel.editTitle(location.data["title"] as String)
                        viewModel.editFloor(location.data["floor"] as String)
                        viewModel.editType(getType(location.data["hydration_station"] as Boolean
                            , location.data["drinking_fountain"] as Boolean))
                        if (user != null && (requireActivity() as MapsActivity).isFavorite(srcID)) {
                            favoriteCheckbox.isChecked = true
                            wasFavorite = true
                        }
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
                            srcID = location.id
                            collection = "user_filling_locations"
                            viewModel.editTitle(location.data["title"] as String)
                            viewModel.editFloor(location.data["floor"] as String)
                            viewModel.editType(getType(location.data["hydration_station"] as Boolean
                                , location.data["drinking_fountain"] as Boolean))
                            if (user != null && (requireActivity() as MapsActivity).isFavorite(srcID)) {
                                favoriteCheckbox.isChecked = true
                                wasFavorite = true
                            }
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
                    totalNumRatings = numRatings
                    ratingValue = sum / numRatings
                    viewModel.editRating(String.format("Rating: %.1f ($numRatings review${if (numRatings != 1) "s" else ""})", sum / numRatings))
                } else {
                    viewModel.editRating("Rating: N/A (0 reviews)")
                }
            }
    }
}