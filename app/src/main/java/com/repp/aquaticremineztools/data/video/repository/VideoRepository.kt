package com.repp.aquaticremineztools.data.video.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.repp.aquaticremineztools.data.local.video.VideoDatabase
import com.repp.aquaticremineztools.data.local.video.VideoDownloadEntity
import com.repp.aquaticremineztools.data.local.video.VideoResultEntity
import com.repp.aquaticremineztools.data.video.model.VideoEpisode
import com.repp.aquaticremineztools.domain.video.worker.VideoDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    private val db = VideoDatabase.getInstance(context)
    private val downloadDao = db.videoDownloadDao()
    private val resultDao = db.videoResultDao()

    val activeDownloads: LiveData<List<VideoDownloadEntity>> = downloadDao.getActiveAndQueuedLiveData()
    val videoResults: LiveData<List<VideoResultEntity>> = resultDao.getAllLiveData()

    suspend fun enqueueDownload(
        seriesTitle: String,
        episode: VideoEpisode,
        serverId: String,
        qualityLabel: String
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = downloadDao.getActiveByEpisodeUrl(episode.url)
        if (existing != null) {
            return@withContext false
        }

        val source = episode.availableSources.find { it.serverId == serverId || it.quality.label == qualityLabel }
            ?: episode.availableSources.find { it.quality.height == 720 }
            ?: episode.availableSources.firstOrNull()

        if (source == null) {
            return@withContext false
        }

        val safeTitle = "${seriesTitle}_${episode.title}".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "${safeTitle}_${source.quality.label}.mp4"

        val entity = VideoDownloadEntity(
            seriesTitle = seriesTitle,
            episodeTitle = episode.title,
            episodeUrl = episode.url,
            downloadPageUrl = source.downloadPageUrl,
            serverId = source.serverId,
            qualityLabel = source.quality.label,
            outputFileName = fileName,
            status = VideoDownloadEntity.STATUS_QUEUED,
            stage = VideoDownloadEntity.STAGE_DOWNLOADING_VIDEO,
            totalBytes = source.quality.fileSizeBytes
        )

        downloadDao.insert(entity)
        VideoDownloadWorker.enqueueWork(context)
        return@withContext true
    }

    suspend fun enqueueCompression(result: VideoResultEntity, targetSizeBytes: Long): Boolean = withContext(Dispatchers.IO) {
        val safeTitle = "${result.seriesTitle}_${result.episodeTitle}_compressed".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "${safeTitle}_${targetSizeBytes / (1024 * 1024)}MB.mp4"

        val entity = VideoDownloadEntity(
            seriesTitle = result.seriesTitle,
            episodeTitle = result.episodeTitle,
            episodeUrl = result.episodeUrl,
            downloadPageUrl = result.videoPath,
            serverId = "compression",
            qualityLabel = result.resolutionLabel,
            outputFileName = fileName,
            status = VideoDownloadEntity.STATUS_QUEUED,
            stage = VideoDownloadEntity.STAGE_COMPRESSING_VIDEO,
            totalBytes = targetSizeBytes,
            videoPath = result.videoPath
        )

        downloadDao.insert(entity)
        resultDao.updateStatus(result.id, VideoResultEntity.STATUS_COMPRESSING)
        VideoDownloadWorker.enqueueWork(context)
        return@withContext true
    }

    suspend fun cancelDownload(id: Long) = withContext(Dispatchers.IO) {
        VideoDownloadWorker.cancelWorkIfRunning(context, id)
        downloadDao.cancelDownload(id)
    }

    suspend fun deleteDownload(id: Long) = withContext(Dispatchers.IO) {
        downloadDao.deleteById(id)
    }

    suspend fun deleteResult(id: Long) = withContext(Dispatchers.IO) {
        val item = resultDao.getById(id)
        if (item != null) {
            val file = java.io.File(item.videoPath)
            if (file.exists()) {
                file.delete()
            }
            resultDao.deleteById(id)
        }
    }
}
