package com.suzhong.njl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.Article;
/**
 * 攻略 Mapper，对应 {@code article} 表。
 *
 * <p>无自定义 SQL。门户只取已发布攻略，过滤与排序在 Service 中用条件构造器拼装，因此这里不重复
 * 声明查询方法；直链详情即使所属城市已关闭仍可读。
 */
public interface ArticleMapper extends BaseMapper<Article> {
}
