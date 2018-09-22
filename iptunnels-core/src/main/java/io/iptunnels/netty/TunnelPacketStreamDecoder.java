package io.iptunnels.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Not a sharable class!
 */
public class TunnelPacketStreamDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(TunnelPacketStreamDecoder.class);

    /**
     * If the framer is null, we are in the "INIT" state, finding the boundary of the messages and the
     * version of the data to come.
     */
    private TunnelFramer framer;


    @Override
    public boolean isSingleDecode() {
        return true;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {

        if (framer != null) {
            framer.process(ctx, in).ifPresent(pkt -> {
                out.add(pkt);
                framer = null;
            });
        } else {

            if (in.readableBytes() < 2) {
                return;
            }

            final ByteBuf header = in.readBytes(2);
            if (header.getByte(0) != ' ') {
                logger.info("Every new message must start with empty space, this one doesn't. Closing");
                ctx.close();
                return;
            }

            final int version = header.getByte(1) - '0';
            framer = TunnelFramer.getVersionedFramer(version);
        }

    }
}
