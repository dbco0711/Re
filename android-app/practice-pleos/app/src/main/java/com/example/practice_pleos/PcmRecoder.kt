package com.example.practice_pleos

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File

class PcmRecorder(private val sessionDir: File) {
    @Volatile
    private var recording = false

    private var worker: Thread? = null

    // MainActivity에서 RECORD_AUDIO 권한을 확인한 뒤 호출합니다.
    @SuppressLint("MissingPermission")
    fun start(
        onSegmentReady: (File, Long, Long) -> Unit,
        onFinished: (Result<File>) -> Unit
    ) {
        if (worker != null) return
        recording = true

        worker = Thread {
            val result = runCatching {
                val sampleRate = 16000
                val channel = AudioFormat.CHANNEL_IN_MONO
                val format = AudioFormat.ENCODING_PCM_16BIT
                val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, format)
                check(minBuffer > 0) { "PCM 녹음 설정을 사용할 수 없습니다." }

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channel,
                    format,
                    maxOf(minBuffer, 3200)
                )

                try {
                    check(recorder.state == AudioRecord.STATE_INITIALIZED) {
                        "마이크를 초기화하지 못했습니다."
                    }

                    check(sessionDir.isDirectory || sessionDir.mkdirs()) {
                        "녹음 폴더를 만들 수 없습니다: ${sessionDir.absolutePath}"
                    }

                    val segmentSize = 320_000 // 16,000Hz × 2바이트 × 10초
                    val buffer = ByteArray(3200)
                    var segmentNumber = 1
                    var bytesInSegment = 0
                    var currentFile: File? = null
                    var output: java.io.BufferedOutputStream? = null

                    var startedAtMs = 0L
                    var startedElapsedMs = 0L
                    fun finishSegment() {
                        val file = currentFile ?: return
                        val stream = output
                        output = null
                        stream?.close()

                        currentFile = null
                        bytesInSegment = 0
                        segmentNumber++
                        onSegmentReady(file, startedAtMs, startedElapsedMs)
                    }

                    try {
                        recorder.startRecording()
                        startedAtMs = System.currentTimeMillis()
                        startedElapsedMs = android.os.SystemClock.elapsedRealtime()
                        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            "녹음을 시작하지 못했습니다."
                        }

                        while (recording) {
                            val count = recorder.read(
                                buffer, 0, buffer.size, AudioRecord.READ_BLOCKING
                            )
                            check(count >= 0) { "마이크 읽기 실패 ($count)" }

                            var offset = 0
                            while (offset < count) {
                                if (output == null) {
                                    val name = segmentNumber.toString().padStart(6, '0') + ".pcm"
                                    val file = File(sessionDir, name)
                                    output = file.outputStream().buffered()
                                    currentFile = file
                                }

                                val size = minOf(count - offset, segmentSize - bytesInSegment)
                                checkNotNull(output).write(buffer, offset, size)
                                offset += size
                                bytesInSegment += size

                                if (bytesInSegment == segmentSize) {
                                    finishSegment()
                                }
                            }
                        }

                        finishSegment() // 종료 버튼을 누른 시점에 남아 있는 마지막 구간
                        check(segmentNumber > 1) { "녹음된 오디오가 없습니다." }
                        sessionDir
                    } finally {
                        output?.close()
                    }
                } finally {
                    try {
                        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            recorder.stop()
                        }
                    } finally {
                        recorder.release()
                    }
                }
            }

            onFinished(result)
        }.also { it.start() }
    }

    fun stop() {
        recording = false
    }

    // MainActivity의 메인 스레드에서 완료 콜백을 처리할 때 호출합니다.
    fun resetAfterFinish() {
        worker = null
    }
}
