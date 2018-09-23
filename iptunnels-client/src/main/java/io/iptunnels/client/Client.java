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

        // the iptunnels.io server.
        // final String host = "127.0.0.1";
        // final String host = "104.248.212.248";
        final String host = "159.89.220.227";
        final int port = 8000;

        // the local target to which we will relay all packets.
        final String targetHost = "127.0.0.1";
        final int targetPort = 5683;

        final Tunnel tunnel = Tunnel.udp(host, port).withTargetAddress(targetHost, targetPort).connect().toCompletableFuture().get();
        Thread.sleep(500);
        System.out.println("Tunnel Session Online");
        System.out.println("Forwarding: " + tunnel.getTunnelAddress() + " <--> " + tunnel.getTargetAddress());
    }
}
