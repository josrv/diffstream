package com.josrv.diffstream

import com.josrv.diffstream.server.handler.ServerInitializer
import com.josrv.diffstream.server.handler.ServerState
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.ImmediateEventExecutor
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

typealias Diff = String

const val STREAM_DIFF_SIZE = 1024

class Server {
    private val group = NioEventLoopGroup()
    private var channel: Channel? = null
    private val serverState = ServerState(
            hostChannel = null,
            viewersChannelGroup = DefaultChannelGroup(ImmediateEventExecutor.INSTANCE),
            stream = ArrayBlockingQueue<Diff>(STREAM_DIFF_SIZE)
    )

    fun start(address: InetSocketAddress): ChannelFuture {
        val bootstrap = ServerBootstrap()

        bootstrap.group(group)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(ServerInitializer(serverState))
        val future = bootstrap.bind(address)
        future.syncUninterruptibly()
        channel = future.channel()
        return future
    }

    fun destroy() {
        channel?.close()

        serverState.viewersChannelGroup.close()
        serverState.hostChannel?.close()
        group.shutdownGracefully()
    }
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Please give port as argument.")
        System.exit(1)
    }

    val port = args[0].toInt()
    val endpoint = Server()
    val future = endpoint.start(InetSocketAddress(port))

    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        endpoint.destroy()
    })

    future.channel().closeFuture().syncUninterruptibly()
}

