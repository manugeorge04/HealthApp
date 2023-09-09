package com.example.manu_mc_proj1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.manu_mc_proj1.databinding.ActivityMain2Binding


class MainActivity2 : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMain2Binding

    private val symptomRatings = mutableMapOf<String, Float>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Define the list of symptoms
        val symptoms = arrayOf(
            "Nausea",
            "Headache",
            "Diarrhea",
            "Sore Throat",
            "Fever",
            "Muscle Ache",
            "Loss of Smell or Taste",
            "Cough",
            "Shortness of Breath",
            "Feeling Tired"
        )

        // Create an ArrayAdapter and set it to the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, symptoms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        viewBinding.symptomsSpinner.adapter = adapter

        // Initialize RatingBar
        val ratingBar = viewBinding.ratingBar

        // Set the default rating to 0
        ratingBar.rating = 0f

        // Handle Spinner item selection
        viewBinding.symptomsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSymptom = symptoms[position]
                // Update the RatingBar's associated symptom
                ratingBar.tag = selectedSymptom
                // Set the RatingBar's rating based on the stored rating for the symptom
                ratingBar.rating = symptomRatings[selectedSymptom] ?: 0f
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle when nothing is selected
            }
        }

        // Handle RatingBar rating changes
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val selectedSymptom = ratingBar.tag as? String
            if (selectedSymptom != null) {
                // Store the rating for the selected symptom
                symptomRatings[selectedSymptom] = rating
            }
        }
    }

}