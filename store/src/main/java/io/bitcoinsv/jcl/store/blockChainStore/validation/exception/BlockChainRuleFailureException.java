/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore.validation.exception;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 */
public class BlockChainRuleFailureException extends Exception {

    public BlockChainRuleFailureException(String s) { super(s); }
    public BlockChainRuleFailureException(String s, Throwable throwable) { super(s, throwable); }
}