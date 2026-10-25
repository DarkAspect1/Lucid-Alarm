package com.lucid.alarm.ui

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Full-bleed looping video wallpaper backed by a Media3 [ExoPlayer].
 *
 * Always muted by default, repeats forever, and scales to fill (zoom) the host. The player is
 * created per [videoUri] and released when the composable leaves composition or the URI changes.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoBackgroundPlayer(
    videoUri: String,
    modifier: Modifier = Modifier,
    muted: Boolean = true,
    loop: Boolean = true
) {
    val context = LocalContext.current
    val player = remember(videoUri) {
        if (videoUri.isBlank()) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                volume = if (muted) 0f else 1f
                playWhenReady = true
                prepare()
            }
        }
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    if (player != null) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                }
            },
            update = { view -> view.player = player },
            modifier = modifier
        )
    }
}
