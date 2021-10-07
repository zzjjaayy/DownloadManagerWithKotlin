package com.jay.downloadmanagerwithkotlin

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloaderViewModel : ViewModel(){

    // This will be updated on the basis of the
    private val _status : MutableLiveData<String> = MutableLiveData()
    val status : LiveData<String> = _status

    // LiveData for progress
    private val _progress : MutableLiveData<List<Int>> = MutableLiveData()
    val progress : LiveData<List<Int>> = _progress

    // This is just to keep track of statuses when the download is running
    private var lastMsg : String = ""

    private lateinit var directory : File
    private lateinit var url : String
    private var downloadId : Long = -1L

    /**
     * This function is the entry point for all downloads and decides
     * whether to start or cancel a download.
     * @param context -> used for the Toasts and getting the DownloadManagerService
     * @param url -> file URL
     * @param requestCode -> to either start or cancel the download {0 for start & 1 for cancel}
     **/
    fun downloadMedia(context: Context, url : String, requestCode : Int){
        // setting the url as top level property for later use ->
        this.url = url

        // Setting up the target Directory ->
        directory = File(Environment.DIRECTORY_DOWNLOADS)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val downloadUri = Uri.parse(url) // parse url to uri

        when(requestCode) {
            0 -> {
                // Creating a request with all necessary methods for the details
                val request = DownloadManager.Request(downloadUri).apply {
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setTitle(url.substring(url.lastIndexOf("/") + 1))
                        .setDescription("")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        // Notifications for DownloadManager are optional and can be removed
                        .setDestinationInExternalPublicDir(
                            directory.toString(),
                            url.substring(url.lastIndexOf("/") + 1)
                        )
                }
                startDownload(context, request)
            }
            1 -> {cancelDownload(context)}
        }
    }

    /**
     * Cancels the download after checking if the download ID is not still the
     * default value set above (-1)
     * @param context is used for Toasts right from the viewModel & getting the DownloadManager
     * **/
    private fun cancelDownload(context: Context) {
        // Calling the DownloadManagerService
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        if(downloadId != -1L) {
            downloadManager.remove(downloadId)
        } else {
            Toast.makeText(context, "Cancel Failed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Has everything to start the request formed in the downloadMedia() function
     * @param context to get the DownloadManager
     * @param request -> This is passed into the DownloadManager as this has all the file details
     */
    private fun startDownload(context: Context, request : DownloadManager.Request) {
        // Calling the DownloadManagerService
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // starting the download
        downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var downloading = true
                // The following is to set statuses for the present download
                while (downloading) {
                    val cursor: Cursor = downloadManager.query(query)
                    cursor.moveToFirst()
                    val total : Long = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    // Progress ->
                    if (total >= 0) {
                        val downloaded : Long = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        val currentProgress: Int = downloaded.toInt()
                        _progress.postValue(listOf(currentProgress, total.toInt()))
                    }

                    // Download finished
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                    }

                    // Changing statuses ->
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val msg = statusMessage(url, directory, status)
                    if (msg != lastMsg) {
                        _status.postValue(msg)
                        lastMsg = msg ?: ""
                    }
                    cursor.close()
                }
            } catch (e: CursorIndexOutOfBoundsException) {
                _status.postValue("Download cancelled")
                _progress.postValue(listOf(0, 0))
            }
        }
    }

    /**
     * Sets Status Messages from the DownloadManager
     **/
    private fun statusMessage(url: String, directory: File, status: Int): String? {
        var msg = ""
        msg = when (status) {
            DownloadManager.STATUS_FAILED -> "Download has been failed, please try again"
            DownloadManager.STATUS_PAUSED -> "Paused"
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Downloading..."
            DownloadManager.STATUS_SUCCESSFUL -> "Media downloaded successfully in $directory" + File.separator + url.substring(
                url.lastIndexOf("/") + 1
            )
            else -> "There's nothing to download"
        }
        return msg
    }

}