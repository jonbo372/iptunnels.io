package io.iptunnels;

import io.iptunnels.logging.Console;
import io.iptunnels.netty.BackboneFactory;
import io.iptunnels.netty.ClientSideBackbone;
import io.iptunnels.netty.NettyBootstrap;
import io.iptunnels.netty.NettyUdpTunnel;
import io.iptunnels.netty.TunnelBackbone;
import io.iptunnels.proto.TunnelPacket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.CompletionStage;

/**
 * Represents a tunnel
 */
public interface Tunnel {

    static Builder udp(final String serverAddress) {
        final URI uri = URI.create("http://" + serverAddress);
        final int port = uri.getPort();
        final String host = uri.getHost();
        return udp(host, port);
    }

    static Builder udp(final String serverAddress, final int serverPort) {
        return new Builder(Transport.UDP, new InetSocketAddress(serverAddress, serverPort));
    }

    static Builder udp(final SocketAddress serverAddress) {
        return new Builder(Transport.UDP, serverAddress);
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

    int getId();

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
     * Shutdown the tunnel again
     *
     * TODO: should probably return a future...
     */
    void shutdown();

    void process(TunnelPacket pkt);

    /**
     * TODO: we really should have a build stepper thingie here because we either have to
     * connect to the server or we are on the server side that accepted a new TCP backbone
     * connection and as such, will create the backbone first then create the Udp tunnel with
     * that backbone.
     */
    class ServerTunnelBuilder {
        private final TunnelBackbone backbone;

        private final Random random = new Random(System.currentTimeMillis());


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
            final int tunnelId = random.nextInt();
            return NettyUdpTunnel.withTunnelId(tunnelId).withBackbone(backbone).withUdpBootstrap(NettyBootstrap.getBootstrap()).start(localAddress);
        }

    }

    class Builder {

        private final Transport transport;

        private InetSocketAddress targetAddress;
        private final SocketAddress serverAddress;


        // whatever...
        private final BackboneFactory factory = BackboneFactory.SINGLETON;

        private Builder(final Transport transport, final SocketAddress serverAddress) {
            this.transport = transport;
            this.serverAddress = serverAddress;
        }

        public Builder withTargetAddress(final String ip, final int port) {
            targetAddress = new InetSocketAddress(ip, port);
            return this;
        }

        public Builder withTargetAddress(final InetSocketAddress target) {
            targetAddress = target;
            return this;
        }

        public CompletionStage<Tunnel> connect() {
            Console.info("Connecting to iptunnels server at {}", serverAddress);

            return factory.connect(serverAddress).thenCompose(backbone -> {
                final ClientSideBackbone tunnelBackbone = backbone;
                return tunnelBackbone.hello().thenCompose(hi -> {
                    final InetSocketAddress breakoutAddress = hi.breakoutAddressAsSocketAddress();
                    return NettyUdpTunnel.withTunnelId(hi.tunnelId())
                            .withBackbone(tunnelBackbone)
                            .withBreakoutAddress(breakoutAddress)
                            .withUdpBootstrap(NettyBootstrap.getBootstrap())
                            .withTargetAddress(targetAddress)
                            .start(0).thenApply(tunnel -> {
                                // TODO: not nice
                                ((ClientSideBackbone)tunnelBackbone).manageTunnel(tunnel);
                                return tunnel;
                            });

                });
            });
        }
    }
}
