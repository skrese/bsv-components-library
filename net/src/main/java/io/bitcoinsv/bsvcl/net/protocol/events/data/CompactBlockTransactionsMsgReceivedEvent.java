package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockTransactionsMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;

/**
 * An event triggered when a CMPCTBLCKTXS message is received from a remote peer.
 */
public final class CompactBlockTransactionsMsgReceivedEvent extends MsgReceivedEvent<CompactBlockTransactionsMsg> {
    public CompactBlockTransactionsMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<CompactBlockTransactionsMsg> message) {
        super(peerAddress, message);
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