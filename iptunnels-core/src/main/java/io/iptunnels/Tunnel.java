package io.iptunnels;

import io.iptunnels.netty.BackboneFactory;
import io.iptunnels.netty.NettyUdpTunnel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.CompletionStage;

/**
 * Represents a tunnel
 */
public interface Tunnel {

    static Builder udp(final String serverAddress, final int serverPort) {
        return new Builder(Transport.UDP, serverAddress, serverPort);
    }

    TunnelIdentifier getId();

    /**
     * This is the transport that is exposed by the {@link Tunnel}.
     *
     * @return
     */
    Transport getTransport();

    class Builder {

        private final Transport transport;
        private final String serverAddress;
        private final int serverPort;

        // should be injected.
        private static final EventLoopGroup udpGroup = new NioEventLoopGroup();
        private static final Bootstrap bootstrap = createUDPBootstrap();

        // whatever...
        private final BackboneFactory factory = BackboneFactory.SINGLETON;

        private Builder(final Transport transport, final String serverAddress, final int serverPort) {
            this.transport = transport;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        private static Bootstrap createUDPBootstrap() {
            final Bootstrap b = new Bootstrap();
            b.group(udpGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(final DatagramChannel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                                /*
                                pipeline.addLast("decoder", new SipMessageDatagramDecoder(clock, vipAddress));
                                pipeline.addLast("encoder", new SipMessageDatagramEncoder());
                                for (int i = 0; i < handlers.size(); ++i) {
                                    pipeline.addLast(handlerNames.get(i), handlers.get(i));
                                }
                                */
                        }
                    });
            return b;
        }

        public CompletionStage<Tunnel> connect() {
            return factory.connect(serverAddress, serverPort).thenApply(tunnelBackbone -> {
                System.err.println("Got the backbone, now building up the tunnel");
                // build up the rest of the tunnel...
                tunnelBackbone.tunnel(null);

                got to bind to localhost and forward packets to a given address. This is for the local
                        stuff. On the server side we need to create a UDP "Tunnel Opening/Endpoint" or
                        perhaps this is what the NettyUdpTunnel is for...
                return new NettyUdpTunnel();
            });
            /*
            .exceptionally(throwable -> {
                final CompletableFuture<Tunnel> failed = new CompletableFuture<>();
                failed.completeExceptionally(throwable);
                return failed;
            });
            */
        }

    }

}
