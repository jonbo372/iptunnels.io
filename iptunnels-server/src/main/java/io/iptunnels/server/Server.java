package io.iptunnels.server;

import io.iptunnels.Clock;
import io.iptunnels.config.ConfigurationEnvironment;
import io.iptunnels.netty.NettyBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

public class Server {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventLoopGroup udpGroup;
    private Clock clock;

    private final ServerConfig config;

    public Server(final ServerConfig config) {
        this.config = config;
    }

    public void run() throws InterruptedException {
        // final EventLoopGroup bossGroup = new NioEventLoopGroup();

        // TODO: let's share these two event loops with the UDP pool as well.
        // final EventLoopGroup workerGroup = new NioEventLoopGroup();

        // final ServerHandler handler = new ServerHandler();

        final ChannelFuture f = NettyBootstrap.getServerBootstrap().bind(config.getListenAddress()).sync();
        f.channel().closeFuture().sync();

        /*
        final TunnelPacketStreamEncoder encoder = new TunnelPacketStreamEncoder();
        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("decoder", new TunnelPacketStreamDecoder());
                            pipeline.addLast("encoder", encoder);
                            // pipeline.addLast("init", handler);
                            pipeline.addLast("backbone", new ServerSideBackbone(ch));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            final ChannelFuture f = b.bind(config.getListenAddress()).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
                    */
    }

    public static void main(final String... args) throws Exception {
        final ConfigurationEnvironment<ServerConfig> env = ConfigurationEnvironment.of(ServerConfig.class).withProjectName("iptunnels_server").build();
        new Server(env.loadConfig()).run();

    }
}
