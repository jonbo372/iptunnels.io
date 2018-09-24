package io.iptunnels.server;

import io.netty.channel.ChannelInboundHandlerAdapter;


public class ServerHandler extends ChannelInboundHandlerAdapter {


    // TOOD: I think we may actually need to keep track of the backbones and the tunnels separate.
    // The backbone has the TCP channel always but in theory we can have multiple tunnels using the
    // same backbone. Each of those tunnels will bind to an external port and as such, will get their
    // own channel where we can store the actual Tunnel behind.
    // this would also mean that if we did this then we cannot use the channel.pipeline as the
    // dispatch mechanism anymore. We would need this server handler to do the multiplexing...
    //
    // For now the key in this map is the actual id of the backbone channel
    // private final ConcurrentMap<ChannelId, TunnelBackbone> backbones = new ConcurrentHashMap<>();

    /*
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        System.err.println("ServerHandler: I shouldn't get any packets anymore");
        try {
            final TunnelPacket pkt = (TunnelPacket)msg;
            if (pkt.isPayload()) {
                // System.err.println("ServerHandler: Processing payload - forwarding up the chain");
                ctx.fireChannelRead(pkt);
            } else if (pkt.isHello()) {
                // TODO: why not just, again, get rid of the ServerHandler and have all go
                // to TunnelBackbone instead?
                final TunnelBackbone backbone = backbones.get(ctx.channel().id());
                processHello(backbone, pkt.toHello());
            }

        } catch (final ClassCastException e) {
            System.err.println("Decoder not doing its job?");
            ctx.close();
        }
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        // TODO: should probably initiate a new state machine here if we were to do this for real.
        // System.err.println("New channel: " + ctx.channel().localAddress() + " Remote: " + ctx.channel().remoteAddress());

        // TODO: perhaps we can add the backbone as the pipeline handler and remove the ServerHandler
        // here. Perhaps even in the earlier call before the channel is registered.
        final TunnelBackbone backbone = new TunnelBackbone(ctx.channel());
        backbones.put(ctx.channel().id(), backbone);
        ctx.channel().pipeline().addLast("backbone", backbone);
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        final TunnelBackbone backbone = backbones.get(ctx.channel().id());
        if (backbone == null) {
            System.err.println("WTF - backbone was null when we are trying to unregister the channel");
            return;
        }

        backbone.closeTunnels();
    }

    private void processHello(final TunnelBackbone backbone, final HelloPacket pkt) {
        // final String ip = "104.248.212.248";
        // final String ip = "10.46.0.5";
        final String ip = "10.36.10.27";
        Tunnel.of(backbone).bind(ip, 7890).thenAccept(tunnel -> {
            backbone.manageTunnel(tunnel);
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
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        System.err.println("ChannelHandler caught an exception");
        cause.printStackTrace();
        ctx.close();
    }
    */
}
