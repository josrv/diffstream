package com.josrv.diffstream.server.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler

class ServerInitializer(
        private val serverState: ServerState
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline()
                .addLast(HttpServerCodec())
                .addLast(HttpObjectAggregator(65536))
                .addLast(WebSocketServerProtocolHandler("/", null, true))
                .addLast(DiffHandler(serverState))
    }
}
