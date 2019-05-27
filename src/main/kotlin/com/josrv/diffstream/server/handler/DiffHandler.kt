package com.josrv.diffstream.server.handler

import com.josrv.diffstream.ServerState
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler

class DiffHandler(
        private val serverState: ServerState
) : SimpleChannelInboundHandler<TextWebSocketFrame>() {

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        if (evt is WebSocketServerProtocolHandler.HandshakeComplete) {
            synchronized(serverState) {
                if (serverState.hostChannel == null || !serverState.hostChannel!!.isActive) {
                    serverState.hostChannel = ctx.channel()
                    println("Host connected")
                } else {
                    serverState.viewersChannelGroup.add(ctx.channel())
                    println("Viewer ${ctx.channel().id()} connected")
                }
            }

        } else {
            super.userEventTriggered(ctx, evt)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (ctx.channel().id() == serverState.hostChannel?.id()) {
            println("Host disconnected")
        } else {
            println("Viewer ${ctx.channel().id()} disconnected")
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
        serverState.viewersChannelGroup.writeAndFlush(msg.retain())
    }
}