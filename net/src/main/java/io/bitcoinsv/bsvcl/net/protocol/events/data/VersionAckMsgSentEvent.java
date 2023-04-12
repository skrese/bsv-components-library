package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.VersionAckMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a VERSION_ACK Message is sent to a remote Peer.
 */
public final class VersionAckMsgSentEvent extends MsgSentEvent<VersionAckMsg> {
    public VersionAckMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<VersionAckMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}