package io.iptunnels.server;

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
                processPayload(pkt.toPayload());
            } else if (pkt.isHello()) {
                processHello(ctx, pkt.toHello());
            }

        } catch (final ClassCastException e) {
            System.err.println("Decoder not doing its job?");
            ctx.close();
        }
    }

    private void processPayload(final PayloadPacket pkt) {
        System.err.println("Processing payload");
    }

    private void processHello(final ChannelHandlerContext ctx, final HelloPacket pkt) {
        final int tunnelId = random.nextInt();
        final String url = "127.0.0.1:8907";
        System.err.println("Sending hi back");
        ctx.writeAndFlush(TunnelPacket.hi(tunnelId, url), ctx.voidPromise());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
