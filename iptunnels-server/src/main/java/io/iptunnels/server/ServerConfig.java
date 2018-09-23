package io.iptunnels.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ServerConfig {

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("port")
    private int port;

    @JsonIgnore
    public SocketAddress getListenAddress() {
        if (ip == null) {
            return new InetSocketAddress(port);
        }

        return new InetSocketAddress(ip, port);
    }
}
