package io.iptunnels.netty;

import io.iptunnels.proto.TunnelPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TunnelFramerVersion1 implements TunnelFramer {

    private static final Logger logger = LoggerFactory.getLogger(TunnelFramerVersion1.class);

    private DecodingState state = DecodingState.HEADER;

    // TODO: need to move these into their own class
    // Stuff needed for when parsing HI message
    private HiDecodingState hiState = HiDecodingState.HEADER;
    private int tunnelId;
    private int breakoutLength;

    @Override
    public Optional<TunnelPacket> process(final ChannelHandlerContext ctx, final ByteBuf in) {

        boolean isMakingProgress = in.isReadable();
        Optional<TunnelPacket> pkt = Optional.empty();

        while (isMakingProgress && !pkt.isPresent()) {
            final int start = in.readerIndex();
            switch (state) {
                case HEADER:
                    processHeader(in);
                    break;
                case HELLO:
                    pkt = processHello(in);
                    break;
                case PAYLOAD:
                    pkt = processPayload(in);
                    break;
                case HI:
                    pkt = processHi(in);
                    break;
                case UNKNOWN:
                    // TODO: throw exception instead...
                    System.err.println("Ended up in a unknown decoding state, bailing out");
                    ctx.close();
                    return Optional.empty();
                default:
                    logger.warn("Unknown DecodingState. Must be a bug");
            }

            isMakingProgress = start < in.readerIndex();
        }

        // useful when testing using telnet since you need to hit enter at the end.
        // Perhaps we should make that part of the protocol and skip the leading space
        // instead.
        pkt.ifPresent(ignore -> consumeLineFeed(in));

        return pkt;
    }

    private void consumeLineFeed(final ByteBuf in) {
        if (in.readableBytes() >=2 ) {
            final int index = in.readerIndex();
            if (in.getByte(index) == 13 && in.getByte(index + 1) == 10) {
                in.readerIndex(index + 2);
            }
        }
    }

    private Optional<TunnelPacket> processPayload(final ByteBuf in) {
        return Optional.empty();
    }

    private Optional<TunnelPacket> processHi(final ByteBuf in) {
        switch (hiState) {
            case HEADER:
                if (in.readableBytes() >= 4) {
                    tunnelId = in.readInt();
                    breakoutLength = in.readInt();
                    hiState = HiDecodingState.URL;
                }
                break;
            case URL:
                if (in.readableBytes() >= breakoutLength) {
                    final byte[] rawUrl = new byte[breakoutLength];
                    in.readBytes(rawUrl);
                    final TunnelPacket hi = TunnelPacket.hi(tunnelId, new String(rawUrl));
                    tunnelId = 0;
                    breakoutLength = 0;
                    hiState = HiDecodingState.HEADER;
                    return Optional.of(hi);
                }
                break;
        }

        return Optional.empty();
    }

    private void processHeader(final ByteBuf in) {
        if (in.readableBytes() >= 3) {
            final ByteBuf header = in.readBytes(3);
            final byte a = header.getByte(0);
            final byte b = header.getByte(1);
            final byte c = header.getByte(2);

            if (a == 'H' && b == 'E' && c == 'L') {
                state = DecodingState.HELLO;
            } else if (a == 'P' && b == 'L' && c == 'D') {
                state = DecodingState.PAYLOAD;
            } else if (a == 'H' && b == 'I' && c == 'I') {
                state = DecodingState.HI;
            } else {
                state = DecodingState.UNKNOWN;
            }
        }
    }

    private Optional<TunnelPacket> processHello(final ByteBuf in) {
        return Optional.of(TunnelPacket.hello());
    }

    private enum DecodingState {
        HEADER, HELLO, PAYLOAD, HI, UNKNOWN;
    }

    private enum HiDecodingState {
        HEADER, URL;
    }

}
