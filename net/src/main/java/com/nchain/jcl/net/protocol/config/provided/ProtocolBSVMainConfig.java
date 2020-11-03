package com.nchain.jcl.net.protocol.config.provided;


import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.net.protocol.config.*;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Stores the Configuration needed to run the Default P2P Handlers in the BSV Main Network.
 */
@Getter
public class ProtocolBSVMainConfig extends ProtocolConfigImpl implements ProtocolConfig {

    // Specific values for BSV Main Net:

    private static String id                   = "BSV [main Net]";
    private static long magicPackage           = 0xe8f3e1e3L;
    private static int services                = ProtocolServices.NODE_BLOOM.getProtocolServices();
    private static int port                    = 8333;
    private static int protocolVersion         = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
    private static String[] userAgentBlacklist = new String[] {"Bitcoin ABC:", "BUCash:" };
    private static String[] userAgentWhitelist = new String[] {"Bitcoin SV:", HandshakeHandlerConfig.DEFAULT_USER_AGENT };
    private static String[] dns                = new String[] {
            "seed.bitcoinsv.io",
            "seed.cascharia.com",
            "seed.satoshivision.network"
    };

    // Genesis Block for BSV-Main:
    private static BlockHeader genesisBlock = BlockHeader.builder()
            .difficultyTarget(0x1d00ffffL)
            .nonce(2083236893)
            .time(1231006505L)
            .build();
    // Basic Configuration:
    private static ProtocolBasicConfig basicConfig = ProtocolBasicConfig.builder()
            .id(id)
            .magicPackage(magicPackage)
            .servicesSupported(services)
            .port(port)
            .protocolVersion(protocolVersion)
            .dns(dns)
            .userAgentBlacklist(userAgentBlacklist)
            .userAgentWhitelist(userAgentWhitelist)
            .build();

    /** Constructor */
    public ProtocolBSVMainConfig() {
        super( null,
                null,
                null,
                genesisBlock,
                basicConfig,
                null,            // Default Network Config
                null,           // Default Message Config
                null,          // Default Gandshake Config
                null,           // Default PingPong Config
                null,           // Default Discovery Config
                null,            // Default Blacklist Config
                null);    // Default Block Downloader Config

    }

    @Override
    public String getId() { return id;}
}
