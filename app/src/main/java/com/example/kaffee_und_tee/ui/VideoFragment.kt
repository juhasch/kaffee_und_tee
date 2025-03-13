package com.example.kaffee_und_tee.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.kaffee_und_tee.databinding.FragmentVideoBinding

@UnstableApi
class VideoFragment : Fragment() {
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!
    private val args: VideoFragmentArgs by navArgs()
    private var player: ExoPlayer? = null
    private val TAG = "VideoFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVideo()
        setupClickListeners()
    }

    private fun setupVideo() {
        context?.let { context ->
            Log.d(TAG, "Setting up video player with URL: ${args.videoUrl}")

            // Configure track selector with forced video
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setMaxVideoSizeSd()
                        .setPreferredVideoMimeType(C.MIMETYPE_VIDEO_H264)
                        .setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
                        .setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                )
            }

            // Configure data source factory
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)

            // Create the player with track selector
            player = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()
                .also { exoPlayer ->
                    // Configure the player view with simpler settings
                    binding.playerView.apply {
                        player = exoPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        useController = true
                        keepScreenOn = true
                    }

                    // Create media item
                    val mediaItem = MediaItem.fromUri(args.videoUrl)
                    
                    // Set media item and prepare
                    exoPlayer.setMediaItem(mediaItem)
                    
                    // Add a listener for debugging
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            Log.d(TAG, "Player state changed to: $state")
                            when (state) {
                                Player.STATE_BUFFERING -> {
                                    Log.d(TAG, "Buffering video...")
                                    binding.playerView.visibility = View.VISIBLE
                                }
                                Player.STATE_READY -> {
                                    Log.d(TAG, "Video ready to play")
                                    val videoFormat = exoPlayer.videoFormat
                                    Log.d(TAG, "Video format: $videoFormat")
                                    Log.d(TAG, "Surface: ${binding.playerView.videoSurfaceView}")
                                    binding.playerView.visibility = View.VISIBLE
                                }
                                Player.STATE_ENDED -> Log.d(TAG, "Video playback ended")
                                Player.STATE_IDLE -> Log.d(TAG, "Player in idle state")
                            }
                        }

                        override fun onRenderedFirstFrame() {
                            Log.d(TAG, "First frame rendered!")
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e(TAG, "Player error: ${error.message}", error)
                            error.printStackTrace()
                        }

                        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                            Log.d(TAG, "Available tracks: $tracks")
                            for (group in tracks.groups) {
                                if (group.type == C.TRACK_TYPE_VIDEO) {
                                    Log.d(TAG, "Video track found: ${group.length} formats")
                                    for (i in 0 until group.length) {
                                        val format = group.getTrackFormat(i)
                                        Log.d(TAG, "Format[$i]: width=${format.width}, height=${format.height}, codecs=${format.codecs}")
                                    }
                                }
                            }
                        }
                    })
                    
                    // Prepare and play
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
        }
    }

    private fun setupClickListeners() {
        binding.bottomNavigation.btnHome.setOnClickListener {
            findNavController().navigate(
                VideoFragmentDirections.actionVideoToRecipeList()
            )
        }

        binding.bottomNavigation.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
} 