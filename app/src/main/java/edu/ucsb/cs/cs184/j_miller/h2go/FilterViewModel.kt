package edu.ucsb.cs.cs184.j_miller.h2go

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FilterViewModel : ViewModel() {
    var hydrationFilter = false
    var drinkingFilter = false
    var ratingsFilter = false
    var rating = 0
    var favoritesFilter = false
    var favorites: List<String>? = null
}