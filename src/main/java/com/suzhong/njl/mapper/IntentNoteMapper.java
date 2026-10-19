package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.IntentNote;
/**
 * 意向备注 Mapper，对应 {@code intent_note} 表。
 *
 * <p>无自定义 SQL。备注只追加不改写，运营每次跟进写一条，按意向 ID 汇总展示。
 */
public interface IntentNoteMapper extends BaseMapper<IntentNote> {
}
