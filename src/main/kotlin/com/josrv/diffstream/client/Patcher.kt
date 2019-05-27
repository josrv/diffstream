package com.josrv.diffstream.client

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class Patcher(
        val file: Path
) {
    //TODO use PriorityBlockingQueue sorted by diff sequence number
    private val patchQueue = LinkedBlockingQueue<String>()
    private var running = false

    fun applyPatch(patch: String) {
        patchQueue.add(patch)
    }

    fun start() {
        running = true
        while (running) {
            val patch = patchQueue.poll(100, TimeUnit.DAYS)

            val patchProcess = ProcessBuilder("patch", file.toString())
                    .start()

            val writer = BufferedWriter(OutputStreamWriter(patchProcess.outputStream))
            writer.write(patch)
            writer.newLine()
            writer.close()
        }
    }

    fun stop() {
        running = false
    }
}