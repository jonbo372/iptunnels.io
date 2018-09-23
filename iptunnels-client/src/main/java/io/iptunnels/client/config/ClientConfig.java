package io.iptunnels.client.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientConfig {

    @JsonProperty("version")
    private final int version = 1;

    @JsonProperty("server")
    private final String server = "iptunnels.io:8000";

    public String getServer() {
        return server;
    }

}
