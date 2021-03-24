package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when A Peer has been handshaked and it's ready to communicate with.
 */
public final class PeerHandshakedEvent extends Event {
    private final PeerAddress peerAddress;
    // Version Msg sent by the remote Peer during the Handshake process:
    private final VersionMsg versionMsg;

    public PeerHandshakedEvent(PeerAddress peerAddress, VersionMsg versionMsg) {
        this.peerAddress = peerAddress;
        this.versionMsg = versionMsg;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public VersionMsg getVersionMsg()   { return this.versionMsg; }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Event[Peer Handshaked]: " + peerAddress + " : " + versionMsg.getUser_agent().getStr());
        return result.toString();
    }
}