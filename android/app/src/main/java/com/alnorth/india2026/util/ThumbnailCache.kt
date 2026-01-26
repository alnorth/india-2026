package com.alnorth.india2026.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Handles downloading and caching photo thumbnails locally.
 * Thumbnails are stored in the app's cache directory and persisted across app restarts.
 */
class ThumbnailCache(private val context: Context) {

    companion object {
        private const val THUMBNAIL_DIR = "photo_thumbnails"
        private const val THUMBNAIL_SIZE = 200 // pixels
        private const val JPEG_QUALITY = 80
    }

    private val thumbnailDir: File by lazy {
        File(context.cacheDir, THUMBNAIL_DIR).also { it.mkdirs() }
    }

    /**
     * Get thumbnail file for a photo URL.
     * Downloads and creates thumbnail if not cached.
     * Returns null if download fails.
     */
    suspend fun getThumbnail(photoUrl: String): File? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(photoUrl)
        val thumbnailFile = File(thumbnailDir, "$cacheKey.jpg")

        // Return cached thumbnail if exists
        if (thumbnailFile.exists()) {
            return@withContext thumbnailFile
        }

        // Download and create thumbnail
        try {
            val originalBitmap = downloadImage(photoUrl) ?: return@withContext null
            val thumbnail = createThumbnail(originalBitmap)
            saveThumbnail(thumbnail, thumbnailFile)
            thumbnail.recycle()
            originalBitmap.recycle()
            thumbnailFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a thumbnail is already cached for the given URL.
     */
    fun isCached(photoUrl: String): Boolean {
        val cacheKey = generateCacheKey(photoUrl)
        val thumbnailFile = File(thumbnailDir, "$cacheKey.jpg")
        return thumbnailFile.exists()
    }

    /**
     * Get the cached thumbnail file path without downloading.
     * Returns null if not cached.
     */
    fun getCachedThumbnailPath(photoUrl: String): File? {
        val cacheKey = generateCacheKey(photoUrl)
        val thumbnailFile = File(thumbnailDir, "$cacheKey.jpg")
        return if (thumbnailFile.exists()) thumbnailFile else null
    }

    /**
     * Clear all cached thumbnails.
     */
    fun clearCache() {
        thumbnailDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Get the size of the thumbnail cache in bytes.
     */
    fun getCacheSize(): Long {
        return thumbnailDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun generateCacheKey(url: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(url.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun downloadImage(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.getInputStream().use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createThumbnail(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        // Calculate scale to fit within THUMBNAIL_SIZE while maintaining aspect ratio
        val scale = minOf(
            THUMBNAIL_SIZE.toFloat() / width,
            THUMBNAIL_SIZE.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }

    private fun saveThumbnail(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        }
    }
}
