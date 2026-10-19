package com.suzhong.njl.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suzhong.njl.domain.entity.Store;

/**
 * 门店 Mapper，对应 {@code store} 表。
 *
 * <p>含 2 个自定义 SQL 方法，语句写在 {@code resources/mapper/StoreMapper.xml}，两者共用
 * {@code onShelfFilters} 片段，保证「总数」与「当页列表」的过滤口径完全一致。
 * 其余单表增删改查继承自 MyBatis-Plus {@code BaseMapper}。
 */
public interface StoreMapper extends BaseMapper<Store> {

    /**
     * 查询当页上架门店。
     *
     * <p>上架状态、地区集合、标签、价格带、店名关键词全部下推到 SQL，不在内存里二次过滤；
     * 排序使用 XML 里的 ORDER BY 变量拼接，因此 {@code query.orderBy} 只能由
     * {@code StoreSearch.applySort} 用枚举白名单写入，不能塞外部原始参数。
     *
     * @param query 查询条件载体，limit/offset 已由分页参数换算；regionIds 为空表示不限地区
     * @return 当页门店，无匹配时为空列表，不会返回 null
     */
    List<Store> searchOnShelf(StoreSearch query);

    /**
     * 统计与列表相同条件下的上架门店总数。
     *
     * <p>先 count：总数为 0 时调用方不必再查明细，省一次 SQL；limit/offset 不参与统计。
     *
     * @param query 查询条件载体，过滤字段与 {@code searchOnShelf} 一致
     * @return 匹配总数，无匹配时为 0
     */
    long countOnShelf(StoreSearch query);
}
