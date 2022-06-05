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

class FilterFragment : Fragment() {

    companion object {
        fun newInstance() = FilterFragment()
    }

    private lateinit var hydrationCheckbox: CheckBox
    private lateinit var drinkingCheckbox: CheckBox
    private lateinit var ratingCheckbox: CheckBox
    private lateinit var favoritesCheckbox: CheckBox
    private lateinit var ratingDropdown: Spinner
    private lateinit var applyButton: Button
    private lateinit var resetButton: Button
    private lateinit var closeButton: ImageButton
    private val ratings = arrayOf(0,1,2,3,4,5)
    private var userID = ""
    private lateinit var viewModel: FilterViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.filter_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = requireActivity().run {
            ViewModelProvider(this).get(FilterViewModel::class.java)
        }

        hydrationCheckbox = view.findViewById(R.id.hydration_station_check)
        drinkingCheckbox = view.findViewById(R.id.drinking_fountain_check)
        ratingCheckbox = view.findViewById(R.id.ratings_check)
        favoritesCheckbox = view.findViewById(R.id.favorites_check)
        ratingDropdown = view.findViewById(R.id.rating_dropdown)
        applyButton = view.findViewById(R.id.apply_button)
        resetButton = view.findViewById(R.id.reset_button)
        closeButton = view.findViewById(R.id.close_button)

        ArrayAdapter.createFromResource(
            this.requireContext(),
            R.array.ratings,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            ratingDropdown.adapter = adapter
        }

        ratingDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.rating = ratings[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        hydrationCheckbox.isChecked = viewModel.hydrationFilter
        drinkingCheckbox.isChecked = viewModel.drinkingFilter
        ratingCheckbox.isChecked = viewModel.ratingsFilter
        ratingDropdown.setSelection(viewModel.rating)
        favoritesCheckbox.isChecked = viewModel.favoritesFilter

        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        if (this.arguments != null) {
            userID = this.requireArguments().getString("userID")!!
        }

        if (userID == "") {
            favoritesCheckbox.visibility = View.INVISIBLE
        }

        applyButton.setOnClickListener {
            viewModel.hydrationFilter = hydrationCheckbox.isChecked
            viewModel.drinkingFilter = drinkingCheckbox.isChecked
            if (ratingCheckbox.isChecked) {
                viewModel.ratingsFilter = true
            } else {
                viewModel.ratingsFilter = false
                viewModel.rating = 0
            }
            if (userID != "") {
                viewModel.favoritesFilter = favoritesCheckbox.isChecked
                (requireActivity() as MapsActivity).updateFavorites()
            } else {
                viewModel.favoritesFilter = false
                viewModel.favorites = null
            }
            Toast.makeText(this.context, "Applied Filters", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }

        resetButton.setOnClickListener {
            hydrationCheckbox.isChecked = false
            drinkingCheckbox.isChecked = false
            ratingCheckbox.isChecked = false
            ratingDropdown.setSelection(0)
            if (userID != "")
                favoritesCheckbox.isChecked = false
        }
    }

}