/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.josrv.diffstream

import com.josrv.diffstream.client.Patcher
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

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
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        val p = ch.pipeline()
                        p.addLast(
                                HttpClientCodec(),
                                HttpObjectAggregator(8192),
//                                    WebSocketClientCompressionHandler.INSTANCE,
                                handler)
                    }
                })

        val ch = b.connect(host, port).sync().channel()
        handler.handshakeFuture?.sync()


        val filePath = Paths.get(file).toAbsolutePath()
        if (role.equals("host", true)) {
            val diffQueue = LinkedBlockingQueue<String>()

            thread(start = true, name = "diffSender") {

                while (true) {
                    val diff = diffQueue.poll(1, TimeUnit.DAYS)

                    val frame = TextWebSocketFrame(diff)
                    ch.writeAndFlush(frame)
                }
            }

            val diffStream = ProcessBuilder("./dyn-diff.sh", filePath.toString())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .inputStream

            while (true) {
                if (diffStream.available() > 0) {
                    val diff = String(diffStream.readNBytes(diffStream.available()))
                            .trim('\n')
                    diffQueue.put(diff)
                }
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
