package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockTransactionsMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;

/**
 * An Event triggered when a CMPCTBLCKTXS message is sent to a remote peer..
 */
public final class CompactBlockTransactionsMsgSentEvent extends MsgSentEvent<CompactBlockTransactionsMsg> {
    public CompactBlockTransactionsMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<CompactBlockTransactionsMsg> message) {
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