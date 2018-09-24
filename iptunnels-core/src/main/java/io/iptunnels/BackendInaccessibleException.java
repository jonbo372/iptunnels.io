package io.iptunnels;

import java.io.IOException;
import java.net.SocketAddress;

public class BackendInaccessibleException extends IOException {

    private final SocketAddress remote;

    public BackendInaccessibleException(final SocketAddress remote, final String msg) {
        super(msg);
        this.remote = remote;
    }

    public SocketAddress getRemoteAddress() {
        return remote;
    }
}
