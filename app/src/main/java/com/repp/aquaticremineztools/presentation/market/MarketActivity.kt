package com.repp.aquaticremineztools.presentation.market

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.presentation.home.HomeActivity

class MarketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        ActivityTransition.startActivitySmooth(this, intent)
        finish()
    }
}
