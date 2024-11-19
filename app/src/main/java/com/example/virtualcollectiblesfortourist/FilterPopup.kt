package com.example.virtualcollectiblesfortourist

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ToggleButton
import androidx.core.content.ContextCompat

class FilterPopup : DialogFragment() {

    private lateinit var museumCheckBox: CheckBox
    private lateinit var parkCheckBox: CheckBox
    private lateinit var houseCheckBox: CheckBox
    private lateinit var otherCheckBox: CheckBox

    private lateinit var commonToggle: ToggleButton
    private lateinit var rareToggle: ToggleButton
    private lateinit var epicToggle: ToggleButton
    private lateinit var legendaryToggle: ToggleButton

    private lateinit var timeLimitedCheckBox: CheckBox
    private lateinit var applyButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.popup_filter, container, false)

        // TODO: all possible categories after final dataset
        museumCheckBox = view.findViewById(R.id.checkbox_museum)
        parkCheckBox = view.findViewById(R.id.checkbox_park)
        houseCheckBox = view.findViewById(R.id.checkbox_house)
        otherCheckBox = view.findViewById(R.id.checkbox_other)

        commonToggle = view.findViewById(R.id.toggle_common)
        rareToggle = view.findViewById(R.id.toggle_rare)
        epicToggle = view.findViewById(R.id.toggle_epic)
        legendaryToggle = view.findViewById(R.id.toggle_legendary)

        timeLimitedCheckBox = view.findViewById(R.id.checkbox_time_limited)

        initializeToggleButton(commonToggle, R.color.common)
        initializeToggleButton(rareToggle, R.color.rare)
        initializeToggleButton(epicToggle, R.color.epic)
        initializeToggleButton(legendaryToggle, R.color.legendary)

        applyButton = view.findViewById(R.id.apply_button)
        applyButton.setOnClickListener {
            applyFilters()
        }

        return view
    }

    private fun initializeToggleButton(toggleButton: ToggleButton, activeColorResId: Int) {
        toggleButton.isChecked = true
        updateToggleButtonColor(toggleButton, activeColorResId)

        toggleButton.setOnCheckedChangeListener { _, _ ->
            updateToggleButtonColor(toggleButton, activeColorResId)
        }
    }

    private fun updateToggleButtonColor(toggleButton: ToggleButton, activeColorResId: Int) {
        val color = if (toggleButton.isChecked) {
            ContextCompat.getColor(requireContext(), activeColorResId)
        } else {
            ContextCompat.getColor(requireContext(), R.color.inactive_color)
        }
        toggleButton.setBackgroundColor(color)
    }

    private fun applyFilters() {
        val selectedFilters = mutableListOf<String>()

        if (museumCheckBox.isChecked) selectedFilters.add("Museum")
        if (parkCheckBox.isChecked) selectedFilters.add("Park")
        if (houseCheckBox.isChecked) selectedFilters.add("House")
        if (otherCheckBox.isChecked) selectedFilters.add("Other")

        if (commonToggle.isChecked) selectedFilters.add("Common")
        if (rareToggle.isChecked) selectedFilters.add("Rare")
        if (epicToggle.isChecked) selectedFilters.add("Epic")
        if (legendaryToggle.isChecked) selectedFilters.add("Legendary")

        if (timeLimitedCheckBox.isChecked) selectedFilters.add("Time-Limited")
        (activity as? FilterDialogListener)?.onFiltersSelected(selectedFilters)

        dismiss()
    }

    interface FilterDialogListener {
        fun onFiltersSelected(filters: List<String>)
    }
}