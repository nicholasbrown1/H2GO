package edu.ucsb.cs.cs184.j_miller.h2go

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddSourceFragment : Fragment() {

    companion object {
        fun newInstance() = AddSourceFragment()
    }

    private lateinit var viewModel: AddSourceViewModel
    private lateinit var titleField: TextView
    private lateinit var latitude: TextView
    private lateinit var longitude: TextView
    private lateinit var locationField: EditText
    private lateinit var buildingField: EditText
    private lateinit var confirmButton: Button
    private lateinit var closeButton: ImageButton
    private lateinit var hydrationStationCheckbox: CheckBox
    private lateinit var drinkingFountainCheckbox: CheckBox
    private lateinit var floorDropdown: Spinner
    private var floor = ""
    private val floorNames = arrayOf(" ","B2","B1","1","2","3","4","5","6","7","8","9","10")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_source_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddSourceViewModel::class.java)
        titleField = view.findViewById<TextView>(R.id.title_field)
        latitude = view.findViewById<TextView>(R.id.latitude_value)
        longitude = view.findViewById<TextView>(R.id.longitude_value)
        confirmButton = view.findViewById<Button>(R.id.confirm_button)
        closeButton = view.findViewById<ImageButton>(R.id.close_button)
        buildingField = view.findViewById<EditText>(R.id.building_value)
        locationField = view.findViewById<EditText>(R.id.location_value)
        floorDropdown = view.findViewById<Spinner>(R.id.floor_dropdown)
        hydrationStationCheckbox = view.findViewById<CheckBox>(R.id.hydration_station)
        drinkingFountainCheckbox = view.findViewById<CheckBox>(R.id.drinking_fountain)

        ArrayAdapter.createFromResource(
            this.requireContext(),
            R.array.floors,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorDropdown.adapter = adapter
        }

        floorDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                floor = floorNames[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        if (this.arguments != null) {
            viewModel.latitude = this.requireArguments().getDouble("latitude")
            viewModel.longitude = this.requireArguments().getDouble("longitude")
        }

        // restore state after rotation
        latitude.text = viewModel.latitude.toString()
        longitude.text = viewModel.longitude.toString()
        //save is enabled by default on editText

        // set first part of title field when something is entered in building field
        val buildingTextWatcher = object : TextWatcher {
            override fun onTextChanged(str: CharSequence?, start: Int, before: Int, count: Int) {
                if(str != null) {
                    val titleVal = "$str - ${locationField.text}"
                    titleField.text = titleVal
                }
            }

            override fun beforeTextChanged(str: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(str: Editable?) {}
        }
        buildingField.addTextChangedListener(buildingTextWatcher)

        val locationTextWatcher = object : TextWatcher {
            override fun onTextChanged(str: CharSequence?, start: Int, before: Int, count: Int) {
                if(str != null) {
                    val titleVal = "${buildingField.text} - $str"
                    titleField.text = titleVal
                }
            }

            override fun beforeTextChanged(str: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(str: Editable?) {}
        }
        locationField.addTextChangedListener(locationTextWatcher)

        // when confirm button clicked, if required fields are filled in,
        // write to Firebase otherwise use a toast to promp user to fill in required fields
        confirmButton.setOnClickListener {
            if(allFieldsFilled()) {
                val entry = hashMapOf(
                    "title" to titleField.text.toString(),
                    "lat" to latitude.text.toString().toDouble(),
                    "long" to longitude.text.toString().toDouble(),
                    "floor" to floor,
                    "hydration_station" to hydrationStationCheckbox.isChecked(),
                    "drinking_fountain" to drinkingFountainCheckbox.isChecked(),
                    "rating" to 0.0,
                    "num_ratings" to 0,
                    "approved" to false
                )
                val db = Firebase.firestore
                db.collection("user_filling_locations").document().set(entry)
                    .addOnFailureListener { exception ->
                    Log.i("firebase_write", "set failed with ", exception)
                }
                Toast.makeText(this.context,
                    "Submitted source to the database! If approved it will appear on the map!",
                    Toast.LENGTH_LONG).show()
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                Toast.makeText(this.context,"Please fill all fields",Toast.LENGTH_SHORT).show()
            }
        }

        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // Returns true only if all required fields are filled
    fun allFieldsFilled(): Boolean {
        if (buildingField.text.isEmpty())
            return false
        if (locationField.text.isEmpty())
            return false
        if (floor == " ")
            return false
        if (!hydrationStationCheckbox.isChecked()
            && !drinkingFountainCheckbox.isChecked())
                return false
        return true
    }

}