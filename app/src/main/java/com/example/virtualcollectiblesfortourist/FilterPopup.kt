package com.example.virtualcollectiblesfortourist

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox

class FilterPopup : DialogFragment() {

    private lateinit var museumCheckBox: CheckBox
    private lateinit var parkCheckBox: CheckBox
    private lateinit var houseCheckBox: CheckBox
    private lateinit var otherCheckBox: CheckBox

    private lateinit var commonCheckBox: CheckBox
    private lateinit var rareCheckBox: CheckBox
    private lateinit var epicCheckBox: CheckBox
    private lateinit var legendaryCheckBox: CheckBox

    private lateinit var timeLimitedCheckBox: CheckBox
    private lateinit var applyButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.popup_filter, container, false)

        // Initialize all checkboxes
        museumCheckBox = view.findViewById(R.id.checkbox_museum)
        parkCheckBox = view.findViewById(R.id.checkbox_park)
        houseCheckBox = view.findViewById(R.id.checkbox_house)
        otherCheckBox = view.findViewById(R.id.checkbox_other)

        commonCheckBox = view.findViewById(R.id.checkbox_common)
        rareCheckBox = view.findViewById(R.id.checkbox_rare)
        epicCheckBox = view.findViewById(R.id.checkbox_epic)
        legendaryCheckBox = view.findViewById(R.id.checkbox_legendary)

        timeLimitedCheckBox = view.findViewById(R.id.checkbox_time_limited)

        applyButton = view.findViewById(R.id.apply_button)
        applyButton.setOnClickListener {
            applyFilters()
        }

        return view
    }

    private fun applyFilters() {
        val selectedFilters = mutableListOf<String>()

        if (museumCheckBox.isChecked) selectedFilters.add("Museum")
        if (parkCheckBox.isChecked) selectedFilters.add("Park")
        if (houseCheckBox.isChecked) selectedFilters.add("House")
        if (otherCheckBox.isChecked) selectedFilters.add("Other")

        if (commonCheckBox.isChecked) selectedFilters.add("Common")
        if (rareCheckBox.isChecked) selectedFilters.add("Rare")
        if (epicCheckBox.isChecked) selectedFilters.add("Epic")
        if (legendaryCheckBox.isChecked) selectedFilters.add("Legendary")

        if (timeLimitedCheckBox.isChecked) selectedFilters.add("Time-Limited")
        (activity as? FilterDialogListener)?.onFiltersSelected(selectedFilters)

        dismiss()
    }

    interface FilterDialogListener {
        fun onFiltersSelected(filters: List<String>)
    }
}