package io.bitcoinsv.bsvcl.net.protocol.events.data;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a NOT_FOUND Message is received from a Remote Peer.
 */
public final class NotFoundMsgReceivedEvent extends MsgReceivedEvent<NotFoundMsg> {
    public NotFoundMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<NotFoundMsg> btcMsg) {
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
