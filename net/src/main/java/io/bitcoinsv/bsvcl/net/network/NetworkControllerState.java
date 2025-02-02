package io.bitcoinsv.bsvcl.net.network;

import io.bitcoinsv.bsvcl.common.handlers.HandlerState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This Event stores the current State of the Connection/Network Handler. The Network Handler implements the
 * physical and low-level connection to a remote Peers, and handles all the incoming/outcoming data between
 * the 2 parties.
 */
public final class NetworkControllerState extends HandlerState {
    private final int numActiveConns;
    private final int numInProgressConns;
    private final int numPendingToOpenConns;
    private final int numPendingToCloseConns;
    private final long numConnsFailed;
    private final long numInProgressConnsExpired;

    private final boolean server_mode;
    private final boolean keep_connecting;

    private final long numConnsTried;

    NetworkControllerState(int numActiveConns, int numInProgressConns, int numPendingToOpenConns, int numPendingToCloseConns,
                           boolean server_mode, boolean keep_connecting,
                           long numConnsFailed, long numInProgressConnsExpired,
                           long numConnsTried) {
        this.numActiveConns = numActiveConns;
        this.numInProgressConns = numInProgressConns;
        this.numPendingToOpenConns = numPendingToOpenConns;
        this.numPendingToCloseConns = numPendingToCloseConns;
        this.server_mode = server_mode;
        this.keep_connecting = keep_connecting;
        this.numConnsFailed = numConnsFailed;
        this.numInProgressConnsExpired = numInProgressConnsExpired;
        this.numConnsTried = numConnsTried;
    }


    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Network Handler State: ");
        result.append("Connections: ");
        result.append(numActiveConns).append(" active, ");
        result.append(numInProgressConns).append(" in progress, ");
        result.append(numConnsTried).append(" tried since last state, ");
        result.append(numPendingToOpenConns).append( " pending to Open, ");
        result.append(numPendingToCloseConns).append(" pending to Close, ");
        result.append(numConnsFailed).append(" failed, ");
        result.append(numInProgressConnsExpired).append(" in-progress expired, ");
        result.append(": ").append((server_mode)? "Running in Server Mode" : "Running in Client Mode");
        result.append(": ").append((keep_connecting)? "connecting": "connections stable");

        return result.toString();
    }

    public int getNumActiveConns()          { return this.numActiveConns; }
    public int getNumInProgressConns()      { return this.numInProgressConns; }
    public int getNumPendingToOpenConns()   { return this.numPendingToOpenConns; }
    public int getNumPendingToCloseConns()  { return this.numPendingToCloseConns; }
    public boolean isServer_mode()          { return this.server_mode; }
    public boolean isKeep_connecting()      { return this.keep_connecting; }
    public long getNumCopnnsTried()          { return this.numConnsTried; }

    public static NetworkHandlerStateBuilder builder() {
        return new NetworkHandlerStateBuilder();
    }
    public NetworkHandlerStateBuilder toBuilder() {
        return new NetworkHandlerStateBuilder()
                .numActiveConns(this.numActiveConns)
                .numInProgressConns(this.numInProgressConns)
                .numPendingToOpenConns(this.numPendingToOpenConns)
                .numPendingToCloseConns(this.numPendingToCloseConns)
                .server_mode(this.server_mode)
                .keep_connecting(this.keep_connecting)
                .numConnsFailed(this.numConnsFailed)
                .numInProgressConnsExpired(this.numInProgressConnsExpired)
                .numConnsTried(this.numConnsTried);
    }

    /**
     * Builder
     */
    public static class NetworkHandlerStateBuilder {
        private int numActiveConns;
        private int numInProgressConns;
        private int numPendingToOpenConns;
        private int numPendingToCloseConns;
        private long numConnsFailed;
        private long numInProgressConnsExpired;
        private boolean server_mode;
        private boolean keep_connecting;
        private long numConnsTried;

        NetworkHandlerStateBuilder() {}

        public NetworkControllerState.NetworkHandlerStateBuilder numActiveConns(int numActiveConns) {
            this.numActiveConns = numActiveConns;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder numInProgressConns(int numInProgressConns) {
            this.numInProgressConns = numInProgressConns;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder numPendingToOpenConns(int numPendingToOpenConns) {
            this.numPendingToOpenConns = numPendingToOpenConns;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder numPendingToCloseConns(int numPendingToCloseConns) {
            this.numPendingToCloseConns = numPendingToCloseConns;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder numConnsFailed(long numConnsFailed) {
            this.numConnsFailed = numConnsFailed;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder numInProgressConnsExpired(long numInProgressConnsExpired) {
            this.numInProgressConnsExpired = numInProgressConnsExpired;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder server_mode(boolean server_mode) {
            this.server_mode = server_mode;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder keep_connecting(boolean keep_connecting) {
            this.keep_connecting = keep_connecting;
            return this;
        }

        public NetworkControllerState.NetworkHandlerStateBuilder numConnsTried(long numConnsTried) {
            this.numConnsTried = numConnsTried;
            return this;
        }

        public NetworkControllerState build() {
            return new NetworkControllerState(numActiveConns, numInProgressConns, numPendingToOpenConns, numPendingToCloseConns, server_mode, keep_connecting,
                    numConnsFailed, numInProgressConnsExpired, numConnsTried);
        }
    }
}
