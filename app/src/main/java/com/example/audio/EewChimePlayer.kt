package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class EewChimePlayer {

    private val TAG = "EewChimePlayer"

    suspend fun playChime() = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val durationSec = 0.55 // Duration of each chime chord
        val numSamples = (sampleRate * durationSec).toInt()

        // Chord 1 (C7(b9)): C4 (261.63), E4 (329.63), G4 (392.00), Bb4 (466.16), Db5 (554.37)
        val chord1 = doubleArrayOf(261.63, 329.63, 392.00, 466.16, 554.37)
        // Chord 2 (Cdim7): C4 (261.63), Eb4 (311.13), Gb4 (369.99), A4 (440.00), C5 (523.25)
        val chord2 = doubleArrayOf(261.63, 311.13, 369.99, 440.00, 523.25)

        val buffer1 = ShortArray(numSamples)
        val buffer2 = ShortArray(numSamples)

        // Generate Chord 1 with smooth attack and decay envelope
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for (freq in chord1) {
                sample += sin(2.0 * Math.PI * freq * t)
            }
            sample /= chord1.size // Normalize to avoid clipping

            // Apply linear attack and decay envelope to sound less abrupt
            val envelope = when {
                i < 2000 -> i.toDouble() / 2000.0 // attack (first ~45ms)
                i > numSamples - 4000 -> (numSamples - i).toDouble() / 4000.0 // decay (last ~90ms)
                else -> 1.0
            }
            buffer1[i] = (sample * Short.MAX_VALUE * envelope).toInt().toShort()
        }

        // Generate Chord 2 with smooth attack and decay envelope
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for (freq in chord2) {
                sample += sin(2.0 * Math.PI * freq * t)
            }
            sample /= chord2.size // Normalize

            // Apply envelope
            val envelope = when {
                i < 2000 -> i.toDouble() / 2000.0 // attack
                i > numSamples - 4000 -> (numSamples - i).toDouble() / 4000.0 // decay
                else -> 1.0
            }
            buffer2[i] = (sample * Short.MAX_VALUE * envelope).toInt().toShort()
        }

        // Initialize AudioTrack with ALARM or NOTIFICATION attributes
        val bufferSize = numSamples * 2 // 2 bytes per 16-bit sample

        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                android.media.AudioManager.STREAM_ALARM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        try {
            audioTrack.play()
            // Repeat 4 times to sound: Ding-Dong Ding-Dong Ding-Dong Ding-Dong
            repeat(4) {
                audioTrack.write(buffer1, 0, buffer1.size)
                audioTrack.write(buffer2, 0, buffer2.size)
            }
            audioTrack.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing chime synthesized audio", e)
        } finally {
            try {
                audioTrack.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
