package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.HiPacket;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(ClientSideBackbone.class);

    private final Channel channel;

    private final Random random = new Random(System.currentTimeMillis());

    private final ConcurrentMap<Integer, Tunnel> tunnels = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, CompletableFuture<HiPacket>> outstandingHelloTransactions = new ConcurrentHashMap<>();

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
        logger.info("Channel {} registered. Remote host {}", ctx.channel().id(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel {} unregistered. Remote host {}", ctx.channel().id(), ctx.channel().remoteAddress());
        closeTunnels();
    }

    public void manageTunnel(final Tunnel tunnel) {
        tunnels.put(tunnel.getId(), tunnel);
    }

    public ClientSideBackbone(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final TunnelPacket pkt = (TunnelPacket)msg;
        if (pkt.isPayload()) {
            final PayloadPacket payload = pkt.toPayload();
            final Tunnel tunnel = tunnels.get(payload.getTunnelId());
            tunnel.process(pkt);
        } else if (pkt.isHi()) {
            logger.debug("Processing a {}", pkt);
            final HiPacket hi = pkt.toHi();
            final CompletableFuture<HiPacket> f = outstandingHelloTransactions.remove(hi.transactionId());
            if (f == null) {
                logger.warn("Unable to locate any outstanding Hello transactions for transaction id {}", hi.transactionId());
            } else {
                f.complete(hi);
            }
        } else {
            logger.warn("Unexpected TunnelPacket received of type {}. Dropping.", pkt.getClass().getName());
        }
    }

    public CompletionStage<HiPacket> hello() {
        final CompletableFuture<HiPacket> future = new CompletableFuture<>();
        final int transactionId = random.nextInt();
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
