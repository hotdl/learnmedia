package com.hotdldl.session001

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    lateinit var frmContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        frmContainer = findViewById(R.id.fragment_container)
        findViewById<Button>(R.id.btn_show1).setOnClickListener {
            showImageByImageView()
        }
        findViewById<Button>(R.id.btn_show2).setOnClickListener {
            showImageBySurfaceView()
        }
        findViewById<Button>(R.id.btn_show3).setOnClickListener {
            showImageByCustomView()
        }
    }

    private fun showImageByImageView() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, ImageViewFragment.newInstance())
        transaction.commit()
    }

    private fun showImageBySurfaceView() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, SurfaceViewFragment.newInstance())
        transaction.commit()
    }

    private fun showImageByCustomView() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, CustomViewFragment.newInstance())
        transaction.commit()
    }
}