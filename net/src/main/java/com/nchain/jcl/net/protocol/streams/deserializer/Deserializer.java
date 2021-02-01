package com.nchain.jcl.net.protocol.streams.deserializer;

import com.google.common.cache.*;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayReaderOptimized;
import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MsgSerializersFactory;
import com.nchain.jcl.net.protocol.serialization.largeMsgs.LargeMessageDeserializer;
import com.nchain.jcl.net.protocol.serialization.largeMsgs.MsgPartDeserializationErrorEvent;
import com.nchain.jcl.net.protocol.serialization.largeMsgs.MsgPartDeserializedEvent;
import lombok.*;

import java.util.function.Consumer;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class implements the De-Serialziation of the Messages coming down the wire. It takes care of de-serializing
 * both normal messages and BIG Messages ( A Big Message is a messafe which size in bytes is loonger than a value
 * specified in the "msgSizeInBytesForRealTimeProcessing" roperty in the Runtime configuration)
 *
 * This class just uses the "MsgSerializersFactory" to get an instance of a Serializer, deserializes and returns
 * the result. That serialzier might be a "regular serializer or a "Large" Serialzier, depending on the Size of them
 * Message. The way Deserialziation works is alittle different:
 *
 * - If a message is small, then you muns tse the "deserialize" method. This method will block until the message
 * content is deserialized and returned.
 *
 * - If a message is big, you must use the "deserializeLarge" method. This method will block, and the result will
 * be returned in baches by invoking the callbacks that are also fed to this method.
 *
 * NOTE: For small messages, this class uses an internal CACHE where those messages are stored and reused. This cache
 * is implemented using the GUAVA CACHE Api. The parameters to determin wether a message is 2cacheable" or not (not all
 * of them are) is determiend by the DEserialzierConfig class.
 *
 */
public class Deserializer {

    /**
     * Key of the Items stored in the Cache for mall messages.
     * It contain several fields, but only the CHECKSUM field is used as a Key (that's why the 'Equals' and 'hasCode'
     * only use that field). The rest of fields are needed to "deserialize" the items into the cache when they are not there
     * (in this case "loading" an item means to deserialize it, so we need the Deserialization Context, the
     * byteArrayReader and the Header of the Message we are about to deserialize...
     */
    @AllArgsConstructor
    class CacheMsgKey {
        private HeaderMsg headerMsg;
        private DeserializerContext desContext;
        private ByteArrayReader reader;

        @Override public boolean equals(Object obj) {
            return (obj != null) && (headerMsg.getChecksum() == (((CacheMsgKey) obj).headerMsg.getChecksum()));
        }
        @Override public int hashCode() { return Long.hashCode(headerMsg.getChecksum()); }
    }

    private Cache<CacheMsgKey, Message> cache;


    // Object instance (Singleton) and configuration:
    private static Deserializer instance;
    private RuntimeConfig runtimeConfig;
    private DeserializerConfig config;


    /** Constructor */
    protected Deserializer(RuntimeConfig runtimeConfig, DeserializerConfig config) {
        this.runtimeConfig = runtimeConfig;
        this.config = config;

        // Guava Cache Configuration:
        Weigher<CacheMsgKey, Message> weigher = (k, v) -> (int) k.headerMsg.getLength();
        CacheBuilder cacheBuilder = CacheBuilder.newBuilder()
                .maximumWeight(config.getMaxCacheSizeInBytes())
                .weigher(weigher);
        if (config.isGenerateStats()) cacheBuilder.recordStats();
        this.cache = cacheBuilder.<CacheMsgKey, Message>build();
    }

    /**
     * It returns a single instance of this Class (Singleton)
     */
    public static Deserializer getInstance(RuntimeConfig runtimeConfig, DeserializerConfig config) {
        if (instance == null) {
            synchronized (Deserializer.class) {
                instance = new Deserializer(runtimeConfig, config);
            }
        }
        return instance;
    }

    /** Expensive Operation. It deserializes a Message from the pipeline using the Bitcoin Serializers */
    private Message deserialize(CacheMsgKey key) {
        //System.out.println("DESERIALIZING " + key.headerMsg.getCommand() + " !!!!!!!!!");
        return MsgSerializersFactory.getSerializer(key.headerMsg.getCommand()).deserialize(key.desContext, key.reader);
    }

    /**
     * Deserializes and returns the Next Message form the pipeline. If the incoming message is cacheable, it tries to
     * look it up from the cache, if its not there it deserializes the Message normally and adds it to the cache for
     * further retrieves.
     *
     * This method assumes that the Header of the next incoming Message has been read already from the pipeline, so
     * the next bytes to read are related to the BODY of the Message...
     *
     * @param headerMsg     HeaderMsg of the next Message coming in the pipeline
     * @param desContext    Deserializer context to use on the Deserialization
     * @param reader        pipeline of incoming messages
     * @return              The next BODY of the message after deserialization
     * @throws Exception
     */
    public Message deserialize(HeaderMsg headerMsg, DeserializerContext desContext, ByteArrayReader reader) throws Exception {
        Message result = null;

        // We build the Cache-Key for this Message...
        CacheMsgKey key = new CacheMsgKey(headerMsg, desContext, reader);

        // We only use the Cache if the requested message is "cacheable"...
        boolean isCacheable = config.getMessagesToCache().contains(headerMsg.getCommand().toUpperCase()) &&
                (headerMsg.getLength() < config.getMaxMsgSizeInBytes());

        if (isCacheable) {
            result = cache.getIfPresent(key);
            if (result != null)
                // we extract and discard the bytes from the pipeline...
                reader.extract((int) headerMsg.getLength());
            else
                result = cache.get(key, () -> deserialize(key));
        }
        else result = deserialize(key);

        return result;
    }

    /**
     * Deserializes and returns the Next BIG Message form the pipeline. Since the message is supposed to be large, no
     * Cahce is used here, and this method will use directly a "LargeMessageDeserializer". According to how the large
     * messages deserialization works, the message is returned in different "batches", populated and sent back to the
     * "client" by calling the callbacks fed into this method.
     *
     * This method assumes that the Header of the next incoming Message has been read already from the pipeline, so
     * the next bytes to read are related to the BODY of the Message...
     *
     * @param headerMsg                 HeaderMsg of the next Message coming in the pipeline
     * @param desContext                Deserializer context to use on the Deserialization
     * @param reader                    pipeline of incoming messages
     * @param onErrorHandler            a Callback triggered when an Error happens during De-serialization
     * @param onPartDeserializedHandler a Callback triggered when a partial part of the message is deserialized and returned
     * @throws Exception
     */
    public Message deserializeLarge(HeaderMsg headerMsg, DeserializerContext desContext, ByteArrayReader reader,
                                    Consumer<MsgPartDeserializationErrorEvent> onErrorHandler,
                                    Consumer<MsgPartDeserializedEvent> onPartDeserializedHandler) throws Exception {
        Message result = null;

        // we adjust the Reader, using an version Optimized for Large Messages:
        reader = new ByteArrayReaderOptimized(reader);
        reader.enableRealTime(runtimeConfig.getMaxWaitingTimeForBytesInRealTime());

        // We set up the callbacks that wil be trigger as the message is deserialized...

        LargeMessageDeserializer largeMsgDeserializer =  MsgSerializersFactory.getLargeMsgDeserializer(headerMsg.getCommand());
        largeMsgDeserializer.onError(onErrorHandler);
        largeMsgDeserializer.onDeserialized(onPartDeserializedHandler);

        // Adn we run it. This call is blocking
        largeMsgDeserializer.deserialize(desContext, reader);

        // we reset the Reader now that it's done... (needed by the OptimizedReader)
        ((ByteArrayReaderOptimized) reader).refreshBuffer();
        return result;
    }

    /** It returns the current State of the Cache */
    public DeserializerState getState() {
        CacheStats cacheStats = cache.stats();
        DeserializerState result = DeserializerState.builder()
                .numHits(cacheStats.hitCount())
                .numLoads(cacheStats.loadCount())
                .hitRatio(cacheStats.hitRate())
                .build();
        return result;
    }
}