package com.josrv.diffstream

import com.josrv.diffstream.client.Patcher
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException

class WebSocketClientHandler(
        private val handshaker: WebSocketClientHandshaker,
        private val patcher: Patcher
) : SimpleChannelInboundHandler<Any>() {

    var handshakeFuture: ChannelPromise? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        handshakeFuture = ctx.newPromise()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        handshaker.handshake(ctx.channel())
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any?) {
        val ch = ctx.channel()

        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(ch, msg as FullHttpResponse)
                println("WebSocket client connected")
                handshakeFuture?.setSuccess()
            } catch (e: WebSocketHandshakeException) {
                println("WebSocket Client failed to connect")
                handshakeFuture?.setFailure(e)
            }
            return
        }

        when (val frame = msg as WebSocketFrame) {
            is TextWebSocketFrame -> {
                patcher.applyPatch(frame.text())

                println("Received: ${frame.text()}")
            }
            is CloseWebSocketFrame -> {
                println("Received closing")
                ch.close()
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (handshakeFuture?.isDone == false) {
            handshakeFuture?.setFailure(cause)
        }

        ctx.close()
    }
}