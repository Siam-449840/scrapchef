package com.scrapchef.android.util

import android.util.Log
import com.scrapchef.android.BuildConfig

/**
 * Centralized logging utility for ScrapChef.
 *
 * Uses inline functions with lambdas to prevent string interpolation
 * overhead when the log is not actually printed.
 *
 * ERROR and WARN levels always emit (even in release builds) to ensure
 * production observability. DEBUG and INFO are gated on BuildConfig.DEBUG.
 */
object ScrapChefLogger {

    inline fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message())
        }
    }

    inline fun i(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message())
        }
    }

    inline fun w(tag: String, message: () -> String) {
        // Always emit warnings for production observability
        Log.w(tag, message())
    }

    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        Log.w(tag, message(), throwable)
    }

    inline fun e(tag: String, message: () -> String) {
        // Always emit errors for production observability
        Log.e(tag, message())
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        Log.e(tag, message(), throwable)
    }
}
