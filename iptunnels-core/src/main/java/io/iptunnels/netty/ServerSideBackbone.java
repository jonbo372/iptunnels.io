package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.HelloPacket;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.BindException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link ServerSideBackbone} is the link between the client and server through which
 * we will transport all the raw messages we are receiving at either end of
 * the {@link Tunnel}
 *
 * A {@link ServerSideBackbone} is always TCP based and will always be initiated by the client to establish a transport
 * between it and the server.
 */
public class ServerSideBackbone extends ChannelInboundHandlerAdapter implements TunnelBackbone {

    private final Channel channel;

    private final ConcurrentMap<Integer, Tunnel> tunnels = new ConcurrentHashMap<>();

    @Override
    public void disconnect() {
        channel.close();
    }

    public void closeTunnels() {
        tunnels.forEach((channelId, tunnel) -> {
            tunnel.shutdown();
        });

    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("ServerSideBackbone: channelRegistered");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("ServerSideBackbone: channelUnregistered");
        closeTunnels();
    }


    public ServerSideBackbone(final Channel channel) {
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
            System.err.println("ServerSideBackbone: Ok, need to take care of the Hello now");
            processHello(ctx, pkt.toHello());
        } else if (pkt.isHi()) {
            System.err.println("ServerSideBackbone: Why the fuck to I have a HI packet");
            // actually we can get a HI packet because it is written
            // System.err.println("How the fuck can I get a HI packet?");
            // ctx.fireChannelRead(pkt);
            final Tunnel tunnel = tunnels.get(pkt.toHi().tunnelId());
            tunnel.process(pkt);
        }
    }

    private void processHello(final ChannelHandlerContext ctx, final HelloPacket pkt) {
        // final String ip = "104.248.212.248";
        // final String ip = "10.46.0.5";
        final String ip = "10.36.10.27";
        Tunnel.of(this).bind(ip, 7890).thenAccept(tunnel -> {
            System.err.println("ServerSideBackbone: my tunnel is up");
            tunnels.put(tunnel.getId(), tunnel);
            final String url = tunnel.getLocalHost() + ":" + tunnel.getLocalPort();
            ctx.writeAndFlush(TunnelPacket.hi(pkt.transactionId(), tunnel.getId(), url));
        }).exceptionally(t -> {
            if (t.getCause() instanceof BindException) {
                System.err.println("Seems like we didn't clean up last time");
            } else {
                t.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void tunnel(final PayloadPacket pkt) {
        if (pkt != null) {
            System.out.println("ServerSideBackbone: " + new String(pkt.getBody()));
            channel.writeAndFlush(pkt);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
