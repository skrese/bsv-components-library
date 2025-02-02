package io.bitcoinsv.bsvcl.net.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
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
    public static final io.bitcoinsv.bsvcl.net.network.PeerAddress REF_PEER_ADDRESS = new io.bitcoinsv.bsvcl.net.network.PeerAddress(InetAddress.getByName("localhost"), REF_PORT)

    private static final String REF_COMPLETE_ADDRESS_MSG = "e3e1f3e86164647200000000000000001f00000077b42c0c010b6d2f5" +
            "d000000000000000000000000000000000000ffff7f000001208d"

    def "Testing AddrMsg Body Deserializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                        .protocolBasicConfig(config.getBasicConfig())
                        .maxBytesToRead((Long) (REF_ADDRESS_MSG.length()/2))
                        .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.AddrMsgSerialzer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.AddrMsgSerialzer.getInstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg address = null
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
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.AddrMsgSerialzer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.AddrMsgSerialzer.getInstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg addMessages = buildAddrMsg()
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
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()

           io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg addMessages = buildAddrMsg()

            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg> bitcoinVersionMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(config.getBasicConfig(), addMessages).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            byte[] addrMsgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg).getFullContent()
            String addrMsgDeserialzed = Utils.HEX.encode(addrMsgBytes)
        then:
             addrMsgDeserialzed.equals(REF_COMPLETE_ADDRESS_MSG)
    }

    private io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg buildAddrMsg() {
        io.bitcoinsv.bsvcl.net.protocol.messages.NetAddressMsg netAddrMsg = io.bitcoinsv.bsvcl.net.protocol.messages.NetAddressMsg.builder().address(REF_PEER_ADDRESS).timestamp(REF_TIMESTAMP).build();

        // List of addresses:
        List<io.bitcoinsv.bsvcl.net.protocol.messages.NetAddressMsg> netAddressMsgs = new ArrayList<io.bitcoinsv.bsvcl.net.protocol.messages.NetAddressMsg>();
        netAddressMsgs.add(netAddrMsg)

        io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg addMessages = io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg.builder().addrList(netAddressMsgs).build()
        return addMessages
    }

    def "testing Address Message COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .maxBytesToRead((long) (REF_ADDRESS_MSG.length()/2))
                    .build()
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_COMPLETE_ADDRESS_MSG), byteInterval, delayMs)
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer bitcoinSerializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl()
        when:
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg> message = bitcoinSerializer.deserialize(context, byteReader)
        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(io.bitcoinsv.bsvcl.net.protocol.messages.AddrMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().getCount().value == 1
            message.getBody().getAddrList().get(0).timestamp == REF_TIMESTAMP
            message.getBody().getAddrList().get(0).getAddress() == REF_PEER_ADDRESS
        where:
            byteInterval | delayMs
                10       |    15
    }

}
