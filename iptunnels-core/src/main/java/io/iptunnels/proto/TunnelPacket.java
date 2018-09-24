package io.iptunnels.proto;

public interface TunnelPacket {

    static HelloPacket hello(final int transactionId) {
        return new HelloPacket.HelloPacketVersion1(transactionId);
    }

    static HiPacket hi(final int transactionId, final int tunnelId, final String url) {
        return new HiPacket.HiPacketVersion1(transactionId, tunnelId, url);
    }

    static PayloadPacket payload(final int tunnelId, final byte[] data) {
        return new PayloadPacket.PayloadPacketVersion1(tunnelId, data);
    }

    default boolean isHi() {
        return false;
    }

    default boolean isHello() {
        return false;
    }

    default boolean isPayload() {
        return false;
    }

    default HelloPacket toHello() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + HelloPacket.class.getName());
    }

    default HiPacket toHi() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + HiPacket.class.getName());
    }

    default PayloadPacket toPayload() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + PayloadPacket.class.getName());
    }

    /**
     * Encode this packet as a byte-array.
     *
     * @return
     */
    byte[] encode();

}
