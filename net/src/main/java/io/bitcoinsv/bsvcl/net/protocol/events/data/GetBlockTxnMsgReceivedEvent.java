package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a @{@link GetBlockTxnMsg} Message is received from a Remote Peer.
 */
public final class GetBlockTxnMsgReceivedEvent extends MsgReceivedEvent<GetBlockTxnMsg> {
    public GetBlockTxnMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<GetBlockTxnMsg> btcMsg) {
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