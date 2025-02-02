package io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.bsvcl.common.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * Event triggered when an Error has been triggered during a Large Message Deserialization
 */
public class MsgPartDeserializationErrorEvent extends Event {
    private Exception exception;

    public MsgPartDeserializationErrorEvent(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return this.exception;
    }
}
