/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockTxnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.HashMsgSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.TxMsgSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.VarIntMsgSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.Optional.ofNullable;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public class BigBlockTxnDeserializer extends LargeMessageDeserializerImpl {

    // The TX are Deserialized and notified in batches:
    private static final int TX_BATCH = 10_000;

    public BigBlockTxnDeserializer() {
    }

    public BigBlockTxnDeserializer(ExecutorService executor) {
        super(executor);
    }

    @Override
    public void deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        var blockHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        var numOfTxs = VarIntMsgSerializer.getInstance().deserialize(context, byteReader).getValue();

        context.setCalculateHashes(true);

        long batchSize = ofNullable(context.getBatchSize()).orElse(TX_BATCH);

        List<TxMsg> transactions = new ArrayList<>();
        for (int i = 0; i < numOfTxs; i++) {
            transactions.add(TxMsgSerializer.getInstance().deserialize(context, byteReader));

            if (transactions.size() == batchSize) {
                notifyDeserialization(
                    PartialBlockTxnMsg.builder()
                        .blockHash(blockHash)
                        .transactions(new ArrayList<>(transactions))
                        .build()
                );
                transactions.clear();
            }
        }

        if (!transactions.isEmpty()) {
            notifyDeserialization(
                PartialBlockTxnMsg.builder()
                    .blockHash(blockHash)
                    .transactions(transactions)
                    .build()
            );
        }
    }
}
