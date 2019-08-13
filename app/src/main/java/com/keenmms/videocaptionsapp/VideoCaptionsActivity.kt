package com.keenmms.videocaptionsapp

import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.MediaPlayer.TrackInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import kotlinx.android.synthetic.main.activity_video_captions.*
import java.io.*
import java.util.*

class VideoCaptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_captions)

        val surfaceHolder = surfaceView.holder

        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val player = MediaPlayer.create(this@VideoCaptionsActivity, R.raw.video)
                player.setDisplay(surfaceHolder)
                try {
                    var mimeType: String
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                        mimeType = MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP
                    else mimeType = MediaFormat.MIMETYPE_TEXT_SUBRIP

                    player.addTimedTextSource(getCaptionsFile(R.raw.subtitle), mimeType)
                    val textTrackIndex = findTrackIndexFor(TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT, player.trackInfo)
                    if (textTrackIndex >= 0) {
                        player.selectTrack(textTrackIndex)
                    }
                    player.setOnTimedTextListener { mp, text ->
                        if (text != null) {
                            handler.post {
                                // To validate the caption with given time we display the time here.
                                // For actual case we should remove this
                                val seconds = mp.currentPosition / 1000
                                subtitleTxtView!!.text = ("[" + secondsToDuration(seconds) + "] " + text.text)
                            }
                        }
                    }
                    player.setOnVideoSizeChangedListener { mp, width, height ->
                        setFitToFillAspectRatio(mp, width, height)
                    }
                    player.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int ) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })

    }

    private fun findTrackIndexFor(mediaTrackType: Int, trackInfo: Array<TrackInfo>): Int {
        val index = -1
        for (i in trackInfo.indices) {
            if (trackInfo[i].trackType == mediaTrackType) {
                return i
            }
        }
        return index
    }

    private fun getCaptionsFile(resId: Int): String {
        val fileName = resources.getResourceEntryName(resId)
        val captionsFile = getFileStreamPath(fileName)
        if (captionsFile.exists()) {
            Log.v(TAG, "Captions file already exists")
            return captionsFile.absolutePath
        } else {
            Log.v(TAG, "Captions file does not exists, copy it to app folder from raw")
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = resources.openRawResource(resId)
                outputStream = FileOutputStream(captionsFile, false)
                copyFile(inputStream!!, outputStream)
                return captionsFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                closeStreams(inputStream, outputStream)
            }
            return ""
        }
    }

    @Throws(IOException::class)
    private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }


    private fun closeStreams(vararg closeables: Closeable?) {
        if (closeables != null) {
            for (stream in closeables) {
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    fun secondsToDuration(seconds: Int): String {
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60, Locale.US)
    }

    private fun setFitToFillAspectRatio(mp: MediaPlayer?, videoWidth: Int, videoHeight: Int) {
        if (mp != null) {
            val screenWidth = windowManager.defaultDisplay.width
            val screenHeight = windowManager.defaultDisplay.height
            val videoParams = rootLayout.layoutParams

            if (videoWidth > videoHeight) {
                videoParams.width = screenWidth
                videoParams.height = screenWidth * videoHeight / videoWidth
            } else {
                videoParams.width = screenHeight * videoWidth / videoHeight
                videoParams.height = screenHeight
            }

            rootLayout.layoutParams = videoParams
        }
    }

    companion object {
        private val TAG = "VideoCaption"
        private val handler = Handler()
    }
}
