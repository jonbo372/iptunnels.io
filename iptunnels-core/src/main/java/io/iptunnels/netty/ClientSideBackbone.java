package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.HiPacket;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link ClientSideBackbone} is the link between the client and server through which
 * we will transport all the raw messages we are receiving at either end of
 * the {@link Tunnel}
 *
 * A {@link ClientSideBackbone} is always TCP based and will always be initiated by the client to establish a transport
 * between it and the server.
 */
public class ClientSideBackbone extends ChannelInboundHandlerAdapter implements TunnelBackbone {

    private final Channel channel;

    private final Random random = new Random(System.currentTimeMillis());

    private final ConcurrentMap<Integer, Tunnel> tunnels = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, CompletableFuture<HiPacket>> outstandingHelloTransactions = new ConcurrentHashMap<>();

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
        // System.err.println("TunnelBackbone: channelRegistered");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        // System.err.println("TunnelBackbone: channelUnregistered");
        closeTunnels();
    }

    public void manageTunnel(final Tunnel tunnel) {
        // currently we don't really use the tunnel id.
        tunnels.put(tunnel.getId(), tunnel);
        // final String url = tunnel.getLocalHost() + ":" + tunnel.getLocalPort();
        // channel.writeAndFlush(TunnelPacket.hi(tunnel.getId(), url));
    }

    public ClientSideBackbone(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        // System.err.println("ClientSdieBackbone: channelReadComplete");
        // ctx.flush();
        ctx.fireChannelReadComplete();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        // System.err.println("TunnelBackbone: pkt received. Looking up tunnel and disptaching");
        final TunnelPacket pkt = (TunnelPacket)msg;
        if (pkt.isPayload()) {
            final PayloadPacket payload = pkt.toPayload();
            final Tunnel tunnel = tunnels.get(payload.getTunnelId());
            // System.out.println("ClientSideBackbone: " + new String(payload.getBody()));
            tunnel.process(pkt);
        } else if (pkt.isHello()) {
            System.err.println("ClientSideBackbone: Why the fuck do I have a Hello pkt?");
        } else if (pkt.isHi()) {
            System.err.println("ClientSideBackbone: Processing a Hi packet " + pkt);
            final HiPacket hi = pkt.toHi();
            final CompletableFuture<HiPacket> f = outstandingHelloTransactions.remove(hi.transactionId());
            if (f == null) {
                System.err.println("ClientSideBackbone: WTF the Hello Transaction Future is gone");
            } else {
                f.complete(hi);
            }
            // TODO: this is wrong. I can't possibly have put the tunnel into the map before I know the freaking
            // tunnel id!!! Guess it must not matter.
            // final Tunnel tunnel = tunnels.get(pkt.toHi().tunnelId());
            // tunnel.process(pkt);
        }
    }

    /*
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
    */

    public CompletionStage<HiPacket> hello() {
        final CompletableFuture<HiPacket> future = new CompletableFuture<>();
        final int transactionId = random.nextInt();
        // System.err.println("ClientSideBackbone: hello transactionId " + transactionId);
        outstandingHelloTransactions.put(transactionId, future);
        channel.writeAndFlush(TunnelPacket.hello(transactionId));
        return future;
    }

    @Override
    public void tunnel(final PayloadPacket pkt) {
        if (pkt != null) {
            channel.writeAndFlush(pkt, channel.voidPromise());
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
