package edu.ucsb.cs.cs184.j_miller.h2go

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

class AddSourceFragment : Fragment() {

    companion object {
        fun newInstance() = AddSourceFragment()
    }

    private lateinit var viewModel: AddSourceViewModel
    private lateinit var titleField: EditText
    private lateinit var latitude: TextView
    private lateinit var longitude: TextView
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

        // restore state after rotation
        latitude.text = viewModel.latitude.toString()
        longitude.text = viewModel.longitude.toString()
        titleField.setText(viewModel.titleText)

        // when confirm button clicked, if required field (title) is filled in,
        // write to Firebase otherwise use a toast to promp user to fill in required field
        confirmButton.setOnClickListener {
            if(titleField.text.isNotEmpty()) {
                // TODO
            } else {
                // TODO
            }
        }

        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            // TODO
        }
    }

}