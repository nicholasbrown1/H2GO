package edu.ucsb.cs.cs184.j_miller.h2go

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CommentInfo(text: String, user: String, time: Long) {
    val text = text
    val user = user
    val time = time

    fun getCommentText(): String {
        return "${user}: ${text}\n"
    }
}
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
    private lateinit var commentField: EditText
    private lateinit var commentButton: Button
    private lateinit var closeButton: ImageButton
    private lateinit var favoriteCheckbox: CheckBox
    private lateinit var commentsLayout: LinearLayout
    private lateinit var adminButtons: ConstraintLayout
    private lateinit var approveButton: Button
    private lateinit var denyButton: Button
    private var db = Firebase.firestore

    private var user: FirebaseUser? = null
    private var srcID = ""
    private var collection = ""
    private var ratingValue = 0.0
    private var totalNumRatings = 0
    private var wasFavorite = false

    private var approved = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.water_info_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(WaterInfoViewModel::class.java)
        titleField = view.findViewById(R.id.title_text)
        latitude = view.findViewById(R.id.latitude_value)
        longitude = view.findViewById(R.id.longitude_value)
        typeField = view.findViewById(R.id.building_value)
        floorField = view.findViewById(R.id.location_value)
        ratingField = view.findViewById(R.id.rating)
        ratingButton = view.findViewById(R.id.rate_button)
        ratingEntry = view.findViewById(R.id.rate_entry)
        commentField = view.findViewById(R.id.comment_field)
        commentButton = view.findViewById(R.id.comment_button)
        closeButton = view.findViewById(R.id.close_button)
        favoriteCheckbox = view.findViewById(R.id.favorite_checkbox)
        commentsLayout = view.findViewById(R.id.comments_list)
        adminButtons = view.findViewById(R.id.admin_buttons)
        approveButton = view.findViewById(R.id.approve_button)
        denyButton = view.findViewById(R.id.deny_button)

        user = (requireActivity() as MapsActivity).auth.currentUser

        if (this.arguments != null) {
            viewModel.latitude = this.requireArguments().getDouble("latitude")
            viewModel.longitude = this.requireArguments().getDouble("longitude")
            srcID = this.requireArguments().getString("id")!!
            approved = this.requireArguments().getBoolean("approved")!!
            loadData()
        }

        latitude.text = viewModel.latitude.toString()
        longitude.text = viewModel.longitude.toString()
        commentField.setText(viewModel.commentText)

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
        if(user == null || !approved) {
            ratingEntry.isVisible = false
            ratingButton.isVisible = false
            favoriteCheckbox.isVisible = false
            commentButton.isVisible = false
            commentField.isVisible = false
        }

        if (!approved) {
            ratingField.isVisible = false
            commentsLayout.isVisible = false
            adminButtons.isVisible = true
        }

        val ratings = arrayOf("1","2","3","4","5")
        val ratingsAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ratings)
        ratingEntry.adapter = ratingsAdapter
        ratingButton.setOnClickListener {
            var hasRated = false
            var oldUserRating = 0.0
            val currentRating = ratingEntry.selectedItem.toString().toDouble()
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
        commentButton.setOnClickListener {
            addComment()
            commentField.setText("")
            updateComments()
        }

        approveButton.setOnClickListener {
            db.collection(collection).document(srcID)
                .set(hashMapOf("approved" to true), SetOptions.merge())
            Toast.makeText(this.context,
                "Source Approved!",
                Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }

        denyButton.setOnClickListener {
            db.collection(collection).document(srcID).delete()
                .addOnSuccessListener {
                    Log.i("firebase_write","Successfully deleted document "+srcID)
                    Toast.makeText(this.context,
                        "Removed Source from the Database.",
                        Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { exception ->
                    Log.i("firebase_write", "set failed with ", exception)
                }
        }

        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            // if the current state of the favorites checkbox is different from the original state
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
                        "admin" to false,
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
            .document(srcID)
            .get()
            .addOnSuccessListener { location ->
                if (location.data != null) {
                    collection = "filling_locations"
                    viewModel.editTitle(location.data!!["title"] as String)
                    viewModel.editFloor(location.data!!["floor"] as String)
                    viewModel.editType(
                        getType(
                            location.data!!["hydration_station"] as Boolean,
                            location.data!!["drinking_fountain"] as Boolean
                        )
                    )
                    if (user != null && (requireActivity() as MapsActivity).isFavorite(srcID)) {
                        favoriteCheckbox.isChecked = true
                        wasFavorite = true
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }
        db.collection("user_filling_locations")
            .document(srcID)
            .get()
            .addOnSuccessListener { location ->
                if (location.data != null) {
                    srcID = location.id
                    approved = location.data!!["approved"] as Boolean
                    collection = "user_filling_locations"
                    viewModel.editTitle(location.data!!["title"] as String)
                    viewModel.editFloor(location.data!!["floor"] as String)
                    viewModel.editType(
                        getType(
                            location.data!!["hydration_station"] as Boolean,
                            location.data!!["drinking_fountain"] as Boolean
                        )
                    )
                    if (user != null && (requireActivity() as MapsActivity).isFavorite(srcID)) {
                        favoriteCheckbox.isChecked = true
                        wasFavorite = true
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }

        updateRating()
        updateComments()
    }
    private fun updateRating() {
        db.collection("ratings")
            .get()
            .addOnSuccessListener { result ->
                var sum = 0.0
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
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }
    }

    private fun addComment() {
        if(commentField.text.toString() != "") {
            val entry = hashMapOf(
                "lat" to viewModel.latitude,
                "long" to viewModel.longitude,
                "text" to commentField.text.toString(),
                "user" to user?.email,
                "time" to System.currentTimeMillis()
            )
            db.collection("comments").document().set(entry)
                .addOnFailureListener { exception ->
                    Log.i("firebase_write", "set failed with ", exception)
                }

        }
    }

    private fun updateComments() {
        commentsLayout.removeAllViews()
        db.collection("comments")
            .get()
            .addOnSuccessListener { result ->
                val comments : MutableList<CommentInfo> = mutableListOf()
                for (comment in result) {
                    if(comment.data["lat"] == viewModel.latitude && comment.data["long"] == viewModel.longitude) {
                        comments.add(CommentInfo(comment.data["text"] as String, comment.data["user"] as String, comment.data["time"] as Long))
                    }
                }
                comments.sortBy { -it.time }
                for (comment in comments) {
                    val commentView = TextView(context)
                    commentView.text = comment.getCommentText()
                    commentView.textSize = 30f
                    commentsLayout.addView(commentView)
                }
            }
            .addOnFailureListener { exception ->
                Log.i("firebase_read", "get failed with ", exception)
            }
    }



    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.commentText = commentField.text.toString()
    }
}