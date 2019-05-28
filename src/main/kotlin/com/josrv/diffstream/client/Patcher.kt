package com.josrv.diffstream.client

import com.josrv.diffstream.Diff
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

const val PATCH_QUEUE_SIZE = 1024

class Patcher(
        val file: Path
) {
    //TODO use PriorityBlockingQueue sorted by diff sequence number
    private val patchQueue = ArrayBlockingQueue<Diff>(PATCH_QUEUE_SIZE)
    private val running = AtomicBoolean(false)

    fun applyPatch(patch: String) {
        patchQueue.put(patch)
    }

    fun start() {
        running.set(true)
        while (running.get()) {
            val patch = patchQueue.take()

            val patchProcess = ProcessBuilder("patch", file.toString())
                    .start()

            val writer = BufferedWriter(OutputStreamWriter(patchProcess.outputStream))
            writer.write(patch)
            writer.newLine()
            writer.close()
        }
    }

    fun stop() {
        running.set(false)
    }
}