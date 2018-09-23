package io.iptunnels.client;

import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final TunnelPacket pkt = (TunnelPacket)msg;
        if (pkt.isHi()) {
            System.err.println("Server says hi!");
        } else {
            System.err.println("ClientHandler not handling packet: " + pkt);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
