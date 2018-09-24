package io.iptunnels.proto;

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
