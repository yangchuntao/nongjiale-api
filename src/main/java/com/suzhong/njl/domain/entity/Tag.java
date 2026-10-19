package com.suzhong.njl.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 门店标签，对应 {@code tag} 表。
 *
 * <p>用于门户标签订阅筛选（如「河鲜」「亲子」），由运营维护；门店通过
 * {@code store_tag_rel} 关联标签。停用标签不再参与筛选和展示，但历史关联保留。
 */
@TableName("tag")
public class Tag {

    /** 主键，自增；被 {@code store_tag_rel.tag_id} 引用。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签英文码，全局唯一，最长 64 字，如 {@code river-food}；门户筛选参数按此码匹配。 */
    private String code;

    /** 标签中文名，非空，最长 64 字，如「河鲜」。 */
    private String name;

    /** 展示顺序，非空，默认 0，升序排列。 */
    private Integer sortOrder;

    /** 是否启用，非空，默认 {@code true}；停用后不出现在筛选结果中。 */
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
