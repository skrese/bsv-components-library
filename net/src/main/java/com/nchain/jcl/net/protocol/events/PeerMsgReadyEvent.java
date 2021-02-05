package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.net.protocol.streams.MessageStream;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is connected, and the connection is wrapped up in a Message Stream, which
 * will take care of Serializing and Deserializing the Messages coming through it.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeerMsgReadyEvent extends Event {
    private MessageStream stream;

    @Override
    public String toString() {
        return "Event[PeerMsgStream Connected]: " + stream.getPeerAddress().toString();
    }
}
