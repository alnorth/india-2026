package com.alnorth.india2026.util

import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Retry configuration for network operations
 */
data class RetryConfig(
    val maxAttempts: Int = 4,
    val initialDelayMs: Long = 2000,
    val maxDelayMs: Long = 16000,
    val multiplier: Double = 2.0
)

/**
 * Result of a retry operation, including whether retries were needed
 */
sealed class RetryResult<T> {
    data class Success<T>(val value: T, val attemptNumber: Int) : RetryResult<T>()
    data class Failure<T>(val exception: Exception, val attemptsMade: Int) : RetryResult<T>()
}

/**
 * Executes a suspending operation with exponential backoff retry logic.
 * Only retries on transient network errors (IOException, SocketTimeoutException).
 *
 * @param config Retry configuration (max attempts, delays)
 * @param onRetry Callback invoked before each retry attempt with (attempt number, delay ms, exception)
 * @param operation The suspending operation to execute
 * @return RetryResult indicating success or failure with attempt information
 */
suspend fun <T> withRetry(
    config: RetryConfig = RetryConfig(),
    onRetry: (attempt: Int, delayMs: Long, error: Exception) -> Unit = { _, _, _ -> },
    operation: suspend () -> T
): RetryResult<T> {
    var currentDelay = config.initialDelayMs
    var lastException: Exception? = null

    repeat(config.maxAttempts) { attempt ->
        try {
            val result = operation()
            return RetryResult.Success(result, attempt + 1)
        } catch (e: Exception) {
            lastException = e

            // Only retry on transient network errors
            if (!isRetryableException(e)) {
                return RetryResult.Failure(e, attempt + 1)
            }

            // Don't retry if we've exhausted attempts
            if (attempt == config.maxAttempts - 1) {
                return RetryResult.Failure(e, attempt + 1)
            }

            // Notify about retry
            onRetry(attempt + 1, currentDelay, e)

            // Wait before retrying
            delay(currentDelay)

            // Increase delay for next attempt (exponential backoff)
            currentDelay = (currentDelay * config.multiplier).toLong()
                .coerceAtMost(config.maxDelayMs)
        }
    }

    return RetryResult.Failure(
        lastException ?: IllegalStateException("Retry exhausted with no exception"),
        config.maxAttempts
    )
}

/**
 * Determines if an exception is a transient network error worth retrying
 */
private fun isRetryableException(e: Exception): Boolean {
    return when (e) {
        is SocketTimeoutException -> true
        is IOException -> true
        else -> {
            // Check for retrofit/okhttp network errors
            val message = e.message?.lowercase() ?: ""
            message.contains("timeout") ||
            message.contains("connection") ||
            message.contains("network") ||
            message.contains("socket") ||
            message.contains("reset") ||
            // HTTP 5xx errors often wrapped
            message.contains("500") ||
            message.contains("502") ||
            message.contains("503") ||
            message.contains("504")
        }
    }
}

/**
 * Extension to convert RetryResult to a standard Result
 */
fun <T> RetryResult<T>.toResult(): Result<T> = when (this) {
    is RetryResult.Success -> Result.success(value)
    is RetryResult.Failure -> Result.failure(exception)
}
