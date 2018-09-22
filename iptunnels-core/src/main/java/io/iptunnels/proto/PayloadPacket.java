package io.iptunnels.proto;

public interface PayloadPacket extends TunnelPacket {

    @Override
    default boolean isPayload() {
        return true;
    }

    @Override
    default PayloadPacket toPayload() {
        return this;
    }
}
