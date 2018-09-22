package io.iptunnels.netty;

import io.iptunnels.Transport;
import io.iptunnels.Tunnel;
import io.iptunnels.TunnelIdentifier;

public class NettyUdpTunnel implements Tunnel {

    @Override
    public TunnelIdentifier getId() {
        return null;
    }

    @Override
    public Transport getTransport() {
        return Transport.UDP;
    }
}
