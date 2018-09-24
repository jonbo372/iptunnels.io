package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.HelloPacket;
import io.iptunnels.proto.HiPacket;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.BindException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private final ConcurrentMap<Integer, Tunnel> tunnels = new ConcurrentHashMap<>();

    /**
     * The {@link HiPacket} from the server contains all the settings we need as far as tunnel id,
     * what UDP port is exposed on the other side etc.
     */
    private HiPacket tunnelConfig;

    /*
    public int getTunnelId() {
        if (tunnelConfig != null) {
            return tunnelConfig.tunnelId();
        }

        // TODO: do something better.
        return -1;
    }
    */

    public void disconnect() {
        channel.close();
    }

    public Channel getChannel() {
        return channel;
    }

    public void closeTunnels() {
        tunnels.forEach((channelId, tunnel) -> {
            tunnel.shutdown();
        });

    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("TunnelBackbone: channelRegistered");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("TunnelBackbone: channelUnregistered");
        closeTunnels();
    }

    public void manageTunnel(final Tunnel tunnel) {
        // currently we don't really use the tunnel id.
        tunnels.put(tunnel.getId(), tunnel);
        final String url = tunnel.getLocalHost() + ":" + tunnel.getLocalPort();
        channel.writeAndFlush(TunnelPacket.hi(tunnel.getId(), url));
    }

    public TunnelBackbone(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        // System.err.println("TunnelBackbone: pkt received. Looking up tunnel and disptaching");
        final TunnelPacket pkt = (TunnelPacket)msg;
        if (pkt.isPayload()) {
            final PayloadPacket payload = pkt.toPayload();
            final Tunnel tunnel = tunnels.get(payload.getTunnelId());

            // TODO: with multiple tunnels we will have to do the multiplexing here to the correct tunnel.
            // the pipelines won't do since we can't have all tunnels in the pipeline.
            // ctx.fireChannelRead(msg);
            tunnel.process(pkt);
        } else if (pkt.isHello()) {
            System.err.println("TunnelBackbone: Ok, need to take care of the Hello now");
            processHello(pkt.toHello());
        } else if (pkt.isHi()) {
            // actually we can get a HI packet because it is written
            // System.err.println("How the fuck can I get a HI packet?");
            // ctx.fireChannelRead(pkt);
            final Tunnel tunnel = tunnels.get(pkt.toHi().tunnelId());
            tunnel.process(pkt);
        }
    }

    private void processHello(final HelloPacket pkt) {
        // final String ip = "104.248.212.248";
        // final String ip = "10.46.0.5";
        final String ip = "10.36.10.27";
        Tunnel.of(this).bind(ip, 7890).thenAccept(tunnel -> {
            manageTunnel(tunnel);
        }).exceptionally(t -> {
            if (t.getCause() instanceof BindException) {
                System.err.println("Seems like we didn't clean up last time");
            } else {
                t.printStackTrace();
            }
            return null;
        });
    }

    public void hello() {
        // TODO: perhaps split the tunnel into a Client and Server Backbone since only the client would
        // be sending the Hello packet. It's a little bit confusing when this currently is both sides.
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
