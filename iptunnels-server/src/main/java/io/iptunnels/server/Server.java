package io.iptunnels.server;

import io.iptunnels.Clock;
import io.iptunnels.config.ConfigurationEnvironment;
import io.iptunnels.config.ServerConfig;
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
        NettyBootstrap.initServerBootstrap(config);
    }

    public void run() throws InterruptedException {
        final ChannelFuture f = NettyBootstrap.getServerBootstrap().bind(config.getListenAddress()).sync();
        f.channel().closeFuture().sync();
    }

    public static void main(final String... args) throws Exception {
        final ConfigurationEnvironment<ServerConfig> env =
                ConfigurationEnvironment.of(ServerConfig.class)
                        .withProjectName("iptunnels")
                        .withConfigFile("server.yaml")
                        .build();
        new Server(env.loadConfig()).run();

    }
}
