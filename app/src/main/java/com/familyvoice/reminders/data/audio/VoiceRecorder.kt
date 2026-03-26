package com.familyvoice.reminders.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG        = "VoiceRecorder"
private const val AUDIO_FILE = "temp_reminder.m4a"

/**
 * Wraps [MediaRecorder] with a simple start / pause / resume / stop / cancel API.
 *
 * Audio is written to [Context.getCacheDir]/temp_reminder.m4a.
 * The file is overwritten each time [start] is called.
 *
 * Thread-safety: all methods must be called from the same thread (main thread).
 */
@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File?        = null
    private var started: Boolean         = false

    /**
     * Start a new recording session.
     * Safe to call even if a previous session was never cancelled — it will be cleaned up.
     */
    fun start(): File {
        releaseRecorder()
        val file = File(context.cacheDir, AUDIO_FILE).also { outputFile = it }

        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        started = true
        Log.d(TAG, "Recording started → ${file.absolutePath}")
        return file
    }

    /** Pause an active recording. No-op if not currently recording. */
    fun pause() {
        if (!started) return
        recorder?.pause()
        Log.d(TAG, "Recording paused")
    }

    /** Resume after [pause]. No-op if not started. */
    fun resume() {
        if (!started) return
        recorder?.resume()
        Log.d(TAG, "Recording resumed")
    }

    /**
     * Stop recording and return the output file.
     * Returns `null` if nothing was recorded (e.g. stop called before any audio frame).
     */
    fun stop(): File? {
        if (!started) return null
        val file = outputFile
        return try {
            recorder?.stop()
            Log.i(TAG, "Готово к отправке в Gemini: ${file?.absolutePath}")
            file
        } catch (e: RuntimeException) {
            // stop() throws if called before the first audio frame is captured
            Log.e(TAG, "stop() failed — no audio captured", e)
            file?.delete()
            null
        } finally {
            releaseRecorder()
        }
    }

    /**
     * Stop recording (if active) and delete the temp file.
     * Safe to call in any state.
     */
    fun cancel() {
        if (started) {
            try {
                recorder?.stop()
            } catch (e: RuntimeException) {
                Log.w(TAG, "cancel(): stop() threw (no audio frame captured)", e)
            }
        }
        outputFile?.delete()
        outputFile = null
        releaseRecorder()
        Log.d(TAG, "Recording cancelled and temp file deleted")
    }

    private fun releaseRecorder() {
        recorder?.release()
        recorder = null
        started  = false
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
