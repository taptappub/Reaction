package taptap.parcel_test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.taptap.parcel_test.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val parcel = Reaction.of { "" }
    }
}