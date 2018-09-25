package io.iptunnels.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ServerConfig {

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("port")
    private int port = 8000;

    @JsonProperty("tunnel")
    private PublicTunnelConfig tunnelConfig = new PublicTunnelConfig();

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setPublicTunnelConfig(final PublicTunnelConfig config) {
        tunnelConfig = config;
    }

    @JsonIgnore
    public SocketAddress getListenAddress() {
        if (ip == null) {
            return new InetSocketAddress(port);
        }

        return new InetSocketAddress(ip, port);
    }

    @JsonIgnore
    public PublicTunnelConfig getTunnelConfig() {
        return tunnelConfig;
    }
}
