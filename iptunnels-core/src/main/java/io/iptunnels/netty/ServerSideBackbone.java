package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.config.PublicTunnelConfig;
import io.iptunnels.config.ServerConfig;
import io.iptunnels.proto.HelloPacket;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.util.Random;
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

    private static final Logger logger = LoggerFactory.getLogger(ServerSideBackbone.class);

    private final ServerConfig config;

    private final Channel channel;

    private final ConcurrentMap<Integer, Tunnel> tunnels = new ConcurrentHashMap<>();

    private final Random random = new Random(System.currentTimeMillis());

    public ServerSideBackbone(final ServerConfig config, final Channel channel) {
        this.config = config;
        this.channel = channel;
    }

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
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        logger.debug("Channel changed writability");
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel {} unregistered. Remote host {}", ctx.channel().id(), ctx.channel().remoteAddress());
        closeTunnels();
    }


    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final TunnelPacket pkt = (TunnelPacket)msg;
        if (pkt.isPayload()) {
            final PayloadPacket payload = pkt.toPayload();
            tunnels.get(payload.getTunnelId()).process(pkt);
        } else if (pkt.isHello()) {
            processHello(ctx, pkt.toHello());
        } else {
            logger.warn("Unexpected packet received {}", pkt);
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
        ctx.flush();
    }

    private void processHello(final ChannelHandlerContext ctx, final HelloPacket pkt) {

        // always honor the port in the hello packet first and foremost, if configured
        // to do so...
        final PublicTunnelConfig tunnelConfig = config.getTunnelConfig();
        final int port;
        if (tunnelConfig.isUseClientSuggestedPort() && pkt.port() > 0) {
            port = pkt.port();
        } else if (config.getTunnelConfig().isUsePortRange()) {
            final int start = tunnelConfig.getStartPortRange();
            final int stop = tunnelConfig.getStopPortRange();
            port = random.nextInt(stop - start) + start;
        } else {
            port = tunnelConfig.getFixedPort();
        }

        final String ip = tunnelConfig.getIp();

        Tunnel.of(this).bind(ip, port).thenAccept(tunnel -> {
            tunnels.put(tunnel.getId(), tunnel);
            final String url = tunnel.getLocalHost() + ":" + tunnel.getLocalPort();
            ctx.writeAndFlush(TunnelPacket.hi(pkt.transactionId(), tunnel.getId(), url));
        }).exceptionally(t -> {
            if (t.getCause() instanceof BindException) {
                logger.warn("Unable to bind to {}:{}", ip == null ? "<any>" : ip, port);
            } else {
                logger.warn("Unable to bind to {}:{} due to unknown exception {}", ip == null ? "<any>" : ip, port, t);
            }
            return null;
        });
    }

    @Override
    public void tunnel(final PayloadPacket pkt) {
        if (pkt != null) {
            if (!channel.isWritable()) {
                logger.warn("The channel {} is not writable, packet is being dropped");
            }
            channel.writeAndFlush(pkt, channel.voidPromise());
        }
    }

    @Override
    public void flushTunnel() {
        channel.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        logger.warn("Unhandled exception caught by Netty handlers. Closing connection.", cause);
        ctx.close();
    }

}
