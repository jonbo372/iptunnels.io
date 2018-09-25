package io.iptunnels.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.iptunnels.proto.HelloPacket;

public class PublicTunnelConfig {

    /**
     * If true then check the incoming {@link HelloPacket} for a suggestion from
     * the client. If false, then we'll just ignore what the client tells us.
     * If the client suggests a port of zero, that means that we should pick after all.
     */
    @JsonProperty("clientSuggestedPort")
    private final boolean useClientSuggestedPort = true;

    /**
     * If the client doesn't suggest a port, then we'll pick one at random between
     * this begin and end port range.
     */
    @JsonProperty("startPortRange")
    private final int start = 10000;

    @JsonProperty("stopPortRange")
    private final int stop = 20000;

    @JsonProperty("fixedPort")
    private int fixedPort = 5683;

    @JsonProperty("usePortRange")
    private boolean usePortRange = true;

    /**
     * Use this IP for the external tunnel. If this one is set not set then
     * we will bind to any.
     */
    @JsonProperty("ip")
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public boolean isUsePortRange() {
        return usePortRange;
    }

    public void setUsePortRange(final boolean usePortRange) {
        this.usePortRange = usePortRange;
    }

    public void setFixedPort(final int port) {
        fixedPort = port;
    }

    public int getFixedPort() {
        return fixedPort;
    }

    @JsonIgnore
    public boolean isUseClientSuggestedPort() {
        return useClientSuggestedPort;
    }

    @JsonIgnore
    public int getStartPortRange() {
        return start;
    }

    @JsonIgnore
    public int getStopPortRange() {
        return stop;
    }
}
