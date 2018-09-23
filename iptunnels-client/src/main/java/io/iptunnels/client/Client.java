package io.iptunnels.client;

import io.iptunnels.Tunnel;
import io.iptunnels.client.config.ClientConfig;

public class Client {

    private final ClientConfig config;

    public Client(final ClientConfig config) {
        this.config = config;
    }

    public void run() throws Exception {

    }

    public static void main(final String...args) throws Exception {
        final String host = "127.0.0.1";
        final int port = 8000;

        final String targetHost = "127.0.0.1";
        final int targetPort = 2115;

        // final BackboneFactory factory = new BackboneFactory();
        // factory.connect(host, port).toCompletableFuture().get();

        final Tunnel tunnel = Tunnel.udp(host, port).withTargetAddress(targetHost, targetPort).connect().toCompletableFuture().get();

    }
}
