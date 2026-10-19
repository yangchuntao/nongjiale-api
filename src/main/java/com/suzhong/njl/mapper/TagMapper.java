package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.Tag;
/**
 * 标签字典 Mapper，对应 {@code tag} 表。
 *
 * <p>无自定义 SQL。{@code code} 是筛选参数使用的稳定标识，{@code enabled} 控制标签是否参与
 * 门店筛选；停用标签不删除，已有关联保留。
 */
public interface TagMapper extends BaseMapper<Tag> {
}
