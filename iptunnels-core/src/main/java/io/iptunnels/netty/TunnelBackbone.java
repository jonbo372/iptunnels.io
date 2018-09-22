package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * The {@link TunnelBackbone} is the link between the client and server through which
 * we will transport all the raw messages we are receiving at either end of
 * the {@link Tunnel}
 *
 * A {@link TunnelBackbone} is always TCP based and will always be initiated by the client to establish a transport
 * between it and the server.
 */
public class TunnelBackbone extends ChannelInboundHandlerAdapter {

    private final Channel channel;

    public TunnelBackbone(final Channel channel) {
        this.channel = channel;
    }


    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final TunnelPacket pkt = (TunnelPacket)msg;
        System.err.println("TunnelBackbone received: " + pkt);
    }


    public void tunnel(final PayloadPacket pkt) {
        channel.writeAndFlush(TunnelPacket.hello());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
