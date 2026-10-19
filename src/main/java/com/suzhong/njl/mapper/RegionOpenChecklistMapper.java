package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.RegionOpenChecklist;

/**
 * 开城检查单 Mapper，对应 {@code region_open_checklist} 表。
 *
 * <p>无自定义 SQL。实体带 {@code @Version} 乐观锁；供给与攻略达标字段由实时计数回填，
 * 运营只能手改三项人工项，开通/关城以检查单为门槛。
 */
public interface RegionOpenChecklistMapper extends BaseMapper<RegionOpenChecklist> {
}
