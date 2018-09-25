package io.iptunnels.proto;

import io.iptunnels.config.PublicTunnelConfig;

import java.nio.ByteBuffer;

public interface HelloPacket extends TunnelPacket {

    int transactionId();

    @Override
    default boolean isHello() {
        return true;
    }

    @Override
    default HelloPacket toHello() {
        return this;
    }

    /**
     * The client can in the {@link HelloPacket} suggest a port to use for the
     * public facing tunnel. Whether or not this is honored by the server is also
     * controlled by the configuration object {@link PublicTunnelConfig#isUseClientSuggestedPort()}
     *
     * Note: even if the server is configured to honor the suggested port, if the client
     * suggests a port of zero, it means "i don't care, you pick" and then the server will
     * follow the suggestions on the configuration object.
     *
     * @return
     */
    default int port() {
        return 0;
    }

    class HelloPacketVersion1 implements HelloPacket {
        private static final byte[] header = " 1HEL".getBytes();
        public static final int SIZE = header.length + 4;

        private final int transactionId;

        public HelloPacketVersion1(final int transactionId) {
            this.transactionId = transactionId;
        }

        @Override
        public byte[] encode() {
            final ByteBuffer buffer = ByteBuffer.allocate(SIZE);
            buffer.put(header);
            buffer.putInt(transactionId);
            return buffer.array();
        }

        @Override
        public int transactionId() {
            return transactionId;
        }
    }
}
