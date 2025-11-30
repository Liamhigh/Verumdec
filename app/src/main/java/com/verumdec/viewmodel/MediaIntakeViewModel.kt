package com.verumdec.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.verumdec.data.Evidence
import com.verumdec.data.EvidenceMetadata
import com.verumdec.data.EvidenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Date

/**
 * ViewModel for media (image) intake operations.
 * Handles image selection, preview, metadata extraction, and OCR preparation.
 */
class MediaIntakeViewModel(application: Application) : AndroidViewModel(application) {

    private val _mediaItems = MutableLiveData<List<MediaItem>>(emptyList())
    val mediaItems: LiveData<List<MediaItem>> = _mediaItems

    private val _selectedMedia = MutableLiveData<MediaItem?>()
    val selectedMedia: LiveData<MediaItem?> = _selectedMedia

    private val _processingState = MutableLiveData<ProcessingState>(ProcessingState.Idle)
    val processingState: LiveData<ProcessingState> = _processingState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val mediaUris = mutableMapOf<String, Uri>()

    /**
     * Add a media item from URI.
     */
    fun addMedia(uri: Uri, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading("Adding media...")
            try {
                val evidence = Evidence(
                    type = EvidenceType.IMAGE,
                    fileName = fileName,
                    filePath = uri.toString()
                )

                val thumbnail = withContext(Dispatchers.IO) {
                    loadThumbnail(uri)
                }

                val metadata = withContext(Dispatchers.IO) {
                    extractMetadata(uri)
                }

                val mediaItem = MediaItem(
                    evidence = evidence,
                    thumbnail = thumbnail,
                    metadata = metadata
                )

                mediaUris[evidence.id] = uri

                val currentList = _mediaItems.value.orEmpty().toMutableList()
                currentList.add(mediaItem)
                _mediaItems.value = currentList

                _processingState.value = ProcessingState.Success("Media added")
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "Failed to add media")
                _error.value = e.message
            }
        }
    }

    /**
     * Select a media item for preview/editing.
     */
    fun selectMedia(mediaItem: MediaItem) {
        _selectedMedia.value = mediaItem
    }

    /**
     * Remove a media item.
     */
    fun removeMedia(mediaItem: MediaItem) {
        mediaUris.remove(mediaItem.evidence.id)
        val currentList = _mediaItems.value.orEmpty().toMutableList()
        currentList.removeAll { it.evidence.id == mediaItem.evidence.id }
        _mediaItems.value = currentList

        if (_selectedMedia.value?.evidence?.id == mediaItem.evidence.id) {
            _selectedMedia.value = null
        }
    }

    /**
     * Get URI for a media item.
     */
    fun getMediaUri(evidenceId: String): Uri? = mediaUris[evidenceId]

    /**
     * Get all media as Evidence list.
     */
    fun getAllEvidence(): List<Evidence> = _mediaItems.value.orEmpty().map { it.evidence }

    /**
     * Get all URIs map.
     */
    fun getAllMediaUris(): Map<String, Uri> = mediaUris.toMap()

    /**
     * Clear all media.
     */
    fun clearAll() {
        mediaUris.clear()
        _mediaItems.value = emptyList()
        _selectedMedia.value = null
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    private fun loadThumbnail(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = getApplication<Application>()
                .contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                // Calculate sample size for thumbnail
                val targetSize = 200
                var sampleSize = 1
                while (options.outWidth / sampleSize > targetSize && 
                       options.outHeight / sampleSize > targetSize) {
                    sampleSize *= 2
                }

                // Reload with sample size
                val thumbOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                getApplication<Application>()
                    .contentResolver.openInputStream(uri)?.use { thumbStream ->
                        BitmapFactory.decodeStream(thumbStream, null, thumbOptions)
                    }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractMetadata(uri: Uri): EvidenceMetadata {
        return try {
            val inputStream = getApplication<Application>()
                .contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                val exifData = mutableMapOf<String, String>()
                exifData["width"] = options.outWidth.toString()
                exifData["height"] = options.outHeight.toString()
                exifData["mimeType"] = options.outMimeType ?: "unknown"

                EvidenceMetadata(
                    creationDate = Date(),
                    exifData = exifData
                )
            } ?: EvidenceMetadata()
        } catch (e: Exception) {
            EvidenceMetadata()
        }
    }

    data class MediaItem(
        val evidence: Evidence,
        val thumbnail: Bitmap?,
        val metadata: EvidenceMetadata,
        val ocrText: String = "",
        val isProcessed: Boolean = false
    )

    sealed class ProcessingState {
        object Idle : ProcessingState()
        data class Loading(val message: String) : ProcessingState()
        data class Success(val message: String) : ProcessingState()
        data class Error(val message: String) : ProcessingState()
    }
}
