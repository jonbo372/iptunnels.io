package io.iptunnels.server;

import io.iptunnels.Tunnel;
import io.iptunnels.netty.NettyUdpTunnel;
import io.iptunnels.netty.TunnelBackbone;
import io.iptunnels.proto.HelloPacket;
import io.iptunnels.proto.PayloadPacket;
import io.iptunnels.proto.TunnelPacket;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Random;


@Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        try {
            final TunnelPacket pkt = (TunnelPacket)msg;
            if (pkt.isPayload()) {
                processPayload(ctx, pkt.toPayload());
            } else if (pkt.isHello()) {
                processHello(ctx, pkt.toHello());
            }

        } catch (final ClassCastException e) {
            System.err.println("Decoder not doing its job?");
            ctx.close();
        }
    }

    private void processPayload(final ChannelHandlerContext ctx, final PayloadPacket pkt) {
        System.err.println("Processing payload - forwarding up the chain");
        // System.err.println(new String(pkt.getBody()));
        ctx.fireChannelRead(pkt);
    }

    private void processHello(final ChannelHandlerContext ctx, final HelloPacket pkt) {
        final int tunnelId = random.nextInt();
        // Ah, this is how we will do it. Now we will remove the ServerHandler from the pipeline
        // and have the tunnelbackbone as the handler going forward. So this server handler
        // is really the initiator.
        final TunnelBackbone backbone = new TunnelBackbone(ctx.channel());

        Tunnel.of(backbone).bind("10.36.10.27", 7890).thenAccept(tunnel -> {
            final String url = tunnel.getLocalHost() + ":" + tunnel.getLocalPort();
            System.err.println("Sending hi back");

            // add the tunnel to the pipeline of the backbone...
            ctx.writeAndFlush(TunnelPacket.hi(tunnelId, url), ctx.voidPromise());
            backbone.getChannel().pipeline().addLast("tunnel", (NettyUdpTunnel)tunnel);
        });
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
