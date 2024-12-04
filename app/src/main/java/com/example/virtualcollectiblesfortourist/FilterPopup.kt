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

    private lateinit var applyButton: Button

    private var activeFilters: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.popup_filter, container, false)

        museumCheckBox = view.findViewById(R.id.checkbox_museum)
        parkCheckBox = view.findViewById(R.id.checkbox_park)
        houseCheckBox = view.findViewById(R.id.checkbox_house)
        otherCheckBox = view.findViewById(R.id.checkbox_other)

        commonToggle = view.findViewById(R.id.toggle_common)
        rareToggle = view.findViewById(R.id.toggle_rare)
        epicToggle = view.findViewById(R.id.toggle_epic)
        legendaryToggle = view.findViewById(R.id.toggle_legendary)

        initializeToggleButton(commonToggle, "common", R.color.common)
        initializeToggleButton(rareToggle, "rare", R.color.rare)
        initializeToggleButton(epicToggle, "epic", R.color.epic)
        initializeToggleButton(legendaryToggle, "legendary", R.color.legendary)

        applyButton = view.findViewById(R.id.apply_button)
        applyButton.setOnClickListener {
            applyFilters()
        }

        return view
    }

    fun setActiveFilters(filters: Set<String>) {
        activeFilters = filters
    }

    private fun initializeToggleButton(toggleButton: ToggleButton, filterKey: String, activeColorResId: Int) {
        // Set toggle button state based on active filters
        toggleButton.isChecked = activeFilters.contains(filterKey)
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

        if (commonToggle.isChecked) selectedFilters.add("common")
        if (rareToggle.isChecked) selectedFilters.add("rare")
        if (epicToggle.isChecked) selectedFilters.add("epic")
        if (legendaryToggle.isChecked) selectedFilters.add("legendary")

        if (museumCheckBox.isChecked) selectedFilters.add("Museum")
        if (parkCheckBox.isChecked) selectedFilters.add("Park")
        if (houseCheckBox.isChecked) selectedFilters.add("House")
        if (otherCheckBox.isChecked) selectedFilters.add("Other")

        (activity as? FilterDialogListener)?.onFiltersSelected(selectedFilters)
        dismiss()
    }

    interface FilterDialogListener {
        fun onFiltersSelected(filters: List<String>)
    }
}