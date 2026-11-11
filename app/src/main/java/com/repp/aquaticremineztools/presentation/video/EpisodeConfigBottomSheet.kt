package com.repp.aquaticremineztools.presentation.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.video.model.VideoEpisode
import com.repp.aquaticremineztools.data.video.model.VideoSource
import com.repp.aquaticremineztools.data.video.source.HentaiCopParser
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class EpisodeConfigBottomSheet : BottomSheetDialogFragment() {

    private var episode: VideoEpisode? = null
    private var seriesCoverUrl: String = ""
    private var initialServerId: String? = null
    private var initialQualityLabel: String? = null
    private var onSaveConfig: ((serverId: String, qualityLabel: String) -> Unit)? = null

    private lateinit var tvSheetTitle: TextView
    private lateinit var playerView: PlayerView
    private lateinit var ivPlayerPoster: android.widget.ImageView
    private lateinit var pbPlayerLoading: ProgressBar
    private lateinit var rgServers: RadioGroup
    private lateinit var rgResolutions: RadioGroup
    private lateinit var tvWarning: TextView
    private lateinit var btnSave: Button

    private var player: ExoPlayer? = null
    private val parser by lazy { HentaiCopParser(requireContext()) }

    private var selectedServerId: String = ""
    private var selectedQualityLabel: String = ""

    companion object {
        fun newInstance(
            episode: VideoEpisode,
            seriesCoverUrl: String = "",
            initialServerId: String?,
            initialQualityLabel: String?,
            onSaveConfig: (serverId: String, qualityLabel: String) -> Unit
        ): EpisodeConfigBottomSheet {
            return EpisodeConfigBottomSheet().apply {
                this.episode = episode
                this.seriesCoverUrl = seriesCoverUrl
                this.initialServerId = initialServerId
                this.initialQualityLabel = initialQualityLabel
                this.onSaveConfig = onSaveConfig
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_episode_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupUI()
        setupListeners()
    }

    private fun bindViews(view: View) {
        tvSheetTitle = view.findViewById(R.id.tvSheetEpisodeTitle)
        playerView = view.findViewById(R.id.playerView)
        ivPlayerPoster = view.findViewById(R.id.ivPlayerPoster)
        pbPlayerLoading = view.findViewById(R.id.pbPlayerLoading)
        rgServers = view.findViewById(R.id.rgServerSelection)
        rgResolutions = view.findViewById(R.id.rgResolutionSelection)
        tvWarning = view.findViewById(R.id.tvResolutionWarning)
        btnSave = view.findViewById(R.id.btnSaveConfig)
    }

    private fun setupUI() {
        val currentEp = episode ?: return
        tvSheetTitle.text = currentEp.title

        val posterToLoad = if (currentEp.thumbnailUrl.isNotBlank() && !HentaiCopParser.isFaviconOrLogoUrl(currentEp.thumbnailUrl)) {
            currentEp.thumbnailUrl
        } else if (seriesCoverUrl.isNotBlank() && !HentaiCopParser.isFaviconOrLogoUrl(seriesCoverUrl)) {
            seriesCoverUrl
        } else {
            ""
        }

        if (posterToLoad.isNotBlank()) {
            coil.Coil.imageLoader(requireContext()).enqueue(
                coil.request.ImageRequest.Builder(requireContext())
                    .data(posterToLoad)
                    .target(ivPlayerPoster)
                    .crossfade(true)
                    .addHeader("User-Agent", HentaiCopParser.USER_AGENT)
                    .addHeader("Referer", "https://hentaicop.com/")
                    .build()
            )
        }

        val sources = currentEp.availableSources
        if (sources.isEmpty()) {
            Toast.makeText(context, "Tidak ada server/sumber video yang tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        // Group sources by server name
        val servers = sources.map { it.serverName }.distinct()

        rgServers.removeAllViews()
        servers.forEachIndexed { index, serverName ->
            val rb = RadioButton(context).apply {
                id = View.generateViewId()
                text = serverName
                tag = serverName
                setTextColor(0xFF292722.toInt())
            }
            rgServers.addView(rb)

            val isSelectedServer = initialServerId?.contains(serverName, ignoreCase = true) == true || index == 0
            if (isSelectedServer) {
                rb.isChecked = true
                selectedServerId = sources.find { it.serverName == serverName }?.serverId ?: ""
            }
        }

        updateResolutionsForServer(servers.firstOrNull() ?: "")
    }

    private fun updateResolutionsForServer(serverName: String) {
        val currentEp = episode ?: return
        val availableForServer = currentEp.availableSources.filter { it.serverName == serverName }

        rgResolutions.removeAllViews()
        var has720p = false
        var precheckId = -1

        availableForServer.forEach { src ->
            val is720 = src.quality.height == 720
            if (is720) has720p = true

            val rb = RadioButton(context).apply {
                val generatedId = View.generateViewId()
                id = generatedId
                text = if (src.quality.formattedSize.isNotBlank()) "${src.quality.label} (${src.quality.formattedSize})" else src.quality.label
                tag = src
                setTextColor(0xFF292722.toInt())

                if (initialQualityLabel == src.quality.label || (initialQualityLabel == null && is720)) {
                    precheckId = generatedId
                    selectedQualityLabel = src.quality.label
                    selectedServerId = src.serverId
                }
            }
            rgResolutions.addView(rb)
        }

        if (precheckId != -1) {
            rgResolutions.check(precheckId)
        }

        if (!has720p && (initialQualityLabel.isNullOrBlank() || availableForServer.none { it.quality.label == initialQualityLabel })) {
            tvWarning.visibility = View.VISIBLE
        } else {
            tvWarning.visibility = View.GONE
        }

        // Reload player for selected source
        val activeSource = availableForServer.find { it.quality.label == selectedQualityLabel }
            ?: availableForServer.firstOrNull()

        if (activeSource != null) {
            selectedServerId = activeSource.serverId
            selectedQualityLabel = activeSource.quality.label
            loadPreviewSource(activeSource)
        }
    }

    private fun setupListeners() {
        rgServers.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<RadioButton>(checkedId)
            val serverName = rb?.tag as? String ?: return@setOnCheckedChangeListener
            updateResolutionsForServer(serverName)
        }

        rgResolutions.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<RadioButton>(checkedId)
            val source = rb?.tag as? VideoSource ?: return@setOnCheckedChangeListener
            selectedServerId = source.serverId
            selectedQualityLabel = source.quality.label
            tvWarning.visibility = View.GONE
            loadPreviewSource(source)
        }

        btnSave.setOnClickListener {
            if (selectedServerId.isBlank() || selectedQualityLabel.isBlank()) {
                Toast.makeText(context, "Pilih resolusi terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onSaveConfig?.invoke(selectedServerId, selectedQualityLabel)
            dismiss()
        }
    }

    private fun loadPreviewSource(source: VideoSource) {
        val ctx = context ?: return

        pbPlayerLoading.visibility = View.VISIBLE

        if (player == null) {
            player = ExoPlayer.Builder(ctx).build().apply {
                playWhenReady = false // Autoplay disabled per requirements
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            ivPlayerPoster.visibility = View.GONE
                        }
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == androidx.media3.common.Player.STATE_READY && playWhenReady) {
                            ivPlayerPoster.visibility = View.GONE
                        }
                    }
                })
            }
            playerView.player = player
        } else {
            player?.stop()
            player?.clearMediaItems()
        }

        lifecycleScope.launch {
            try {
                val viewModel = (activity as? VideoActivity)?.let {
                    androidx.lifecycle.ViewModelProvider(it)[VideoViewModel::class.java]
                }
                val cached = viewModel?.getPreResolvedSource(source.downloadPageUrl)
                val resolved = cached ?: parser.resolveDirectMediaSource(source)

                val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(mapOf("User-Agent" to resolved.userAgent, "Referer" to resolved.referer))

                val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(resolved.mediaUrl))

                player?.setMediaSource(mediaSource)
                player?.prepare()
                player?.seekTo(0L) // Reset position to 00:00 per requirements
                pbPlayerLoading.visibility = View.GONE
            } catch (e: Exception) {
                pbPlayerLoading.visibility = View.GONE
                Toast.makeText(ctx, "Gagal memuat pratinjau video: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let {
            it.stop()
            it.release()
        }
        player = null
    }
}
