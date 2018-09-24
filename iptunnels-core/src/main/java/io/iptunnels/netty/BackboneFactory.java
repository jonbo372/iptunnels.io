package io.iptunnels.netty;

import io.iptunnels.BackendInaccessibleException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * All {@link TunnelBackbone}s share the same Netty bootstrap etc so easiest way to share this
 * is through a typical factory style pattern.
 */
public class BackboneFactory extends ChannelInitializer {

    public static final BackboneFactory SINGLETON = new BackboneFactory();

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Bootstrap bootstrap = new Bootstrap();
    private final TunnelPacketStreamEncoder encoder = new TunnelPacketStreamEncoder();

    public BackboneFactory() {
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(this);
    }

    @Override
    protected void initChannel(final Channel ch) {
        final ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder", new TunnelPacketStreamDecoder());
        pipeline.addLast("encoder", encoder);
        pipeline.addLast("backbone", new ClientSideBackbone(ch));
    }

    public CompletionStage<ClientSideBackbone> connect(final SocketAddress remote) {
        final CompletableFuture<ClientSideBackbone> future = new CompletableFuture();
        final ChannelFuture cf = bootstrap.connect(remote);
        cf.addListener(f -> {
            if (f.isSuccess()) {
                final Channel channel = cf.channel();
                // final ClientSideBackbone backbone = new ClientSideBackbone(channel);
                // channel.pipeline().addLast("backbone", backbone);
                future.complete((ClientSideBackbone)channel.pipeline().get("backbone"));
            } else {
                future.completeExceptionally(new BackendInaccessibleException(remote, "Unable to connect to iptunnels server"));
            }
        });

        return future;

    }
}
