package com.josrv.diffstream.server.handler

import com.josrv.diffstream.Diff
import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import java.util.Queue

data class ServerState(
        var hostChannel: Channel? = null,
        val viewersChannelGroup: ChannelGroup,
        val stream: Queue<Diff>
)
