package com.nchain.jcl.net.unit.protocol.streams.deserializer

import com.nchain.jcl.base.tools.bytes.ByteArrayReader
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter
import com.nchain.jcl.base.tools.config.RuntimeConfig
import com.nchain.jcl.base.tools.config.provided.RuntimeConfigDefault
import com.nchain.jcl.net.network.PeerAddress
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.HeaderMsg
import com.nchain.jcl.net.protocol.messages.HeadersMsg
import com.nchain.jcl.net.protocol.messages.NetAddressMsg
import com.nchain.jcl.net.protocol.messages.VarStrMsg
import com.nchain.jcl.net.protocol.messages.VersionMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.messages.common.Message
import com.nchain.jcl.net.protocol.serialization.HeaderMsgSerializer
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerConfig
import com.nchain.jcl.net.protocol.streams.deserializer.Deserializer
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerState
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.MsgSerializersFactory
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * Testing class for the DeserializerCache.
 */
class DeserializerSpec extends Specification {


    @FunctionalInterface
    interface DeserializerInterface {
        Message deserilize(DeserializerContext desContext, HeaderMsg headerMSg, ByteArrayReader reader);
    }


    /*
        Convenience method that takes a Message (freshly created supposedly), and return the same Message after setting
        the value of the "checksum" value in the Header.
     */
    private BitcoinMsg updateChecksum(ProtocolBasicConfig protocolConfig,  BitcoinMsg message) {
        String command = message.header.command
        SerializerContext serContext = SerializerContext.builder().build()

        BitcoinMsgSerializer serializer = new BitcoinMsgSerializerImpl()
        ByteArrayReader reader = serializer.serialize(serContext, message, command)

        DeserializerContext desContext = DeserializerContext.builder()
                .protocolBasicConfig(protocolConfig)
                .insideVersionMsg(command.equalsIgnoreCase(VersionMsg.MESSAGE_TYPE))
                .build()
        BitcoinMsg result = serializer.deserialize(desContext, reader, command)
        return result
    }

    /*
        Convenience method that creates a new VERSION Message.
     */
    private BitcoinMsg<VersionMsg> buildVersionMsg(ProtocolBasicConfig protocolBasicConfig) {
        VersionMsg versionMsg = VersionMsg.builder()
                .version(1)
                .nonce(2)
                .services(3)
                .start_height(4)
                .relay(true)
                .addr_from(NetAddressMsg.builder().address(PeerAddress.localhost(80)).services(90).build())
                .addr_recv(NetAddressMsg.builder().address(PeerAddress.localhost(81)).services(91).build())
                .user_agent(VarStrMsg.builder().str("testing").build())
                .build()

        BitcoinMsg<VersionMsg> result = new BitcoinMsgBuilder<>(protocolBasicConfig, versionMsg).build()
        result = updateChecksum(protocolBasicConfig, result)
        return result
    }

    /*
        Convenience method that creates a new HEADERS Message containn a list of BlockHeaders
     */
    private BitcoinMsg<HeadersMsg> buildHeadersMsg(ProtocolBasicConfig protocolBasicConfig, int numHeaders) {
        List<BlockHeaderMsg> headersList = new ArrayList<>()
        for (int i = 0; i < numHeaders; i++) {
            BlockHeaderMsg headerMsg = BlockHeaderMsg.builder()
                .nonce(i)
                .version(i)
                .difficultyTarget(i)
                .transactionCount(i)
                .prevBlockHash(HashMsg.builder().hash(new byte[32]).build())
                .merkleRoot(HashMsg.builder().hash(new byte[32]).build())
                .build()
            headersList.add(headerMsg)
        }
        HeadersMsg headersMsg = HeadersMsg.builder()
            .blockHeaderMsgList(headersList)
            .build()
        BitcoinMsg<HeadersMsg> result = new BitcoinMsgBuilder<>(protocolBasicConfig, headersMsg).build()
        result = updateChecksum(protocolBasicConfig, result)
        return result
    }

    /*
        This method simulates the same work that is being done by the JCL-NEt package when it comes to deserializarion:
        it takes a pipeline of incoming messages (represented here by a ByteArrayReader containing the bytes of those
        messages, and deserializing them, returning a List of Messages.

        The process is always the same:
         - first we read the HEADER, so we know WHAT message is coming next
         - then we DESERIALIZE the Body, Here is where we can do it either WITH Cache or WITHOUT Cache. In this method,
            that part is generic and encapsulated in a BiConsumer function.

     */
    private List<BitcoinMsg> deserialize( ProtocolBasicConfig protocolConfig, ByteArrayReader reader, DeserializerInterface deserializer) {
        List<BitcoinMsg> result = new ArrayList<>()
        DeserializerContext desContext = DeserializerContext.builder().protocolBasicConfig(protocolConfig).build()

        while (reader.size() > 0) {
            //println("Bytes left in reader: " + reader.size())
            // We extract the Header:
            HeaderMsg headerMsg = HeaderMsgSerializer.getInstance().deserialize(desContext, reader)
            //println("- after Header: " + reader.size() + ". reading " + headerMsg.length + " next...")
            // We look the Body in the Cache...
            desContext = desContext.toBuilder()
                    .maxBytesToRead(headerMsg.length)
                    .insideVersionMsg(headerMsg.command.toUpperCase().equals(VersionMsg.MESSAGE_TYPE.toUpperCase()))
                    .build()
            Message bodyMsg = deserializer.deserilize (desContext, headerMsg, reader)
            //println("- after Body: " + reader.size())
            BitcoinMsg btcMsg = new BitcoinMsg(headerMsg, bodyMsg)
            result.add(btcMsg)
        }
        return result
    }


    /*
        This method takes a Pipeline (represented by a byte reader that contains the bytes of the incoming Messages),
        and Deserializes its content by using the DeserializerCache.
    */
    private List<BitcoinMsg> deserializeWithCache(ProtocolBasicConfig protocolConfig, ByteArrayReader reader, Deserializer cache) {
        List<BitcoinMsg> result = deserialize(protocolConfig, reader,
                {context, header, pipeline -> cache.deserialize(header, context, pipeline)})
        return result
    }

    /*
        This method takes a Pipeline (represented by a byte reader that contains the bytes of the incoming Messages),
        and Deserializes its content WITHOUT using the DeserializerCache.
        This method is used to compare the time taken by both approach (wiht and without Deserialziation Cache) in
        some tests
     */
    private List<BitcoinMsg> deserializeWithoutCache( ProtocolBasicConfig protocolConfig, ByteArrayReader reader) {
        List<BitcoinMsg> result = deserialize(protocolConfig, reader,
                {context, header, pipeline -> MsgSerializersFactory.getSerializer(header.command).deserialize(context, pipeline)})
        return result
    }

    /*
        this method takes a List of Messages and serializes them into a Pipeline of bytes, a ByteArrayReader that
        can be used to READ bytes form it, as it's being done in the JCL-Net Module.
     */
    private ByteArrayReader serializeMessages(List<BitcoinMsg> msgs) {
        SerializerContext serContext = SerializerContext.builder().build()
        BitcoinMsgSerializer serializer = new BitcoinMsgSerializerImpl()
        ByteArrayWriter writer = new ByteArrayWriter()
        for (BitcoinMsg btcMsg : msgs) {
            byte[] btcMsgBytes = serializer.serialize(serContext, btcMsg, btcMsg.header.command).getFullContentAndClose()
            writer.write(btcMsgBytes)
        }
        return writer.reader()
    }


    /**
     * We test that the Deserialization performed by the Cache is the SAME as the one performed WITHOUT
     * Cache...
     */
    def "testing Deserialization content"() {
        // All the messages are Equals
        int NUM_MSGS = 10
        given:
            // we simulate Messages coming from the BSV Network...
            ProtocolBasicConfig protocolConfig = new ProtocolBSVMainConfig().basicConfig

            // This is the list of "dummy" messages coming down the wire:
            List<BitcoinMsg> msgsFromP2P = new ArrayList<>()
            for (int i = 0; i < NUM_MSGS; i++) msgsFromP2P.add(buildHeadersMsg(protocolConfig, 1000))

            // We initialize the Cache, enabling stats generation...
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            DeserializerConfig cacheConfig = DeserializerConfig.builder().generateStats(true).build()
            Deserializer cache = new Deserializer(runtimeConfig, cacheConfig)
        when:
            // We "simulate" an incoming flow of messages with a ByteReader containing the messages in raw format
            ByteArrayReader reader = serializeMessages(msgsFromP2P)

            // Now we simulate the same process as it's being done by the JCL-Net, that is we deserialize the incoming
            // Messages from the Reader, but this time using the Cache, and we return them...
            List<BitcoinMsg> messagesFromCache = deserializeWithCache(protocolConfig, reader, cache)

            // Now we check that the Messages searched are EQUALS to the BODIES of the Original Messages, adn the stats:
            boolean messagesOK = msgsFromP2P.equals(messagesFromCache)
            DeserializerState cacheState = cache.getState()

        then:
            messagesOK
            cacheState.numLoads == 1
            cacheState.numHits == NUM_MSGS - 1
    }


    /**
     * We compare the time taken on deserialization with and without cache. The test passes as long as the time WITH
     * cache is SHORTER thatn the time WITHOUT it.
     */
    def "Comparing with and without Cache"() {
        // All the messages are Equals
        int NUM_MSGS = 10
        given:
            // we simulate Messages coming from the BSV Network...
            ProtocolBasicConfig protocolConfig = new ProtocolBSVMainConfig().basicConfig

            // This is the list of "dummy" messages coming down the wire:
            List<BitcoinMsg> msgsFromP2P = new ArrayList<>()
            for (int i = 0; i < NUM_MSGS; i++) msgsFromP2P.add(buildHeadersMsg(protocolConfig, 1000))

            // We initialize the Cache, enabling stats generation...
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            DeserializerConfig cacheConfig = DeserializerConfig.builder().generateStats(true).build()
            Deserializer cache = new Deserializer(runtimeConfig, cacheConfig)
        when:
            // We "simulate" an incoming flow of messages with a ByteReader containing the messages in raw format
            // Since the reader is consumed after the deserialization, we create 2 readers, one per each process.
            ByteArrayReader readerForNoCache = serializeMessages(msgsFromP2P)
            ByteArrayReader readerForCache = serializeMessages(msgsFromP2P)

            // We measure times WITHOUT Cache...

            Instant beginTime = Instant.now()
            List<BitcoinMsg> messagesDeserialized = deserializeWithoutCache(protocolConfig, readerForNoCache)
            Duration withoutCacheTime = Duration.between(beginTime, Instant.now())
            println("WITHOUT Cache: " + withoutCacheTime.toMillis() + " millis to deserialize " + msgsFromP2P.size() + " messages")

            // We measure times WITH Cache...

            beginTime = Instant.now()
            List<BitcoinMsg> messagesFromCache = deserializeWithCache(protocolConfig, readerForCache, cache)
            Duration withCacheTime = Duration.between(beginTime, Instant.now())
            println("WITH Cache: " + withCacheTime.toMillis() + " millis to deserialize " + msgsFromP2P.size() + " messages")

        then:
            // The time WITH Cache should be SHORTER...
            withCacheTime.compareTo(withoutCacheTime) < 0
    }
}