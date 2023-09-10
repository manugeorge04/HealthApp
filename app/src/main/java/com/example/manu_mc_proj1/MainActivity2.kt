package com.example.manu_mc_proj1

import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.manu_mc_proj1.databinding.ActivityMain2Binding


class MainActivity2 : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMain2Binding

    private val symptomRatings = mutableMapOf<String, Float>()
    private var dbHelper: FeedReaderDbHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        dbHelper = (applicationContext as HealthApp).dbHelper

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

        viewBinding.uploadSymptoms.setOnClickListener { uploadSymptoms() }

        // Handle RatingBar rating changes
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val selectedSymptom = ratingBar.tag as? String
            if (selectedSymptom != null) {
                // Store the rating for the selected symptom
                symptomRatings[selectedSymptom] = rating
            }
        }
    }

    fun uploadSymptoms() {
        // Step 1: Retrieve the last row
        val db = dbHelper?.writableDatabase
        val cursor = db?.rawQuery("SELECT * FROM data ORDER BY _id DESC LIMIT 1", null)

        if (cursor != null && cursor.moveToFirst()) {
            // Step 2: Update the desired columns
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
            val nausea = symptomRatings["Nausea"]
            val headache = symptomRatings["Headache"]
            val diarrhea = symptomRatings["Diarrhea"]
            val sore_throat = symptomRatings["Sore Throat"]
            val fever = symptomRatings["Fever"]
            val muscle_ache = symptomRatings["Muscle Ache"]
            val loss = symptomRatings["Loss of Smell or Taste"]
            val cough = symptomRatings["Cough"]
            val shortness = symptomRatings["Shortness of Breath"]
            val tired = symptomRatings["Feeling Tired"]

            // Step 3: Perform the update operation
            val values = ContentValues()
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_NAUSEA, nausea)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_HEADACHE, headache)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_DIARRHEA, diarrhea)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_SORE_THROAT, sore_throat)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_FEVER, fever)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_MUSCLE_ACHE, muscle_ache)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_LOSS_OF_SMELL, loss)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_COUGH, cough)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_SHORTNESS_OF_BREATH, shortness)
            values.put(FeedReaderDbHelper.FeedReaderContract.FeedEntry.COLUMN_NAME_FEELING_TIRED, tired)

            val whereClause = "_id = ?"
            val whereArgs = arrayOf(id.toString())

            val updatedRows = db.update(FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME, values, whereClause, whereArgs)

            cursor.close()
        } else {
        }

        if (db != null) {
            db.close()
        }

    }

}