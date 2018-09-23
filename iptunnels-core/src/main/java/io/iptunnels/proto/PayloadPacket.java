package io.iptunnels.proto;

import java.nio.ByteBuffer;

public interface PayloadPacket extends TunnelPacket {

    @Override
    default boolean isPayload() {
        return true;
    }

    @Override
    default PayloadPacket toPayload() {
        return this;
    }

    byte[] getBody();

    class PayloadPacketVersion1 implements PayloadPacket {

        private static final byte[] header = " 1PLD".getBytes();

        private final int tunnelId;
        private final byte[] data;

        public PayloadPacketVersion1(final int tunnelId, final byte[] data) {
            this.tunnelId = tunnelId;
            this.data = data;
        }

        public byte[] getBody() {
            return data;
        }

        @Override
        public byte[] encode() {
            final int size = header.length + 4 + 4 + data.length;
            final ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.put(header);
            buffer.putInt(tunnelId);
            buffer.putInt(data.length);
            buffer.put(data);
            return buffer.array();
        }
    }
}
