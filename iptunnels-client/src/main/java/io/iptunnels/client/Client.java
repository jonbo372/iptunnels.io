package io.iptunnels.client;

import io.iptunnels.BackendInaccessibleException;
import io.iptunnels.Tunnel;
import io.iptunnels.client.config.ClientConfig;
import io.iptunnels.config.ConfigurationEnvironment;

import java.net.InetSocketAddress;
import java.util.Optional;

public class Client {

    private final ClientConfig config;

    public Client(final ClientConfig config) {
        this.config = config;
    }

    public void run() throws Exception {

    }

    private static boolean isInt(final String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    public static Optional<InetSocketAddress> processTarget(final String target) {
        final String host;
        final int port;
        if (target.indexOf(':') != -1) {
            final String[] parts = target.split(":");
            host = parts[0];
            if (isInt(parts[1])) {
                port = Integer.parseInt(parts[1]);
            } else {
                System.err.println("[ERROR] " + parts[1] + " is not a valid port");
                return Optional.empty();
            }
        } else {
            if (isInt(target)) {
                host = "127.0.0.1";
                port = Integer.parseInt(target);
            } else {
                System.err.println("[ERROR] You must specify the target port at a minimum");
                return Optional.empty();
            }
        }

        return Optional.of(new InetSocketAddress(host, port));
    }

    private static void usage() {
        System.out.println("");
        System.out.println("Usage: iptunnels <target>");
        System.out.println("");
        System.out.println("Examples: ");
        System.out.println("   iptunnels 4455                  # Acquire a public UDP tunnel and forward all traffic to port 4455 on localhost");
        System.out.println("   iptunnels example.com:4455      # Tunnel to host:port instead of localhost");
    }

    public static void main(final String...args) throws Exception {
        // we'll use  proper arg parser later. For now, this is simple enough
        if (args.length != 1) {
            System.err.println("[ERROR] You must specify the forwarding target");
            sleep(20); // stupid
            usage();
            System.exit(1);
        }

        final Optional<InetSocketAddress> target = processTarget(args[0]);
        if (!target.isPresent()) {
            System.err.println("[ERROR] Unable to parse the supplied argument to an address");
            sleep(20); // stupid
            usage();
            System.exit(1);
        }

        final ConfigurationEnvironment<ClientConfig> configEnv = ConfigurationEnvironment.of(ClientConfig.class).withProjectName("iptunnels").build();
        final ClientConfig config = configEnv.loadConfig();

        // the local target to which we will relay all packets.
        // final String targetHost = "127.0.0.1";
        // final int targetPort = 5683;

        Tunnel.udp(config.getServer()).withTargetAddress(target.get()).connect().thenAccept(tunnel -> {
            System.out.println("Tunnel Session Online");
            System.out.println("Forwarding: " + tunnel.getTunnelAddress() + " <--> " + tunnel.getTargetAddress());
        }).exceptionally(t -> {
            processTunnelSetupException(t);
            System.exit(1);
            return null;
        });
    }

    private static void sleep(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    private static void processTunnelSetupException(final Throwable t) {
        if (t.getCause() instanceof BackendInaccessibleException) {
            System.err.println("[ERROR] " + t.getMessage());
        } else {
            System.err.println("[ERROR] Unable to establish tunnel due to exception. Message (if any): "
                    + t.getMessage());
            t.printStackTrace();
        }

    }
}
