package com.jay.downloadmanagerwithkotlin

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.jay.downloadmanagerwithkotlin.databinding.ActivityMainBinding
import com.master.permissionhelper.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private val url : String = "https://i.imgur.com/MnmF4Fb.mp4"
    private lateinit var downloaderViewModel : DownloaderViewModel

    private lateinit var permissionHelper : PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handlePerms()

        // instantiating viewModel instance
        downloaderViewModel = ViewModelProvider(this).get(DownloaderViewModel::class.java)

        binding.downloadLinkText.text = url
        binding.download.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || permissionHelper.hasPermission()) {
                downloaderViewModel.downloadMedia(this, url, 0) // start the download
            } else {
                Toast.makeText(this, "Storage Permission Required", Toast.LENGTH_SHORT).show()
                permissionHelper.openAppDetailsActivity()
            }
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

    /**
     * This function checks the API level and request permissions as necessary
     * */
    private fun handlePerms() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionHelper = PermissionHelper(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)

            permissionHelper.denied {
                if (it) {
                    Toast.makeText(this, "Permission denied by system", Toast.LENGTH_LONG).show()
                    permissionHelper.openAppDetailsActivity()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }
            //Request all permission
            permissionHelper.requestAll {}
        }
    }
}