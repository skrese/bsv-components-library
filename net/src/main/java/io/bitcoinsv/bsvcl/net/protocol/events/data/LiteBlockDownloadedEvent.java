package io.bitcoinsv.bsvcl.net.protocol.events.data;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.events.control.BlockDownloadedEvent;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.P2PEvent;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 15:46
 *
 * An Event triggered when a "Lite" Block has been downloaded. A Lite block is a block which size is small enough
 * to be put into memory without risking running out of it.
 *
 * NOTE:
 * When listening for Events regarding Blocks downloaded, the best approach is to listen to "BlockDownloadedEvent"
 * instead, which is always triggered regardless of the block size. This event here thought is only triggered when
 * the Block is NOT Big
 *
 * @see BlockDownloadedEvent
 */
public final class LiteBlockDownloadedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    private final BitcoinMsg<BlockMsg> block; // Whole Downloaded Block.
    private final Duration downloadingTime;

    public LiteBlockDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<BlockMsg> block, Duration downloadingTime) {
        this.peerAddress = peerAddress;
        this.block = block;
        this.downloadingTime = downloadingTime;
    }

    public PeerAddress getPeerAddress()     { return this.peerAddress; }
    public BitcoinMsg<BlockMsg> getBlock()  { return this.block; }
    public Duration getDownloadingTime()    { return this.downloadingTime; }

    @Override
    public String toString() {
        return "LiteBlockDownloadedEvent(peerAddress=" + this.getPeerAddress() + ", block=" + this.getBlock() + ", downloadingTime=" + this.getDownloadingTime() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        LiteBlockDownloadedEvent other = (LiteBlockDownloadedEvent) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.block, other.block)
                && Objects.equal(this.downloadingTime, other.downloadingTime);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, block, downloadingTime);
    }
}
