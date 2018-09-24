package io.iptunnels.netty;

import io.iptunnels.Transport;
import io.iptunnels.Tunnel;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

@ChannelHandler.Sharable
public class NettyUdpTunnel extends ChannelInboundHandlerAdapter implements Tunnel {

    /**
     * Our local UDP channel, which we will use when we send messages to the target.
     * This is also where we will be receiving messages.
     */
    private final Channel channel;

    /**
     * Our backbone, which is just a TCP/TLS connection to the server over which we will
     * tunnel all raw payload from the UDP messages we receive.
     */
    private final TunnelBackbone backbone;

    private final int tunnelId;

    /**
     * The target to which we will relay all data we receive over the backbone.
     */
    private final AtomicReference<InetSocketAddress> remoteTarget = new AtomicReference<>();

    /**
     * The local address we're listening for UDP on.
     */
    private final InetSocketAddress localUdpAddress;

    /**
     * This is the the so-called breakout address we have on the "other side" of the tunnel.
     * If this {@link NettyUdpTunnel} is the one "on the other side" then it will be the same as
     * the localUdpAddress.
     */
    private final InetSocketAddress breakoutAddress;

    private NettyUdpTunnel(final Channel channel, final int tunnelId, final TunnelBackbone backbone, final InetSocketAddress remoteTarget, final InetSocketAddress breakoutAddress) {
        this.channel = channel;
        this.tunnelId = tunnelId;
        this.backbone = backbone;
        if (remoteTarget != null) {
            this.remoteTarget.set(remoteTarget);
        }

        localUdpAddress = (InetSocketAddress)channel.localAddress();
        this.breakoutAddress = breakoutAddress;
    }

    /**
     * Update the remote target if we are allowed.
     *
     * TODO: create some rules around this.
     *
     * @param remoteTarget
     */
    public void setRemoteTarget(final InetSocketAddress remoteTarget) {
        this.remoteTarget.set(remoteTarget);
    }

    @Override
    public int getId() {
        return tunnelId;
    }

    @Override
    public Transport getTransport() {
        return Transport.UDP;
    }

    @Override
    public int getLocalPort() {
        return localUdpAddress.getPort();
    }

    @Override
    public String getLocalHost() {
        return localUdpAddress.getHostString();
    }

    @Override
    public InetSocketAddress getTunnelAddress() {
        return breakoutAddress;
    }

    @Override
    public InetSocketAddress getTargetAddress() {
        return remoteTarget.get();
    }

    @Override
    public void shutdown() {
        channel.close().addListener(f -> {
            if (!f.isSuccess()) {
                // TODO: log and deal with it using real loggers.
                System.err.println("Unable to shut down tunnel. Ports may linger");
            }
        });
    }

    /**
     * Could implement firewall filtering or whatever.
     *
     * @param remote
     * @return
     */
    private boolean accept(final InetSocketAddress remote) {
        return true;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof TunnelPacket) {
            System.err.println("NettyUdpTunnel: I shouldn't receive any tunnel packets through the backbone netty pipeline anymore");
            // process((TunnelPacket)msg);
        } else if (msg instanceof DatagramPacket) {
            processUdpPacket(ctx, (DatagramPacket)msg);
        } else {
            // TODO: log or something.
            // unknown packet, dropping...
            System.err.println("[ERROR] NettyUdpTunnel Uknown packet type received");
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        // ctx.flush();
        backbone.flushTunnel();
        ctx.fireChannelReadComplete();
        // System.err.println("NettyUdpTunnel: channelReadComplete");
    }

    private void processUdpPacket(final ChannelHandlerContext ctx, final DatagramPacket udp) {
        final InetSocketAddress sender = udp.sender();
        if (accept(sender)) {
            if (!sender.equals(remoteTarget.get())) {
                remoteTarget.set(sender);
            }

            final ByteBuf content = udp.content();
            final byte[] rawData = new byte[content.readableBytes()];
            content.getBytes(0, rawData);
            // final PayloadPacket pkt = TunnelPacket.payload(backbone.getTunnelId(), rawData);
            // System.out.println(new String(rawData));

            final PayloadPacket pkt = TunnelPacket.payload(tunnelId, rawData);
            backbone.tunnel(pkt);
        }
    }

    @Override
    public void process(final TunnelPacket pkt) {
        final InetSocketAddress target = remoteTarget.get();
        if (pkt.isPayload() && target != null) {
            final ByteBuf data = toByteBuf(channel, pkt.toPayload().getBody());
            final DatagramPacket udp = new DatagramPacket(data, target);
            final ChannelPromise p = channel.newPromise();
            p.addListener(f -> {

            });
            channel.writeAndFlush(udp, p);
            // channel.writeAndFlush(udp);
        } else if (pkt.isHi()) {
            System.err.println("NettyUdpTunnel: Why am I getting a HI message still?");
        }
    }

    public static ByteBuf toByteBuf(final Channel channel, final byte[] data) {
        final ByteBuf buffer = channel.alloc().buffer(data.length, data.length);
        buffer.writeBytes(data);
        return buffer;
    }

    public static BackboneBuildStep withTunnelId(final int id) {
        return backbone -> new Builder(id, backbone);
    }

    public interface BackboneBuildStep {
        Builder withBackbone(TunnelBackbone backbone);
    }

    public static class Builder {

        private final TunnelBackbone backbone;
        private Bootstrap bootstrap;
        private InetSocketAddress remoteTarget;
        private InetSocketAddress breakoutAddress;
        private final int tunnelId;

        private Builder(final int tunnelId, final TunnelBackbone backbone) {
            this.tunnelId = tunnelId;
            this.backbone = backbone;
        }

        /**
         * Specify the remote address to where we will be relaying everything that comes across this tunnel.
         * If the remote address isn't specified, it will be set to the remote address of the first packet
         * we receive. If we are asked to relay something to remote host before we have this address, it will obviously
         * be dropped.
         *
         * @param port
         * @param host
         * @return
         */
        public Builder withTargetAddress(final int port, final String host) {
            remoteTarget = new InetSocketAddress(host, port);
            return this;
        }

        public Builder withTargetAddress(final InetSocketAddress remoteTarget) {
            this.remoteTarget = remoteTarget;
            return this;
        }

        public Builder withBreakoutAddress(final InetSocketAddress breakoutAddress) {
            this.breakoutAddress = breakoutAddress;
            return this;
        }

        public Builder withUdpBootstrap(final Bootstrap bootstrap) {
            this.bootstrap = bootstrap;
            return this;
        }

        public CompletionStage<Tunnel> start() {
            return start(0);
        }

        public CompletionStage<Tunnel> start(final int port) {
            return start(new InetSocketAddress(port));
        }

        public CompletionStage<Tunnel> start(final InetSocketAddress localAddress) {
            final CompletableFuture<Tunnel> future = new CompletableFuture<>();
            bootstrap.bind(localAddress).addListener(f -> {
                final ChannelFuture channelFuture = (ChannelFuture)f;
                if (channelFuture.isSuccess()) {
                    final Channel channel = channelFuture.channel();
                    final NettyUdpTunnel tunnel = new NettyUdpTunnel(channel, tunnelId, backbone, remoteTarget, breakoutAddress);
                    channel.pipeline().addLast("tunnel", tunnel);

                    // note that we have two different channels. This is the TCP channel going to
                    // the server and we want to be part of that pipeline as well.
                    // TODO: the backbone should do this as part of its "manage" functionality
                    // backbone.getChannel().pipeline().addLast("tunnel", tunnel);
                    future.complete(tunnel);
                } else {
                    future.completeExceptionally(channelFuture.cause());
                }
            });
            return future;
        }
    }
}
