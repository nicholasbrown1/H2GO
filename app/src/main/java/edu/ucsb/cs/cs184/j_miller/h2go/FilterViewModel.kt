package edu.ucsb.cs.cs184.j_miller.h2go

import androidx.lifecycle.ViewModel

class FilterViewModel : ViewModel() {
    var hydrationFilter = false
    var drinkingFilter = false
    var ratingsFilter = false
    var rating = 0
    var favoritesFilter = false
}