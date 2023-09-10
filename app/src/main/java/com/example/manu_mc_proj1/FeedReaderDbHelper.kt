package com.example.manu_mc_proj1

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns


class FeedReaderDbHelper(context: HealthApp) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    object FeedReaderContract {
        // Table contents are grouped together in an anonymous object.
        object FeedEntry : BaseColumns {
            const val TABLE_NAME = "data"
            const val COLUMN_NAME_HEART_RATE = "heart_rate"
            const val COLUMN_NAME_BREATH_RATE = "breath_rate"
            const val COLUMN_NAME_NAUSEA = "nausea"
            const val COLUMN_NAME_HEADACHE = "headache"
            const val COLUMN_NAME_DIARRHEA = "diarrhea"
            const val COLUMN_NAME_SORE_THROAT = "sore_throat"
            const val COLUMN_NAME_FEVER = "fever"
            const val COLUMN_NAME_MUSCLE_ACHE = "muscle_ache"
            const val COLUMN_NAME_LOSS_OF_SMELL = "loss_of_smell_or_taste"
            const val COLUMN_NAME_COUGH = "cough"
            const val COLUMN_NAME_SHORTNESS_OF_BREATH = "shortness_of_breath"
            const val COLUMN_NAME_FEELING_TIRED = "feeling_tired"
        }
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FeedReader.db"
        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedReaderContract.FeedEntry.TABLE_NAME}"
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${FeedReaderContract.FeedEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_HEART_RATE} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_BREATH_RATE} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_NAUSEA} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_HEADACHE} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_DIARRHEA} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_SORE_THROAT} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_FEVER} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_MUSCLE_ACHE} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_LOSS_OF_SMELL} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_COUGH} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_SHORTNESS_OF_BREATH} TEXT," +
                    "${FeedReaderContract.FeedEntry.COLUMN_NAME_FEELING_TIRED} TEXT)"

    }
}