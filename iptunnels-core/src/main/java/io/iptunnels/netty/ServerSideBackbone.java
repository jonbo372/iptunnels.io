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
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        System.err.println("ServerSideBackbone: channelWriteabilityChanged");
        ctx.fireChannelWritabilityChanged();
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
            processHello(ctx, pkt.toHello());
        } else if (pkt.isHi()) {
            System.err.println("ServerSideBackbone: Why the fuck to I have a HI packet");
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
        ctx.flush();
    }

    private void processHello(final ChannelHandlerContext ctx, final HelloPacket pkt) {
        final String ip = "104.248.212.248";
        // final String ip = "10.46.0.5";
        // final String ip = "10.36.10.27";
        // final String ip = "127.0.0.1";
        final int port = 5683;
        Tunnel.of(this).bind(ip, port).thenAccept(tunnel -> {
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

    private static void sleep(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void tunnel(final PayloadPacket pkt) {
        if (pkt != null) {
            // System.out.println("ServerSideBackbone: Tunneling " + new String(pkt.getBody()));
            if (!channel.isWritable()) {
                System.err.println("Turns out the channel isn't writable at this point in time!");
            }
            channel.writeAndFlush(pkt, channel.voidPromise());
            // channel.write(pkt, channel.voidPromise());
        }
    }

    @Override
    public void flushTunnel() {
        channel.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
