package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Preconditions;

import com.nchain.jcl.net.protocol.messages.common.Message;
import io.bitcoinj.bitcoin.api.base.TxOutput;
import io.bitcoinj.bitcoin.bean.base.TxOutputBean;
import io.bitcoinj.core.Coin;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Transaction Output represent  outputs or destinations for coins and consists of the following fields:
 *
 * -  field: "value" (8 bytes)
 *    Transaction Value.
 *
 * - field: "pk_script length"  VarInt
 *    The length of the pk script.
 *
 * - field: "pk_script"  uchar[]
 *   Usually contains the public key as a Bitcoin script setting up conditions to claim this output.
 */
@Value
@EqualsAndHashCode
public class TxOutputMsg extends Message {
    public static final String MESSAGE_TYPE = "TxOut";
    private static final int txValue_length = 8;

    private long txValue;
    private VarIntMsg pk_script_length;
    private byte[] pk_script;

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Builder
    protected TxOutputMsg(long txValue, byte[] pk_script) {
        this.txValue = txValue;
        this.pk_script = pk_script;
        this.pk_script_length = VarIntMsg.builder().value(pk_script.length).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long length = txValue_length + pk_script_length.getLengthInBytes() + pk_script.length;;
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(pk_script.length  ==  pk_script_length.getValue(), "Script lengths are not same.");
    }

    @Override
    public String toString() {
        return "value: " + txValue + ", scriptLength: " + pk_script_length;
    }

}
