package io.iptunnels;

public enum Transport {
    UDP, TCP, TLS;

    public boolean isUDP() {
        return this == UDP;
    }

    public boolean isTCP() {
        return this == TCP;
    }

    public boolean isTLS() {
        return this == TLS;
    }

}
