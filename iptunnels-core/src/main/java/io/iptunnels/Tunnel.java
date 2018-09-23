package io.iptunnels;

import io.iptunnels.netty.BackboneFactory;
import io.iptunnels.netty.NettyUdpTunnel;
import io.iptunnels.netty.TunnelBackbone;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;

/**
 * Represents a tunnel
 */
public interface Tunnel {

    static Builder udp(final String serverAddress, final int serverPort) {
        return new Builder(Transport.UDP, serverAddress, serverPort);
    }

    /**
     * Construct a new tunnel with the given {@link TunnelBackbone}. This is what's typically
     * done on the server side since it will have accepted the incoming TCP connection that is going
     * to be used as the backbone and based off of it, it will create a public facing port that is connected
     * to this backbone.
     *
     * @param backbone
     * @return
     */
    static ServerTunnelBuilder of(final TunnelBackbone backbone) {
        return new ServerTunnelBuilder(backbone);
    }

    TunnelIdentifier getId();

    /**
     * This is the transport that is exposed by the {@link Tunnel}.
     *
     * @return
     */
    Transport getTransport();

    /**
     * The the port to which this {@link Tunnel} is listening for traffic.
     * @return
     */
    int getLocalPort();

    /**
     * The port to which this {@link Tunnel} is listening for traffic.
     * @return
     */
    String getLocalHost();

    /**
     * This is our public tunnel address, i.e. the one we advertise to the world and is then relayed
     * to our local machine and onto the {@link #getTargetAddress()}.
     *
     * @return
     */
    InetSocketAddress getTunnelAddress();

    InetSocketAddress getTargetAddress();

    /**
     * TODO: we really should have a build stepper thingie here because we either have to
     * connect to the server or we are on the server side that accepted a new TCP backbone
     * connection and as such, will create the backbone first then create the Udp tunnel with
     * that backbone.
     */
    class ServerTunnelBuilder {
        private final TunnelBackbone backbone;

        private ServerTunnelBuilder(final TunnelBackbone backbone) {
            this.backbone = backbone;
        }

        public CompletionStage<Tunnel> bind() {
            return bind(0);
        }

        public CompletionStage<Tunnel> bind(final int port) {
            return bind(null, port);
        }

        public CompletionStage<Tunnel> bind(final String ip, final int port) {
            final InetSocketAddress localAddress;
            if (ip != null && !ip.isEmpty()) {
                localAddress = new InetSocketAddress(ip, port);
            } else {
                localAddress = new InetSocketAddress(port);
            }
            return bind(localAddress);
        }

        public CompletionStage<Tunnel> bind(final InetSocketAddress localAddress) {
            return NettyUdpTunnel.withBackbone(backbone).withUdpBootstrap(UdpBootstrap.getBootstrap()).start(localAddress);
        }

    }

    /**
     * Stupid
     */
    class UdpBootstrap {

        // should be injected.
        private static final EventLoopGroup udpGroup = new NioEventLoopGroup();
        private static final Bootstrap bootstrap = createUDPBootstrap();

        public static Bootstrap getBootstrap() {
            return bootstrap;
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
    }

    class Builder {

        private final Transport transport;
        private final String serverAddress;
        private final int serverPort;

        private InetSocketAddress targetAddress;


        // whatever...
        private final BackboneFactory factory = BackboneFactory.SINGLETON;

        private Builder(final Transport transport, final String serverAddress, final int serverPort) {
            this.transport = transport;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        public Builder withTargetAddress(final String ip, final int port) {
            targetAddress = new InetSocketAddress(ip, port);
            return this;
        }

        public CompletionStage<Tunnel> connect() {

            return factory.connect(serverAddress, serverPort).thenCompose(tunnelBackbone -> {
                System.err.println("Got the backbone, now building up the tunnel");
                // build up the rest of the tunnel...

                return NettyUdpTunnel.withBackbone(tunnelBackbone).withUdpBootstrap(UdpBootstrap.getBootstrap()).withTargetAddress(targetAddress).start(0);

                /*
                got to bind to localhost and forward packets to a given address. This is for the local
                        stuff. On the server side we need to create a UDP "Tunnel Opening/Endpoint" or
                        perhaps this is what the NettyUdpTunnel is for...
                        */
                // return new NettyUdpTunnel();
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
