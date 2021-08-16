/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;


import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration needed by the Blacklist Handler
 */
public class BlacklistHandlerConfig extends HandlerConfig {
    private ProtocolBasicConfig basicConfig;

    public BlacklistHandlerConfig(ProtocolBasicConfig basicConfig) {
        this.basicConfig = basicConfig;
    }

    public ProtocolBasicConfig getBasicConfig() {
        return this.basicConfig;
    }

    public BlacklistHandlerConfigBuilder toBuilder() {
        return new BlacklistHandlerConfigBuilder().basicConfig(this.basicConfig);
    }

    public static BlacklistHandlerConfigBuilder builder() {
        return new BlacklistHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlacklistHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;

        BlacklistHandlerConfigBuilder() {}

        public BlacklistHandlerConfig.BlacklistHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public BlacklistHandlerConfig build() {
            return new BlacklistHandlerConfig(basicConfig);
        }
    }
}
