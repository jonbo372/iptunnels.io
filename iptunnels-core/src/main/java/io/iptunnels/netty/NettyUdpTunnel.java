package io.iptunnels.netty;

import io.iptunnels.Transport;
import io.iptunnels.Tunnel;
import io.iptunnels.TunnelIdentifier;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

    /**
     * The target to which we will relay all data we receive over the backbone.
     */
    private InetSocketAddress remoteTarget;

    /**
     * The local address we're listening for UDP on.
     */
    private final InetSocketAddress localUdpAddress;

    private NettyUdpTunnel(final Channel channel, final TunnelBackbone backbone, final InetSocketAddress remoteTarget) {
        this.channel = channel;
        this.backbone = backbone;
        this.remoteTarget = remoteTarget;

        localUdpAddress = (InetSocketAddress)channel.localAddress();
    }

    /**
     * Update the remote target if we are allowed.
     *
     * TODO: create some rules around this.
     *
     * @param remoteTarget
     */
    public void setRemoteTarget(final InetSocketAddress remoteTarget) {
        this.remoteTarget = remoteTarget;
    }

    @Override
    public TunnelIdentifier getId() {
        return null;
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
            processTunnelPacket(ctx, (TunnelPacket)msg);
        } else if (msg instanceof DatagramPacket) {
            processUdpPacket(ctx, (DatagramPacket)msg);
        } else {
            // TODO: log or something.
            // unknown packet, dropping...
        }

    }

    private void processUdpPacket(final ChannelHandlerContext ctx, final DatagramPacket udp) {
        final InetSocketAddress sender = udp.sender();
        if (accept(sender)) {
            if (remoteTarget != null) {
                remoteTarget = sender;
            }

            final ByteBuf content = udp.content();
            final byte[] rawData = new byte[content.readableBytes()];
            content.getBytes(0, rawData);
            final PayloadPacket pkt = TunnelPacket.payload(backbone.getTunnelId(), rawData);
            backbone.tunnel(pkt);
        }
    }

    private void processTunnelPacket(final ChannelHandlerContext ctx, final TunnelPacket pkt) {
        if (pkt.isPayload() && remoteTarget != null) {
            final ByteBuf data = toByteBuf(channel, pkt.toPayload().getBody());
            final DatagramPacket udp = new DatagramPacket(data, remoteTarget);
            channel.writeAndFlush(udp);
        }
    }

    public static ByteBuf toByteBuf(final Channel channel, final byte[] data) {
        final ByteBuf buffer = channel.alloc().buffer(data.length, data.length);
        buffer.writeBytes(data);
        return buffer;
    }

    public static Builder withBackbone(final TunnelBackbone backbone) {
        return new Builder(backbone);
    }

    public static class Builder {

        private final TunnelBackbone backbone;
        private Bootstrap bootstrap;
        private InetSocketAddress remoteTarget;

        private Builder(final TunnelBackbone backbone) {
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
                    // System.err.println("My local UDP address is: " + channel.localAddress());
                    final NettyUdpTunnel tunnel = new NettyUdpTunnel(channel, backbone, remoteTarget);
                    channel.pipeline().addLast("tunnel", tunnel);

                    // note that we have two different channels. This is the TCP channel going to
                    // the server and we want to be part of that pipeline as well.
                    backbone.getChannel().pipeline().addLast("tunnel", tunnel);
                    future.complete(tunnel);
                } else {
                    System.err.println("Unable to bind I guess...");
                }
            });
            return future;
        }
    }
}
