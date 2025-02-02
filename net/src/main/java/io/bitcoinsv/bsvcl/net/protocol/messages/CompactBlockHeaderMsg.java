package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.Message;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.AbstractBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

import java.io.Serializable;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class CompactBlockHeaderMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "CompactClockHeader";

    public static final int TIMESTAMP_LENGTH = 4;

    public static final int NONCE_LENGTH = 4;

    // IMPORTANT: This field (hash) is NOT SERIALIZED.
    // The hash of the block is NOT part of the BLOCK Message itself: its external to it.
    // In order to calculate a Block Hash we need to serialize the Block first, so instead of doing
    // that avery time we need a Hash, we store the Hash here, at the moment when we deserialize the
    // Block for the first time, so its available for further use.
    protected final HashMsg hash;

    protected final long version;
    protected final HashMsg prevBlockHash;
    protected final HashMsg merkleRoot;
    protected final long creationTimestamp;
    protected final long difficultyTarget;
    protected final long nonce;

    public CompactBlockHeaderMsg(HashMsg hash,
                                 long version,
                                 HashMsg prevBlockHash,
                                 HashMsg merkleRoot,
                                 long creationTimestamp,
                                 long difficultyTarget,
                                 long nonce) {
        this.hash = hash;
        this.version = version;
        this.prevBlockHash = prevBlockHash;
        this.merkleRoot = merkleRoot;
        this.creationTimestamp = creationTimestamp;
        this.difficultyTarget = difficultyTarget;
        this.nonce = nonce;
        init();
    }

    public static CompactBlockHeaderMsgBuilder builder() {
        return new CompactBlockHeaderMsgBuilder();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes() +
            TIMESTAMP_LENGTH + TIMESTAMP_LENGTH + NONCE_LENGTH;
    }

    @Override
    protected void validateMessage() {
    }

    @Override
    public String toString() {
        return "CompactBlockHeaderMsg(hash=" + this.getHash() + ", version=" + this.getVersion() + ", prevBlockHash=" + this.getPrevBlockHash() + ", merkleRoot=" + this.getMerkleRoot() + ", creationTimestamp=" + this.getCreationTimestamp() + ", difficultyTarget=" + this.getDifficultyTarget() + ", nonce=" + this.getNonce() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }

        CompactBlockHeaderMsg other = (CompactBlockHeaderMsg) obj;

        return Objects.equal(this.hash, other.hash)
                && Objects.equal(this.version, other.version)
                && Objects.equal(this.prevBlockHash, other.prevBlockHash)
                && Objects.equal(this.merkleRoot, other.merkleRoot)
                && Objects.equal(this.creationTimestamp, other.creationTimestamp)
                && Objects.equal(this.difficultyTarget, other.difficultyTarget)
                && Objects.equal(this.nonce, other.nonce);
    }

    /**
     * Returns a Domain Class.
     */
    public HeaderReadOnly toBean() {
        HeaderBean result = new HeaderBean((AbstractBlock) null);
        result.setTime(this.creationTimestamp);
        result.setDifficultyTarget(this.difficultyTarget);
        result.setNonce(this.nonce);
        result.setPrevBlockHash(Sha256Hash.wrapReversed(this.prevBlockHash.getHashBytes()));
        result.setVersion(this.version);
        result.setMerkleRoot(Sha256Hash.wrapReversed(this.merkleRoot.getHashBytes()));
        result.setHash(Sha256Hash.wrapReversed(this.hash.getHashBytes()));
        return result;
    }

    public HashMsg getHash()            { return this.hash; }
    public long getVersion()            { return this.version; }
    public HashMsg getPrevBlockHash()   { return this.prevBlockHash; }
    public HashMsg getMerkleRoot()      { return this.merkleRoot; }
    public long getCreationTimestamp()  { return this.creationTimestamp; }
    public long getDifficultyTarget()   { return this.difficultyTarget; }
    public long getNonce()              { return this.nonce; }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce);
    }

    public CompactBlockHeaderMsgBuilder toBuilder() {
        return new CompactBlockHeaderMsgBuilder()
                    .hash(this.hash)
                    .version(this.version)
                    .prevBlockHash(this.prevBlockHash)
                    .merkleRoot(this.merkleRoot)
                    .creationTimestamp(this.creationTimestamp)
                    .difficultyTarget(this.difficultyTarget)
                    .nonce(this.nonce);
    }

    /**
     * Builder
     */
    public static class CompactBlockHeaderMsgBuilder {
        protected HashMsg hash;
        protected long version;
        protected HashMsg prevBlockHash;
        protected HashMsg merkleRoot;
        protected long creationTimestamp;
        protected long difficultyTarget;
        protected long nonce;

        public CompactBlockHeaderMsgBuilder() {}

        public CompactBlockHeaderMsgBuilder hash(HashMsg hash) {
            this.hash = hash;
            return this;
        }

        public CompactBlockHeaderMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public CompactBlockHeaderMsgBuilder prevBlockHash(HashMsg prevBlockHash) {
            this.prevBlockHash = prevBlockHash;
            return this;
        }

        public CompactBlockHeaderMsgBuilder merkleRoot(HashMsg merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public CompactBlockHeaderMsgBuilder creationTimestamp(long creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public CompactBlockHeaderMsgBuilder difficultyTarget(long difficultyTarget) {
            this.difficultyTarget = difficultyTarget;
            return this;
        }

        public CompactBlockHeaderMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public CompactBlockHeaderMsg build() {
            return new CompactBlockHeaderMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce);
        }
    }
}
