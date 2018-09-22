package io.iptunnels.netty;

import io.iptunnels.proto.TunnelPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.Optional;

public interface TunnelFramer {

    /**
     * Process the data from the channel and if we now have enough data to construct a new {@link TunnelPacket}, one
     * will be returned at which point you should throw away this {@link TunnelFramer};
     *
     * @param ctx
     * @param in
     * @return
     */
    Optional<TunnelPacket> process(ChannelHandlerContext ctx, ByteBuf in);

    static TunnelFramer getVersionedFramer(final int version) {
        switch(version) {
            case 1:
                return new TunnelFramerVersion1();
            default:
                throw new IllegalArgumentException("Unknown protocol version " + version);
        }
    }

}
