package com.josrv.diffstream.client

import com.josrv.diffstream.WebSocketClientHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator

class ClientInitializer(val handler: WebSocketClientHandler) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val p = ch.pipeline()
        p.addLast(
                HttpClientCodec(),
                HttpObjectAggregator(8192),
                handler)
    }
}