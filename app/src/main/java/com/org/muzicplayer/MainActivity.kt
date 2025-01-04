package com.org.muzicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var currentSongTitle: TextView
    private lateinit var currentSongArtist: TextView
    
    private val STORAGE_PERMISSION_CODE = 101
    private val TAG = "MainActivity"
    private var musicService: MusicService? = null
    private var songs = mutableListOf<Song>()
    private val handler = Handler(Looper.getMainLooper())
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService?.updateSongs(songs)
            musicService?.setOnSongChangeListener(object : MusicService.OnSongChangeListener {
                override fun onSongChanged(song: Song) {
                    runOnUiThread {
                        updatePlaybackUI(song)
                    }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    private val updateSeekBar = object : Runnable {
        override fun run() {
            musicService?.let { service ->
                if (service.isPlaying()) {
                    seekBar.progress = service.getCurrentPosition()
                    updateTimeText(currentTimeText, service.getCurrentPosition().toLong())
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        checkPermissionAndLoadSongs()
        startMusicService()
    }

    private fun startMusicService() {
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        playPauseButton = findViewById(R.id.playPauseButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTime)
        totalTimeText = findViewById(R.id.totalTime)
        currentSongTitle = findViewById(R.id.currentSongTitle)
        currentSongArtist = findViewById(R.id.currentSongArtist)

        currentTimeText.text = "0:00"
        totalTimeText.text = "0:00"
    }

    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            musicService?.let { service ->
                if (service.isPlaying()) {
                    service.pauseMusic()
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    service.resumeMusic()
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                    handler.post(updateSeekBar)
                }
            }
        }

        nextButton.setOnClickListener {
            musicService?.playNextSong()
        }

        prevButton.setOnClickListener {
            musicService?.playPreviousSong()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                    updateTimeText(currentTimeText, progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateTimeText(textView: TextView, milliseconds: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutes)
        textView.text = String.format("%d:%02d", minutes, seconds)
    }

    private fun checkPermissionAndLoadSongs() {
        if (checkPermission()) {
            loadSongs()
        } else {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted, loading songs")
                loadSongs()
            } else {
                Log.d(TAG, "Permission denied")
                Toast.makeText(this, "Storage permission required to access songs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSongs() {
        Log.d(TAG, "Starting to load songs")
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                Log.d(TAG, "Found ${cursor.count} songs")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val path = cursor.getString(pathColumn)
                    val duration = cursor.getLong(durationColumn)

                    val song = Song(
                        id = id,
                        title = title,
                        artist = artist,
                        path = path,
                        duration = duration
                    )
                    songs.add(song)
                }
            }

            if (songs.isEmpty()) {
                Log.d(TAG, "No songs found in the device")
                Toast.makeText(this, "No songs found in the device", Toast.LENGTH_SHORT).show()
            } else {
                songAdapter = SongAdapter(songs) { song ->
                    val index = songs.indexOf(song)
                    musicService?.playSong(song, index)
                }
                recyclerView.adapter = songAdapter
                musicService?.updateSongs(songs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading songs: ${e.message}")
            Toast.makeText(this, "Error loading songs", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlaybackUI(song: Song) {
        currentSongTitle.text = song.title
        currentSongArtist.text = song.artist
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        
        seekBar.max = song.duration.toInt()
        updateTimeText(totalTimeText, song.duration)
        handler.post(updateSeekBar)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        unbindService(serviceConnection)
    }
}