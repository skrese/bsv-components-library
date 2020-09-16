package com.nchain.jcl.base.domain.api.extended;

import com.nchain.jcl.base.domain.api.BitcoinObject;
import com.nchain.jcl.base.domain.bean.extended.BlockMetaBean;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Due to the fact that Block  might be very big in size, it's not possibel to hold all their information into
 * memory. So differnet classes might be defined to store different pieces of information or summary of a block,
 * without storing the whole structure.
 * A BlockMata is a data structure that returns the number of TXs and the Block size of a block.
 */
public interface BlockMeta extends BitcoinObject {
    int getTxCount();
    long getBlockSize(); // NOT used.

    // Convenience methods to get a reference to the Builder
    static BlockMetaBean.BlockMetaBeanBuilder builder() { return BlockMetaBean.builder(); }
    default BlockMetaBean.BlockMetaBeanBuilder toBuilder() { return ((BlockMetaBean) this).toBuilder();}
}
