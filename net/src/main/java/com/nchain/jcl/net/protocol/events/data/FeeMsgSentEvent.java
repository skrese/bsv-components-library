package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.FeeFilterMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a FEE_FILTER Message is sent to a remote Peer.
 */
public final class FeeMsgSentEvent extends MsgSentEvent<FeeFilterMsg> {
    public FeeMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<FeeFilterMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}