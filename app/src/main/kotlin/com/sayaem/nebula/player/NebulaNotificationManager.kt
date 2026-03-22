package com.sayaem.nebula.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sayaem.nebula.MainActivity
import com.sayaem.nebula.R
import com.sayaem.nebula.data.models.Song

class DeckNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_PLAYBACK    = "deck_playback"
        const val CHANNEL_ENGAGEMENT  = "deck_engagement"
        const val CHANNEL_PROMO       = "deck_promo"
        const val NOTIF_PLAYBACK_ID   = 1
        const val NOTIF_RETURN_ID     = 2
        const val NOTIF_WEEKLY_ID     = 3
        const val NOTIF_DISCOVERY_ID  = 4
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init { createChannels() }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Playback controls channel — high priority, always visible
            NotificationChannel(
                CHANNEL_PLAYBACK, "Now Playing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description      = "Music playback controls"
                setShowBadge(false)
                nm.createNotificationChannel(this)
            }

            // Re-engagement — medium priority
            NotificationChannel(
                CHANNEL_ENGAGEMENT, "Your Music",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly stats, mood reminders, listening streaks"
                nm.createNotificationChannel(this)
            }

            // Promo — low priority
            NotificationChannel(
                CHANNEL_PROMO, "Deck Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "New features, tips, and offers"
                nm.createNotificationChannel(this)
            }
        }
    }

    private fun launchIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // ── Now Playing notification ─────────────────────────────────────
    fun showNowPlaying(song: Song, isPlaying: Boolean) {
        val n = NotificationCompat.Builder(context, CHANNEL_PLAYBACK)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText("Deck")
            .setContentIntent(launchIntent())
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            // Prev
            .addAction(android.R.drawable.ic_media_previous, "Previous",
                actionIntent(ACTION_PREV))
            // Play/Pause
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                actionIntent(ACTION_PLAY_PAUSE)
            )
            // Next
            .addAction(android.R.drawable.ic_media_next, "Next",
                actionIntent(ACTION_NEXT))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_PLAYBACK_ID, n)
    }

    fun cancelPlayback() = nm.cancel(NOTIF_PLAYBACK_ID)

    // ── Re-engagement: "Come back" after 1 day away ──────────────────
    fun showComeBackNotification(topSong: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_ENGAGEMENT)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Your music misses you 🎵")
            .setContentText("$topSong and more are waiting for you")
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_RETURN_ID, n)
    }

    // ── Weekly stats notification ─────────────────────────────────────
    fun showWeeklyStats(minutesListened: Int, topArtist: String) {
        val hours = minutesListened / 60
        val mins  = minutesListened % 60
        val n = NotificationCompat.Builder(context, CHANNEL_ENGAGEMENT)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Your week in music 🎧")
            .setContentText("You listened for ${hours}h ${mins}m • Top: $topArtist")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You listened for ${hours}h ${mins}m this week.\nTop artist: $topArtist\nTap to see your full Deck Wrapped!"))
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_WEEKLY_ID, n)
    }

    // ── Mood-based suggestion ─────────────────────────────────────────
    fun showMoodSuggestion(mood: String, playlistName: String) {
        val emoji = when (mood) {
            "energetic" -> "⚡"
            "chill"     -> "🌙"
            "focus"     -> "🧠"
            "happy"     -> "😊"
            else        -> "🎵"
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ENGAGEMENT)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$emoji Perfect for right now")
            .setContentText("Try \"$playlistName\" for your $mood mood")
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_DISCOVERY_ID, n)
    }

    // ── Listening streak ──────────────────────────────────────────────
    fun showStreakNotification(days: Int) {
        val n = NotificationCompat.Builder(context, CHANNEL_ENGAGEMENT)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle("🔥 $days day streak!")
            .setContentText("Keep it going — open Deck and keep your streak alive")
            .setContentIntent(launchIntent())
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_DISCOVERY_ID + 1, n)
    }

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(action).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val ACTION_PLAY_PAUSE = "com.sayaem.nebula.PLAY_PAUSE"
        const val ACTION_NEXT       = "com.sayaem.nebula.NEXT"
        const val ACTION_PREV       = "com.sayaem.nebula.PREV"
    }
}
