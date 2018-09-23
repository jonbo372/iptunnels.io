package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.HiPacket;
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

    /**
     * The {@link HiPacket} from the server contains all the settings we need as far as tunnel id,
     * what UDP port is exposed on the other side etc.
     */
    private HiPacket tunnelConfig;

    /**
     * This is the tunnel that we will relay data through. I.e., when we (i.e. the {@link TunnelBackbone}) receives
     * a {@link PayloadPacket} we will push that to the remote address represented by the {@link Tunnel}
     */
    private Tunnel tunnel;

    public int getTunnelId() {
        if (tunnelConfig != null) {
            return tunnelConfig.tunnelId();
        }

        // TODO: do something better.
        return -1;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setTunnel(final Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public TunnelBackbone(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final TunnelPacket pkt = (TunnelPacket)msg;
        if (pkt.isPayload()) {
            ctx.fireChannelRead(msg);
        } else if (pkt.isHi()) {
            // System.err.println("Backbone: consuming the HI packet");
            tunnelConfig = pkt.toHi();
            // System.err.println("our remote breakout address is " + tunnelConfig.breakoutAddress());
        }
    }

    public void hello() {
        channel.writeAndFlush(TunnelPacket.hello());
    }

    public void tunnel(final PayloadPacket pkt) {
        if (pkt != null) {
            channel.writeAndFlush(pkt);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
