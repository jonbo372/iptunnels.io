package io.iptunnels.netty;

import io.iptunnels.proto.TunnelPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class TunnelPacketStreamEncoder extends MessageToByteEncoder<TunnelPacket> {
    @Override
    protected void encode(final ChannelHandlerContext ctx, final TunnelPacket pkt, final ByteBuf out) throws Exception {
            out.writeBytes(pkt.encode());
    }
}


