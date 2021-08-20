/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg
import io.bitcoinsv.jcl.net.protocol.messages.NetAddressMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.AddrMsgSerialzer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 05/08/2019 14:33
 *
 * Testing class for the AddrMsg Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class AddrMsgSerialzerSpec extends Specification {

    // This is a AddrMsg Message Serialized for the Main Network in HEX format,
    // generated by a third party (bitcoinJ).
    // The following Serialized value for an Address has been produced using:
    // - Main Network
    // - timestamp: 0
    // - Localhost and standard getPort (8333)

    private static final String REF_ADDRESS_MSG = "010b6d2f5d000000000000000000000000000000000000ffff7f000001208d"
    private static final long REF_TIMESTAMP = 1563389195
    private static final int REF_PORT = 8333
    public static final PeerAddress REF_PEER_ADDRESS = new PeerAddress(InetAddress.getByName("localhost"), REF_PORT)

    private static final String REF_COMPLETE_ADDRESS_MSG = "e3e1f3e86164647200000000000000001f00000077b42c0c010b6d2f5" +
            "d000000000000000000000000000000000000ffff7f000001208d"

    def "Testing AddrMsg Body Deserializing"(int byteInterval, int delayMs) {
        given:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                        .protocolBasicConfig(config.getBasicConfig())
                        .maxBytesToRead((Long) (REF_ADDRESS_MSG.length()/2))
                        .build()
        AddrMsgSerialzer serializer = AddrMsgSerialzer.getInstance()
        AddrMsg address = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_ADDRESS_MSG), byteInterval, delayMs);
        when:
             address = serializer.deserialize(context,byteReader)
        then:
             address.getAddrList().size() == 1
             address.count.value == 1
             address.getAddrList().get(0).timestamp == REF_TIMESTAMP
             address.getAddrList().get(0).getAddress() == REF_PEER_ADDRESS
        where:
            byteInterval | delayMs
                10       |    15

    }


    def "Testing AddrMsg Body Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            AddrMsgSerialzer serializer = AddrMsgSerialzer.getInstance()
            AddrMsg addMessages = buildAddrMsg()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String msgSerializedHex = null
        when:
            serializer.serialize(context, addMessages, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContentAndClose()
            msgSerializedHex = Utils.HEX.encode(messageBytes)
        then:
            msgSerializedHex.equals(REF_ADDRESS_MSG)
    }

    def "testing Address Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()

           AddrMsg addMessages = buildAddrMsg()

        BitcoinMsg<AddrMsg> bitcoinVersionMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), addMessages).build()
        BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            byte[] addrMsgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg, AddrMsg.MESSAGE_TYPE).getFullContent()
            String addrMsgDeserialzed = Utils.HEX.encode(addrMsgBytes)
        then:
             addrMsgDeserialzed.equals(REF_COMPLETE_ADDRESS_MSG)
    }

    private AddrMsg buildAddrMsg() {
        NetAddressMsg netAddrMsg = NetAddressMsg.builder().address(REF_PEER_ADDRESS).timestamp(REF_TIMESTAMP).build();

        // List of addresses:
        List<NetAddressMsg> netAddressMsgs = new ArrayList<NetAddressMsg>();
        netAddressMsgs.add(netAddrMsg)

        AddrMsg addMessages = AddrMsg.builder().addrList(netAddressMsgs).build()
        return addMessages
    }

    def "testing Address Message COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_ADDRESS_MSG.length()/2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_COMPLETE_ADDRESS_MSG), byteInterval, delayMs)
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            BitcoinMsg<AddrMsg> message = bitcoinSerializer.deserialize(context, byteReader, AddrMsg.MESSAGE_TYPE)
        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(AddrMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().getCount().value == 1
            message.getBody().getAddrList().get(0).timestamp == REF_TIMESTAMP
            message.getBody().getAddrList().get(0).getAddress() == REF_PEER_ADDRESS
        where:
            byteInterval | delayMs
                10       |    15
    }

}
