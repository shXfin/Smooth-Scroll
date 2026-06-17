package com.example

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object VfrToCfrTranscoder {
    private const val TAG = "VfrToCfrTranscoder"
    private const val TIMEOUT_USEC = 10000L

    suspend fun transcode(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        var isFinished = false
        val thread = Thread {
            try {
                val success = runTranscode(context, inputUri, outputFile, onProgress)
                if (!isFinished) {
                    isFinished = true
                    continuation.resume(success)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcoding thread exception", e)
                if (!isFinished) {
                    isFinished = true
                    continuation.resume(false)
                }
            }
        }

        continuation.invokeOnCancellation {
            isFinished = true
            thread.interrupt()
        }

        thread.start()
    }

    private fun runTranscode(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null
        var muxerStarted = false

        try {
            // Retrieve rotation and duration
            val retriever = MediaMetadataRetriever()
            var rotation = 0
            var durationUs = 0L
            try {
                retriever.setDataSource(context, inputUri)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                rotation = rotationStr?.toIntOrNull() ?: 0
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationUs = (durationStr?.toLongOrNull() ?: 0L) * 1000L
            } catch (e: Exception) {
                Log.e(TAG, "Failed retrieving media metadata", e)
            } finally {
                try { retriever.release() } catch (ignored: Exception) {}
            }

            videoExtractor = MediaExtractor().apply { setDataSource(context, inputUri, null) }
            audioExtractor = MediaExtractor().apply { setDataSource(context, inputUri, null) }

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                Log.e(TAG, "No video track found in input file")
                return false
            }

            videoExtractor.selectTrack(videoTrackIndex)
            if (audioTrackIndex >= 0) {
                audioExtractor.selectTrack(audioTrackIndex)
            }

            val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)

            // Setup Encoder for constant H.264 at 60 FPS
            val targetFps = 60
            val outputFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 15_000_000) // High quality 15 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1s keyframes for Premiere Pro
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()

            decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME)!!)
            decoder.configure(videoFormat, inputSurface, null, 0)

            encoder.start()
            decoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotation)

            var videoMuxerTrackIndex = -1
            var audioMuxerTrackIndex = -1

            var decoderInputDone = false
            var decoderOutputDone = false
            var encoderOutputDone = false
            var emptyEncoderDequeueCount = 0

            var frameCount = 0L
            val bufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()

            val audioBuf = ByteBuffer.allocateDirect(512 * 1024)
            val audioInfo = MediaCodec.BufferInfo()

            while (!encoderOutputDone && !Thread.currentThread().isInterrupted) {
                var workDone = false

                // 1. Feed Decoder with samples from Extractor
                if (!decoderInputDone) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufferIndex >= 0) {
                        workDone = true
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = videoExtractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            decoderInputDone = true
                        } else {
                            val sampleTimeUs = videoExtractor.sampleTime
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTimeUs, 0)
                            videoExtractor.advance()
                        }
                    }
                }

                // 2. Poll output of Decoder -> Release to Encoder surface with strictly spaced PTS (60 FPS CFR)
                if (!decoderOutputDone) {
                    val decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    if (decoderStatus >= 0) {
                        workDone = true
                        val isEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val isSample = bufferInfo.size > 0

                        if (isSample) {
                            // strictly spaced at targetFps interval in Nanoseconds
                            val outputPtsNs = frameCount * (1_000_000_000L / targetFps)
                            decoder.releaseOutputBuffer(decoderStatus, outputPtsNs)
                            frameCount++

                            if (durationUs > 0) {
                                val progress = bufferInfo.presentationTimeUs.toFloat() / durationUs
                                onProgress(progress.coerceIn(0f, 0.99f))
                            }
                        } else {
                            decoder.releaseOutputBuffer(decoderStatus, false)
                        }

                        if (isEos) {
                            decoderOutputDone = true
                            try {
                                val method = MediaCodec::class.java.getMethod("signalEndOfStream")
                                method.invoke(encoder)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed signaling EOS", e)
                            }
                        }
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED || decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        workDone = true
                    }
                }

                // 3. Dequeue from Encoder -> write to MediaMuxer
                if (!encoderOutputDone) {
                    val encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
                    if (encoderStatus >= 0) {
                        workDone = true
                        emptyEncoderDequeueCount = 0
                        val encodedData = encoder.getOutputBuffer(encoderStatus)!!
                        if (encoderBufferInfo.size > 0) {
                            if (!muxerStarted) {
                                Log.e(TAG, "Muxer not started but encoder returned data")
                                return false
                            }
                            muxer.writeSampleData(videoMuxerTrackIndex, encodedData, encoderBufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)

                        if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderOutputDone = true
                        }
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        workDone = true
                        if (muxerStarted) {
                            Log.e(TAG, "Encoder output format changed twice!")
                            return false
                        }
                        val newVideoFormat = encoder.outputFormat
                        videoMuxerTrackIndex = muxer.addTrack(newVideoFormat)

                        if (audioTrackIndex >= 0 && audioFormat != null) {
                            audioMuxerTrackIndex = muxer.addTrack(audioFormat)
                        }

                        muxer.start()
                        muxerStarted = true
                    } else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (decoderOutputDone) {
                            emptyEncoderDequeueCount++
                            if (emptyEncoderDequeueCount > 300) {
                                Log.w(TAG, "Timeout waiting for encoder EOS (decoder is done). Forcing completion.")
                                encoderOutputDone = true
                            }
                        }
                    }
                }

                // 4. Ingest and Interleave Audio seamlessly
                if (muxerStarted && audioTrackIndex >= 0) {
                    val currentAudioTimeUs = audioExtractor.sampleTime
                    if (currentAudioTimeUs < 0) {
                        audioTrackIndex = -1
                    } else {
                        // Keep audio slightly integrated with the current frame's timing projection
                        val targetVideoTimeUs = frameCount * (1000000L / targetFps)
                        if (currentAudioTimeUs <= targetVideoTimeUs) {
                            workDone = true
                            audioInfo.offset = 0
                            val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                            if (sampleSize >= 0) {
                                audioInfo.size = sampleSize
                                audioInfo.presentationTimeUs = currentAudioTimeUs
                                audioInfo.flags = audioExtractor.sampleFlags
                                muxer.writeSampleData(audioMuxerTrackIndex, audioBuf, audioInfo)
                                audioExtractor.advance()
                            } else {
                                audioTrackIndex = -1 // Audio track EOF
                            }
                        }
                    }
                }

                // If no active work was processed in this pass, sleep briefly to avoid pegging the CPU
                if (!workDone) {
                    try {
                        Thread.sleep(5)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            // Flush any remaining audio packets
            if (muxerStarted && audioTrackIndex >= 0 && !Thread.currentThread().isInterrupted) {
                while (audioTrackIndex >= 0) {
                    val currentAudioTimeUs = audioExtractor.sampleTime
                    if (currentAudioTimeUs < 0) {
                        audioTrackIndex = -1
                        break
                    }
                    audioInfo.offset = 0
                    val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                    if (sampleSize >= 0) {
                        audioInfo.size = sampleSize
                        audioInfo.presentationTimeUs = currentAudioTimeUs
                        audioInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(audioMuxerTrackIndex, audioBuf, audioInfo)
                        audioExtractor.advance()
                    } else {
                        audioTrackIndex = -1
                    }
                }
            }

            return !Thread.currentThread().isInterrupted && encoderOutputDone
        } catch (e: Exception) {
            Log.e(TAG, "Error during video transcoding runTranscode()", e)
            return false
        } finally {
            // Safe cleanup
            try { decoder?.stop() } catch (ignored: Exception) {}
            try { decoder?.release() } catch (ignored: Exception) {}
            try { encoder?.stop() } catch (ignored: Exception) {}
            try { encoder?.release() } catch (ignored: Exception) {}
            try { inputSurface?.release() } catch (ignored: Exception) {}
            try { videoExtractor?.release() } catch (ignored: Exception) {}
            try { audioExtractor?.release() } catch (ignored: Exception) {}
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
            } catch (ignored: Exception) {}
            try { muxer?.release() } catch (ignored: Exception) {}
        }
    }
}
