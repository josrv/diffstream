package com.josrv.diffstream

import com.josrv.diffstream.client.ClientInitializer
import com.josrv.diffstream.client.Patcher
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

const val DIFF_QUEUE_SIZE = 1024

fun main(args: Array<String>) {
    val host = args[0]
    val port = args[1].toInt()
    val role = args[2]
    val file = args[3]
    val uri = URI("ws://$host:$port/")

    val group = NioEventLoopGroup()
    try {
        val patcher = Patcher(Paths.get(file))
        val handler = WebSocketClientHandler(
                handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, true, DefaultHttpHeaders()),
                patcher = patcher)

        val b = Bootstrap()
        b.group(group)
                .channel(NioSocketChannel::class.java)
                .handler(ClientInitializer(handler))

        val ch = b.connect(host, port).sync().channel()
        handler.handshakeFuture?.sync()

        val filePath = Paths.get(file).toAbsolutePath()
        if (role.equals("host", true)) {
            val diffQueue = ArrayBlockingQueue<String>(DIFF_QUEUE_SIZE)

            thread(start = true, name = "diffSender") {
                // TODO extract
                while (true) {
                    val diff = diffQueue.take()

                    val frame = TextWebSocketFrame(diff)
                    ch.writeAndFlush(frame)
                }
            }

            val diffStream = ProcessBuilder("./dyn-diff.sh", filePath.toString())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .inputStream

            while (true) {
                // TODO extract
                if (diffStream.available() > 0) {
                    val diff = String(diffStream.readNBytes(diffStream.available()))
                            .trim('\n')
                    diffQueue.put(diff)
                }
                //TODO refactor this
                Thread.sleep(10L)
            }
        } else if (role.equals("viewer", ignoreCase = true)) {
            patcher.start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        group.shutdownGracefully()
    }
}
