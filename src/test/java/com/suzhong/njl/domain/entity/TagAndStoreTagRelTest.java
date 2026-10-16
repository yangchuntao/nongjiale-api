package com.suzhong.njl.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagAndStoreTagRelTest {

    @Test
    void tagAccessorsRoundTrip() {
        Tag tag = new Tag();
        tag.setId(3L);
        tag.setCode("family");
        tag.setName("亲子");
        tag.setSortOrder(10);
        tag.setEnabled(true);

        assertThat(tag.getId()).isEqualTo(3L);
        assertThat(tag.getCode()).isEqualTo("family");
        assertThat(tag.getName()).isEqualTo("亲子");
        assertThat(tag.getSortOrder()).isEqualTo(10);
        assertThat(tag.getEnabled()).isTrue();
    }

    @Test
    void storeTagRelKeepsTheCompositeKey() {
        StoreTagRel rel = new StoreTagRel();
        rel.setStoreId(8L);
        rel.setTagId(3L);

        assertThat(rel.getStoreId()).isEqualTo(8L);
        assertThat(rel.getTagId()).isEqualTo(3L);
    }
}
