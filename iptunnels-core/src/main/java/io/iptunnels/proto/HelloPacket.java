package io.iptunnels.proto;

public interface HelloPacket extends TunnelPacket {

    @Override
    default boolean isHello() {
        return true;
    }

    @Override
    default HelloPacket toHello() {
        return this;
    }

    class HelloPacketVersion1 implements HelloPacket {

        @Override
        public byte[] encode() {
            return " 1HEL".getBytes();
        }
    }
}
