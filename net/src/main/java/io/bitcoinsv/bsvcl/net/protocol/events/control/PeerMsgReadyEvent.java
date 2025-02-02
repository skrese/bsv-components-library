package io.bitcoinsv.bsvcl.net.protocol.events.control;


import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.MessageStream;
import io.bitcoinsv.bsvcl.net.network.events.P2PEvent;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is connected, and the connection is wrapped up in a Message Stream, which
 * will take care of Serializing and Deserializing the Messages coming through it.
 */
public final class PeerMsgReadyEvent extends P2PEvent {
    private final MessageStream stream;

    public PeerMsgReadyEvent(MessageStream stream) {
        this.stream = stream;
    }

    public MessageStream getStream() { return this.stream; }

    @Override
    public String toString() {
        return "Event[PeerMsgStream Connected]: " + stream.getPeerAddress().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeerMsgReadyEvent other = (PeerMsgReadyEvent) obj;
        return Objects.equals(this.stream, other.stream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stream);
    }
}
