package com.example

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class AppState {
    IDLE,
    VIDEO_SELECTED,
    CONVERTING,
    COMPLETED,
    FAILED
}

data class SelectedVideo(
    val uri: Uri,
    val name: String,
    val sizeString: String,
    val durationString: String
)

class MainViewModel : ViewModel() {

    private val _appState = MutableStateFlow(AppState.IDLE)
    val appState: StateFlow<AppState> = _appState

    private val _selectedVideo = MutableStateFlow<SelectedVideo?>(null)
    val selectedVideo: StateFlow<SelectedVideo?> = _selectedVideo

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _outputUri = MutableStateFlow<Uri?>(null)
    val outputUri: StateFlow<Uri?> = _outputUri

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun selectVideo(context: Context, uri: Uri) {
        _errorMessage.value = null
        _outputUri.value = null
        _progress.value = 0f

        val details = queryVideoDetails(context, uri)
        if (details != null) {
            _selectedVideo.value = details
            _appState.value = AppState.VIDEO_SELECTED
        } else {
            _errorMessage.value = "Could not load video metadata. Format may be unsupported."
            _appState.value = AppState.FAILED
        }
    }

    fun startConversion(context: Context) {
        val video = _selectedVideo.value ?: return
        _appState.value = AppState.CONVERTING
        _progress.value = 0f
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.Default) {
            val tempFile = File(context.cacheDir, "temp_cfr_fixed.mp4")
            if (tempFile.exists()) {
                tempFile.delete()
            }

            val success = VfrToCfrTranscoder.transcode(
                context = context,
                inputUri = video.uri,
                outputFile = tempFile,
                onProgress = { currentProgress ->
                    _progress.value = currentProgress
                }
            )

            if (success && tempFile.exists()) {
                val savedUri = MediaStoreHelper.saveVideoToPublicMovies(context, tempFile, video.name)
                tempFile.delete()
                if (savedUri != null) {
                    _outputUri.value = savedUri
                    _progress.value = 1.0f
                    _appState.value = AppState.COMPLETED
                } else {
                    _errorMessage.value = "Failed saving transcoded video to Movies folder."
                    _appState.value = AppState.FAILED
                }
            } else {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                _errorMessage.value = "Transcoding error. Hardware encoder failed to force 60 FPS on this file."
                _appState.value = AppState.FAILED
            }
        }
    }

    fun resetState() {
        _appState.value = AppState.IDLE
        _selectedVideo.value = null
        _progress.value = 0f
        _outputUri.value = null
        _errorMessage.value = null
    }

    private fun queryVideoDetails(context: Context, uri: Uri): SelectedVideo? {
        var name = "recording.mp4"
        var sizeBytes = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: "recording.mp4"
                    }
                    if (sizeIndex >= 0) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback name remains
        }

        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            // Retain default durationMs
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }

        return SelectedVideo(
            uri = uri,
            name = name,
            sizeString = formatSize(sizeBytes),
            durationString = formatDuration(durationMs)
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }
}
