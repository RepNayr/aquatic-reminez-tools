package com.repp.aquaticremineztools.presentation.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.video.model.VideoEpisode
import com.repp.aquaticremineztools.data.video.model.VideoSource
import com.repp.aquaticremineztools.data.video.source.HentaiCopParser

class EpisodeConfigBottomSheet : BottomSheetDialogFragment() {

    private var episode: VideoEpisode? = null
    private var seriesCoverUrl: String = ""
    private var initialServerId: String? = null
    private var initialQualityLabel: String? = null
    private var onSaveConfig: ((serverId: String, qualityLabel: String) -> Unit)? = null

    private lateinit var tvSheetTitle: TextView
    private lateinit var rgServers: RadioGroup
    private lateinit var rgResolutions: RadioGroup
    private lateinit var tvWarning: TextView

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
        rgServers = view.findViewById(R.id.rgServerSelection)
        rgResolutions = view.findViewById(R.id.rgResolutionSelection)
        tvWarning = view.findViewById(R.id.tvResolutionWarning)
    }

    private fun setupUI() {
        val currentEp = episode ?: return
        tvSheetTitle.text = currentEp.title

        val sources = currentEp.availableSources
        if (sources.isEmpty()) {
            Toast.makeText(context, "Tidak ada server/sumber video yang tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        // Group sources by server name
        val servers = sources.map { it.serverName }.distinct()
        val initialSource = sources.find { it.serverId == initialServerId }
        val targetServerName = initialSource?.serverName ?: servers.firstOrNull { s ->
            initialServerId?.contains(s.replace(" ", "_"), ignoreCase = true) == true
        } ?: servers.firstOrNull() ?: ""

        rgServers.removeAllViews()
        var checkedServerId = -1
        servers.forEach { serverName ->
            val rbId = View.generateViewId()
            val rb = RadioButton(context).apply {
                id = rbId
                text = serverName
                tag = serverName
                setTextColor(0xFF292722.toInt())
                applyDarkRadioTint(this)
            }
            rgServers.addView(rb)

            if (serverName.equals(targetServerName, ignoreCase = true)) {
                checkedServerId = rbId
            }
        }

        if (checkedServerId != -1) {
            rgServers.check(checkedServerId)
        }

        updateResolutionsForServer(targetServerName)
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
                applyDarkRadioTint(this)

                if (initialQualityLabel == src.quality.label || (initialQualityLabel == null && is720)) {
                    precheckId = generatedId
                    selectedQualityLabel = src.quality.label
                    selectedServerId = src.serverId
                }
            }
            rgResolutions.addView(rb)
        }

        if (precheckId == -1 && availableForServer.isNotEmpty()) {
            val firstRb = rgResolutions.getChildAt(0) as? RadioButton
            if (firstRb != null) {
                precheckId = firstRb.id
                val firstSrc = availableForServer.first()
                selectedQualityLabel = firstSrc.quality.label
                selectedServerId = firstSrc.serverId
            }
        }

        if (precheckId != -1) {
            rgResolutions.check(precheckId)
        }

        if (!has720p && (initialQualityLabel.isNullOrBlank() || availableForServer.none { it.quality.label == initialQualityLabel })) {
            tvWarning.visibility = View.VISIBLE
        } else {
            tvWarning.visibility = View.GONE
        }

        val activeSource = availableForServer.find { it.quality.label == selectedQualityLabel }
            ?: availableForServer.firstOrNull()

        if (activeSource != null) {
            selectedServerId = activeSource.serverId
            selectedQualityLabel = activeSource.quality.label
        }
    }

    private fun applyDarkRadioTint(radioButton: RadioButton) {
        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                0xFF292722.toInt(),
                0xFF8A8275.toInt()
            )
        )
        androidx.core.widget.CompoundButtonCompat.setButtonTintList(radioButton, colorStateList)
    }

    private fun setupListeners() {
        rgServers.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<RadioButton>(checkedId)
            val serverName = rb?.tag as? String ?: return@setOnCheckedChangeListener
            updateResolutionsForServer(serverName)
            if (selectedServerId.isNotBlank() && selectedQualityLabel.isNotBlank()) {
                onSaveConfig?.invoke(selectedServerId, selectedQualityLabel)
            }
        }

        rgResolutions.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<RadioButton>(checkedId)
            val source = rb?.tag as? VideoSource ?: return@setOnCheckedChangeListener
            selectedServerId = source.serverId
            selectedQualityLabel = source.quality.label
            tvWarning.visibility = View.GONE

            if (selectedServerId.isNotBlank() && selectedQualityLabel.isNotBlank()) {
                onSaveConfig?.invoke(selectedServerId, selectedQualityLabel)
                Toast.makeText(context, "Pengaturan disimpan: $selectedQualityLabel", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
