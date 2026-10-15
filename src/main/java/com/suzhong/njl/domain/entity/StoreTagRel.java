package com.suzhong.njl.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 联合主键 (store_id, tag_id)。不要用 BaseMapper 的按单主键方法。
 */
@TableName("store_tag_rel")
public class StoreTagRel {

    private Long storeId;

    private Long tagId;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

}
