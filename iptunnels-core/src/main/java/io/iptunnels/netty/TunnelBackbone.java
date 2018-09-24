package io.iptunnels.netty;

import io.iptunnels.Tunnel;
import io.iptunnels.proto.PayloadPacket;

/**
 * The {@link TunnelBackbone} is the link between the client and server through which
 * we will transport all the raw messages we are receiving at either end of
 * the {@link Tunnel}
 *
 * A {@link TunnelBackbone} is always TCP based and will always be initiated by the client to establish a transport
 * between it and the server.
 */
public interface TunnelBackbone {


    void disconnect();

    void tunnel(final PayloadPacket pkt);

}
