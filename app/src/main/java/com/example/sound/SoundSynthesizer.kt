package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundSynthesizer {
    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.IO)
    var isSoundEnabled = true

    fun playTick() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Synthesize a short tick: 25ms duration
                val durationMs = 25
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Frequency glide from 2500Hz to 200Hz for a crisp click
                    val frequency = 2500.0 - (2300.0 * (i.toDouble() / numSamples))
                    // Exponential decay envelope
                    val envelope = kotlin.math.exp(-6.0 * (i.toDouble() / numSamples))
                    val value = sin(2.0 * Math.PI * frequency * t) * envelope
                    samples[i] = (value * Short.MAX_VALUE * 0.35).toInt().toShort()
                }

                playPcm(samples)
            } catch (e: Exception) {
                Log.e("SoundSynthesizer", "Error playing tick sound", e)
            }
        }
    }

    fun playWin() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Synthesize a fanfare chord arpeggio: C5 (523.25 Hz), E5 (659.25 Hz), G5 (783.99 Hz), C6 (1046.50 Hz)
                val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                val noteDurationMs = 140
                val chordDurationMs = 600
                
                val totalSamplesList = ArrayList<Short>()
                
                // 1. Upward arpeggio
                for (freq in notes) {
                    val numSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        // Subtle envelope decay
                        val envelope = 1.0 - (i.toDouble() / numSamples) * 0.4
                        val value = sin(2.0 * Math.PI * freq * t) * envelope
                        totalSamplesList.add((value * Short.MAX_VALUE * 0.4).toInt().toShort())
                    }
                }
                
                // 2. Vibrant final chord (with a bit of vibrato or rich harmony)
                val chordSamples = (sampleRate * (chordDurationMs / 1000.0)).toInt()
                for (i in 0 until chordSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = kotlin.math.exp(-2.5 * (i.toDouble() / chordSamples))
                    
                    var value = 0.0
                    for (freq in notes) {
                        // Add a slight frequency modulation (vibrato) for organic analog synth warmth
                        val vibrato = 1.0 + 0.01 * sin(2.0 * Math.PI * 8.0 * t)
                        value += sin(2.0 * Math.PI * freq * vibrato * t)
                    }
                    value /= notes.size // Normalize to prevent clipping
                    value *= envelope
                    totalSamplesList.add((value * Short.MAX_VALUE * 0.5).toInt().toShort())
                }
                
                val samplesArray = ShortArray(totalSamplesList.size)
                for (i in totalSamplesList.indices) {
                    samplesArray[i] = totalSamplesList[i]
                }
                
                playPcm(samplesArray)
            } catch (e: Exception) {
                Log.e("SoundSynthesizer", "Error playing win sound", e)
            }
        }
    }

    private fun playPcm(samples: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)
        
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
            .setTransferMode(AudioTrack.MODE_STATIC)

        val audioTrack = builder.build()
            
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        
        scope.launch {
            val playDurationMs = (samples.size.toDouble() / sampleRate * 1000).toLong()
            kotlinx.coroutines.delay(playDurationMs + 100)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
