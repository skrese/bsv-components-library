package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockTxnMsg extends Message {
    public static final String MESSAGE_TYPE = "blocktxn";

    private final HashMsg blockHash;
    private final VarIntMsg transactionsLength;
    private final List<TxMsg> transactions;

    public BlockTxnMsg(HashMsg blockHash, VarIntMsg transactionsLength, List<TxMsg> transactions) {
        this.blockHash = blockHash;
        this.transactionsLength = transactionsLength;
        this.transactions = transactions;
        init();
    }

    public static BlockTransactionsMsgBuilder builder() {
        return new BlockTransactionsMsgBuilder();
    }

    public HashMsg getBlockHash() {
        return blockHash;
    }

    public VarIntMsg getTransactionsLength() {
        return transactionsLength;
    }

    public List<TxMsg> getTransactions() {
        return transactions;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return blockHash.calculateLength()
            + transactionsLength.calculateLength()
            + transactions.stream()
            .mapToLong(TxMsg::calculateLength)
            .sum();
    }

    @Override
    protected void validateMessage() {
    }

    public static class BlockTransactionsMsgBuilder {
        private HashMsg blockHash;
        private VarIntMsg transactionsLength;
        private List<TxMsg> transactions;

        public BlockTransactionsMsgBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public BlockTransactionsMsgBuilder transactionsLength(VarIntMsg transactionsLength) {
            this.transactionsLength = transactionsLength;
            return this;
        }

        public BlockTransactionsMsgBuilder transactions(List<TxMsg> transactions) {
            this.transactions = transactions;
            return this;
        }

        public BlockTxnMsg build() {
            return new BlockTxnMsg(blockHash, transactionsLength, transactions);
        }
    }
}