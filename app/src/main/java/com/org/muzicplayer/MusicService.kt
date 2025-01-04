package com.org.muzicplayer

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.widget.RemoteViews

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null
    private lateinit var mediaSession: MediaSessionCompat
    private val binder = MusicBinder()
    private var songs = listOf<Song>()
    private var currentSongIndex = -1
    private var songChangeListener: OnSongChangeListener? = null

    interface OnSongChangeListener {
        fun onSongChanged(song: Song)
    }

    fun setOnSongChangeListener(listener: OnSongChangeListener) {
        songChangeListener = listener
    }

    companion object {
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.org.muzicplayer.PLAY"
        const val ACTION_PAUSE = "com.org.muzicplayer.PAUSE"
        const val ACTION_NEXT = "com.org.muzicplayer.NEXT"
        const val ACTION_PREVIOUS = "com.org.muzicplayer.PREVIOUS"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> resumeMusic()
                ACTION_PAUSE -> pauseMusic()
                ACTION_NEXT -> playNextSong()
                ACTION_PREVIOUS -> playPreviousSong()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MusicService")
        createNotificationChannel()
        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
        })
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music player controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
    }

    fun playSong(song: Song, index: Int) {
        currentSong = song
        currentSongIndex = index
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
            setOnCompletionListener {
                playNextSong()
            }
        }
        songChangeListener?.onSongChanged(song)
        updateNotification()
    }

    private fun updateNotification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val song = currentSong ?: return createEmptyNotification()
        
        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(if (mediaPlayer?.isPlaying == true) ACTION_PAUSE else ACTION_PLAY),
            FLAG_IMMUTABLE
        )
        
        val nextIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_NEXT),
            FLAG_IMMUTABLE
        )
        
        val previousIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(ACTION_PREVIOUS),
            FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            FLAG_IMMUTABLE
        )

        val customNotification = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.notificationSongTitle, song.title)
            setTextViewText(R.id.notificationArtist, song.artist)
            setImageViewResource(
                R.id.notificationPlayPause,
                if (mediaPlayer?.isPlaying == true) 
                    android.R.drawable.ic_media_pause 
                else 
                    android.R.drawable.ic_media_play
            )
            
            setOnClickPendingIntent(R.id.notificationPlayPause, playPauseIntent)
            setOnClickPendingIntent(R.id.notificationNext, nextIntent)
            setOnClickPendingIntent(R.id.notificationPrevious, previousIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setCustomContentView(customNotification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Music Player")
            .setContentText("No song playing")
            .build()
    }

    fun resumeMusic() {
        mediaPlayer?.start()
        updateNotification()
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
        updateNotification()
    }

    fun playNextSong() {
        if (songs.isEmpty()) return
        currentSongIndex = (currentSongIndex + 1) % songs.size
        playSong(songs[currentSongIndex], currentSongIndex)
    }

    fun playPreviousSong() {
        if (songs.isEmpty()) return
        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else songs.size - 1
        playSong(songs[currentSongIndex], currentSongIndex)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        unregisterReceiver(broadcastReceiver)
    }
}
