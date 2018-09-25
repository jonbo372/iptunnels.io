package io.iptunnels.proto;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * The response from the server to the {@link HelloPacket}. Yeah I know, it is a bit dumb. Could do what TLS does
 * with "Client Hello" follow with a "Server Hello" but i'm just keeping this simple and dumb for now.
 *
 */
public interface HiPacket extends TunnelPacket {


    int transactionId();

    int tunnelId();

    String breakoutAddress();

    InetSocketAddress breakoutAddressAsSocketAddress();

    @Override
    default boolean isHi() {
        return true;
    }

    @Override
    default HiPacket toHi() {
        return this;
    }

    class HiPacketVersion1 implements HiPacket {

        private static final byte[] header = " 1HII".getBytes();

        private final byte[] raw;

        private final int tunnelId;

        private final int transactionId;

        private final String breakoutAddress;

        public HiPacketVersion1(final int transactionId, final int tunnelId, final String url) {
            final byte[] urlBytes = url.getBytes();
            final int size = header.length + 4 + 4 + 4 + urlBytes.length;
            final ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.put(header);
            buffer.putInt(transactionId);
            buffer.putInt(tunnelId);
            buffer.putInt(urlBytes.length);
            buffer.put(urlBytes);
            raw = buffer.array();

            this.transactionId = transactionId;
            this.tunnelId = tunnelId;
            this.breakoutAddress = url;
        }

        @Override
        public String toString() {
            return "HI " + transactionId + " " + tunnelId + " " + breakoutAddress;
        }

        @Override
        public byte[] encode() {
            return raw;
        }

        @Override
        public int transactionId() {
            return transactionId;
        }

        @Override
        public int tunnelId() {
            return tunnelId;
        }

        @Override
        public InetSocketAddress breakoutAddressAsSocketAddress() {
            final int index = breakoutAddress.lastIndexOf(':');
            final String host = breakoutAddress.substring(0, index);
            final int port = Integer.parseInt(breakoutAddress.substring(index + 1, breakoutAddress.length()));
            return new InetSocketAddress(host, port);
        }

        @Override
        public String breakoutAddress() {
            return breakoutAddress;
        }
    }

}
