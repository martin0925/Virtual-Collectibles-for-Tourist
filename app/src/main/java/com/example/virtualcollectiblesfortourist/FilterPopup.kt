package com.example.virtualcollectiblesfortourist

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat

class FilterPopup(private val initialDistance: Int) : DialogFragment() {

    private lateinit var museumCheckBox: CheckBox
    private lateinit var natureCheckBox: CheckBox
    private lateinit var historicalCheckBox: CheckBox
    private lateinit var artCheckBox: CheckBox
    private lateinit var unusualCheckBox: CheckBox

    private lateinit var commonToggle: ToggleButton
    private lateinit var rareToggle: ToggleButton
    private lateinit var epicToggle: ToggleButton
    private lateinit var legendaryToggle: ToggleButton

    private lateinit var distanceSeekBar: SeekBar
    private lateinit var distanceTextView: TextView

    private lateinit var applyButton: Button

    private var activeFilters: Set<String> = emptySet()
    private var pendingFilters: Set<String>? = null

    private val ANY_DISTANCE = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.popup_filter, container, false)

        // Initialize views
        museumCheckBox = view.findViewById(R.id.checkbox_museums)
        natureCheckBox = view.findViewById(R.id.checkbox_nature)
        historicalCheckBox = view.findViewById(R.id.checkbox_historical)
        artCheckBox = view.findViewById(R.id.checkbox_art)
        unusualCheckBox = view.findViewById(R.id.checkbox_unusual)

        commonToggle = view.findViewById(R.id.toggle_common)
        rareToggle = view.findViewById(R.id.toggle_rare)
        epicToggle = view.findViewById(R.id.toggle_epic)
        legendaryToggle = view.findViewById(R.id.toggle_legendary)

        initializeToggleButton(commonToggle, "common", R.color.common)
        initializeToggleButton(rareToggle, "rare", R.color.rare)
        initializeToggleButton(epicToggle, "epic", R.color.epic)
        initializeToggleButton(legendaryToggle, "legendary", R.color.legendary)

        distanceSeekBar = view.findViewById(R.id.seekbar_distance)
        distanceTextView = view.findViewById(R.id.textview_distance)

        // Set initial distance
        distanceSeekBar.progress = if (initialDistance == ANY_DISTANCE) 0 else initialDistance
        distanceTextView.text = if (initialDistance == ANY_DISTANCE) "Any" else "$initialDistance km"

        // SeekBar listener to update distance display
        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                distanceTextView.text = if (progress == 0) "Any" else "$progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Apply button click listener
        applyButton = view.findViewById(R.id.apply_button)
        applyButton.setOnClickListener { applyFilters() }

        // Set filters if there were pending ones
        pendingFilters?.let {
            setActiveFilters(it)
            pendingFilters = null
        }

        return view
    }

    // Set the active filters
    fun setActiveFilters(filters: Set<String>) {
        if (!this::museumCheckBox.isInitialized) {
            pendingFilters = filters
            return
        }

        activeFilters = filters

        museumCheckBox.isChecked = activeFilters.contains("museums")
        natureCheckBox.isChecked = activeFilters.contains("nature")
        historicalCheckBox.isChecked = activeFilters.contains("historical")
        artCheckBox.isChecked = activeFilters.contains("art")
        unusualCheckBox.isChecked = activeFilters.contains("unusual")

        commonToggle.isChecked = activeFilters.contains("common")
        rareToggle.isChecked = activeFilters.contains("rare")
        epicToggle.isChecked = activeFilters.contains("epic")
        legendaryToggle.isChecked = activeFilters.contains("legendary")
    }

    // Initialize toggle button color change
    private fun initializeToggleButton(toggleButton: ToggleButton, filterKey: String, activeColorResId: Int) {
        toggleButton.setOnCheckedChangeListener { _, _ ->
            updateToggleButtonColor(toggleButton, activeColorResId)
        }
    }

    // Update toggle button background color
    private fun updateToggleButtonColor(toggleButton: ToggleButton, activeColorResId: Int) {
        val color = if (toggleButton.isChecked) {
            ContextCompat.getColor(requireContext(), activeColorResId)
        } else {
            ContextCompat.getColor(requireContext(), R.color.inactive_color)
        }
        toggleButton.setBackgroundColor(color)
    }

    // Apply selected filters and notify listener
    private fun applyFilters() {
        val selectedFilters = mutableListOf<String>()

        if (commonToggle.isChecked) selectedFilters.add("common")
        if (rareToggle.isChecked) selectedFilters.add("rare")
        if (epicToggle.isChecked) selectedFilters.add("epic")
        if (legendaryToggle.isChecked) selectedFilters.add("legendary")

        if (museumCheckBox.isChecked) selectedFilters.add("museums")
        if (natureCheckBox.isChecked) selectedFilters.add("nature")
        if (historicalCheckBox.isChecked) selectedFilters.add("historical")
        if (artCheckBox.isChecked) selectedFilters.add("art")
        if (unusualCheckBox.isChecked) selectedFilters.add("unusual")

        val selectedDistance = if (distanceSeekBar.progress == 0) ANY_DISTANCE else distanceSeekBar.progress

        (activity as? FilterDialogListener)?.onFiltersSelected(selectedFilters, selectedDistance)
        dismiss()  // Close the popup
    }

    interface FilterDialogListener {
        fun onFiltersSelected(filters: List<String>, distance: Int)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)  // Transparent background
    }
}