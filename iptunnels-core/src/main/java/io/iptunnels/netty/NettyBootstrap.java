package io.iptunnels.netty;

import io.iptunnels.config.ServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyBootstrap {

    private static final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static final Bootstrap bootstrap = createUDPBootstrap();

    private static ServerBootstrap serverBootstrap;

    private static final TunnelPacketStreamEncoder encoder = new TunnelPacketStreamEncoder();

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }

    public static synchronized ServerBootstrap getServerBootstrap() {
        if (serverBootstrap != null) {
            return serverBootstrap;
        }
        throw new NullPointerException("The ServerBootstrap has not been initialized just yet");
    }

    public static synchronized void initServerBootstrap(final ServerConfig config) {
        if (serverBootstrap != null) {
            return;
        }

        serverBootstrap = createTcpBootstrap(config);
    }

    public static ServerBootstrap createTcpBootstrap(final ServerConfig config) {
        final ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new TunnelPacketStreamDecoder());
                        pipeline.addLast("encoder", encoder);
                        pipeline.addLast("backbone", new ServerSideBackbone(config, ch));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        return b;
    }

    private static Bootstrap createUDPBootstrap() {
        final Bootstrap b = new Bootstrap();
        b.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(final DatagramChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                    }
                });
        return b;
    }
}
