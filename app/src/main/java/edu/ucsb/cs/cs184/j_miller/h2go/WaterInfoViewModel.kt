package edu.ucsb.cs.cs184.j_miller.h2go

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WaterInfoViewModel: ViewModel() {
    var latitude = 0.0
    var longitude = 0.0
    private val _titleText = MutableLiveData<String>().apply {
        value ="Loading..."
    }
    private val _floorText = MutableLiveData<String>().apply {
        value ="Loading..."
    }
    private val _typeText = MutableLiveData<String>().apply {
        value ="Loading..."
    }

    fun editTitle(newTitle: String) {
        _titleText.apply {
            value = newTitle
        }
    }
    fun editFloor(newFloor: String) {
        _floorText.apply {
            value = newFloor
        }
    }
    fun editType(newType: String) {
        _typeText.apply {
            value = newType
        }
    }
    val titleText: LiveData<String> = _titleText
    val floorText: LiveData<String> = _floorText
    val typeText: LiveData<String> = _typeText
}
