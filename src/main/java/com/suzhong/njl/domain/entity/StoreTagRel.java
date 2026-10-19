package com.suzhong.njl.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 门店与标签的关联，对应 {@code store_tag_rel} 表。
 *
 * <p>多对多关系表，由运营或商家在维护门店标签时写入；门户按标签码筛选门店时通过
 * 本表关联 {@code tag.code}，且只认启用标签。
 *
 * <p>联合主键 (store_id, tag_id)，不要用 BaseMapper 的按单主键方法。
 */
@TableName("store_tag_rel")
public class StoreTagRel {

    /** 门店 ID，对应 {@code store.id}，联合主键之一，非空。 */
    private Long storeId;

    /** 标签 ID，对应 {@code tag.id}，联合主键之一，非空。 */
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
