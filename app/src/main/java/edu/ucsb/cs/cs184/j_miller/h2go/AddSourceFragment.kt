package edu.ucsb.cs.cs184.j_miller.h2go

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddSourceFragment : Fragment() {

    companion object {
        fun newInstance() = AddSourceFragment()
    }

    private lateinit var viewModel: AddSourceViewModel
    private lateinit var titleField: EditText
    private lateinit var latitude: TextView
    private lateinit var longitude: TextView
    private lateinit var typeField: EditText
    private lateinit var floorField: EditText
    private lateinit var confirmButton: Button
    private lateinit var closeButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_source_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddSourceViewModel::class.java)
        titleField = view.findViewById<EditText>(R.id.title_field)
        latitude = view.findViewById<TextView>(R.id.latitude_value)
        longitude = view.findViewById<TextView>(R.id.longitude_value)
        confirmButton = view.findViewById<Button>(R.id.confirm_button)
        closeButton = view.findViewById<ImageButton>(R.id.close_button)
        typeField = view.findViewById<EditText>(R.id.type_value)
        floorField = view.findViewById<EditText>(R.id.floor_value)

        if (this.arguments != null) {
            viewModel.latitude = this.requireArguments().getDouble("latitude")
            viewModel.longitude = this.requireArguments().getDouble("longitude")
        }

        // restore state after rotation
        latitude.text = viewModel.latitude.toString()
        longitude.text = viewModel.longitude.toString()
        titleField.setText(viewModel.titleText)

        // when confirm button clicked, if required fields are filled in,
        // write to Firebase otherwise use a toast to promp user to fill in required fields
        confirmButton.setOnClickListener {
            if(titleField.text.isNotEmpty() && typeField.text.isNotEmpty() && floorField.text.isNotEmpty()) {
                val entry = hashMapOf(
                    "title" to titleField.text.toString(),
                    "lat" to latitude.text.toString().toDouble(),
                    "long" to longitude.text.toString().toDouble(),
                    "type" to typeField.text.toString(),
                    "floor" to floorField.text.toString()
                )
                val db = Firebase.firestore
                db.collection("filling_locations").document().set(entry)
                    .addOnFailureListener { exception ->
                    Log.i("firebase_write", "set failed with ", exception)
                }
                Toast.makeText(this.context,"Added source to the database!",Toast.LENGTH_SHORT).show()
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

}