package com.scrapchef.android.util

import android.util.Log
import com.scrapchef.android.BuildConfig

/**
 * Centralized logging utility for ScrapChef.
 * 
 * Uses inline functions with lambdas to prevent string interpolation
 * overhead in Release builds when the log is not actually printed.
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
        if (BuildConfig.DEBUG) {
            Log.w(tag, message())
        }
    }

    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message(), throwable)
        }
    }

    inline fun e(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message())
        }
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message(), throwable)
        }
    }
}
