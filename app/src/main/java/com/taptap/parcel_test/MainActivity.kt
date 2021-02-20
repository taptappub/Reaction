package com.taptap.parcel_test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import taptap.parcel.Parcel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val parcel = Parcel.of { "" }
    }
}