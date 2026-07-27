package com.finnflow.data.logger

import android.util.Log
import com.finnflow.BuildConfig

object SecureLogger {
    private const val TAG = "FinnFlow"

    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    fun d(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message, throwable)
        }
    }

    fun i(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message, throwable)
        } else {
            Log.i(tag, maskSensitiveData(message))
        }
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        val logMsg = maskSensitiveData(message)
        Log.w(tag, logMsg, throwable)
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        val logMsg = maskSensitiveData(message)
        Log.e(tag, logMsg, throwable)
    }

    private fun maskSensitiveData(message: String): String {
        var masked = message
        masked = maskEmails(masked)
        masked = maskAmounts(masked)
        masked = maskTransactionIds(masked)
        masked = maskUserIds(masked)
        masked = maskApiKeys(masked)
        masked = maskTokens(masked)
        return masked
    }

    private fun maskEmails(text: String): String =
        text.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "***@***.***")

    private fun maskAmounts(text: String): String =
        text.replace(Regex("amount[=:]\\s*([0-9.]+)"), "amount=***")
            .replace(Regex("\\$([0-9.]+)"), "$***")
            .replace(Regex("amount\\s*([0-9.]+)"), "amount ***")

    private fun maskTransactionIds(text: String): String =
        text.replace(Regex("transactionId[=:]\\s*([0-9a-fA-F-]+)"), "transactionId=***")
            .replace(Regex("transaction_id[=:]\\s*([0-9a-fA-F-]+)"), "transaction_id=***")

    private fun maskUserIds(text: String): String =
        text.replace(Regex("userId[=:]\\s*([0-9a-fA-F-]+)"), "userId=***")
            .replace(Regex("user_id[=:]\\s*([0-9a-fA-F-]+)"), "user_id=***")

    private fun maskApiKeys(text: String): String =
        text.replace(Regex("apiKey[=:]\\s*([a-zA-Z0-9._-]+)"), "apiKey=***")
            .replace(Regex("api_key[=:]\\s*([a-zA-Z0-9._-]+)"), "api_key=***")

    private fun maskTokens(text: String): String =
        text.replace(Regex("token[=:]\\s*([a-zA-Z0-9._-]+)"), "token=***")
            .replace(Regex("accessToken[=:]\\s*([a-zA-Z0-9._-]+)"), "accessToken=***")
}

inline fun <T> withLogging(tag: String, action: String, block: () -> T): T {
    SecureLogger.d(tag, "Starting: $action")
    return try {
        val result = block()
        SecureLogger.d(tag, "Completed: $action")
        result
    } catch (e: Exception) {
        SecureLogger.e(tag, "Failed: $action", e)
        throw e
    }
}

suspend inline fun <T> withLoggingSuspend(tag: String, action: String, block: suspend () -> T): T {
    SecureLogger.d(tag, "Starting: $action")
    return try {
        val result = block()
        SecureLogger.d(tag, "Completed: $action")
        result
    } catch (e: Exception) {
        SecureLogger.e(tag, "Failed: $action", e)
        throw e
    }
}
