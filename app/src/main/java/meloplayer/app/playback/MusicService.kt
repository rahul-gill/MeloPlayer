package meloplayer.app.playback

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.exoplayer.ExoPlayer
import meloplayer.app.R

class MusicService : MediaBrowserServiceCompat(){
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var exoPlayerWrapper: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this,MusicService::class.java.simpleName)
        this@MusicService.sessionToken = mediaSession.sessionToken

    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        //TODO: implement later
        return BrowserRoot(getString(R.string.app_name), null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //TODO: implement later
    }
}