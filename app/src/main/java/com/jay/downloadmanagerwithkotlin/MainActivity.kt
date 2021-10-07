package com.jay.downloadmanagerwithkotlin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.jay.downloadmanagerwithkotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private val url : String = "https://i.imgur.com/MnmF4Fb.mp4"
    private lateinit var downloaderViewModel : DownloaderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // instantiating viewModel instance
        downloaderViewModel = ViewModelProvider(this).get(DownloaderViewModel::class.java)

        binding.downloadLinkText.text = url
        binding.download.setOnClickListener {
            downloaderViewModel.downloadMedia(this, url, 0) // start the download
        }

        binding.cancel.setOnClickListener {
            downloaderViewModel.downloadMedia(this, url, 1) // cancel the download
        }

        downloaderViewModel.status.observe(this, {
            if(!it.isNullOrEmpty()) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        })
    }
}