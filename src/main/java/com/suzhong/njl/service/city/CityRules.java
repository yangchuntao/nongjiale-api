package com.suzhong.njl.service.city;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;

/**
 * 开城规则的纯函数集合（城市可见性）。
 *
 * <p>职责：把「怎样算开通」「哪些内容计入开城门槛」「缺项文案」「意向归属哪个市」这些判断集中到一处，
 * 供门户读路径与运营开城服务共用，避免两边各写一套后口径漂移。
 *
 * <p>关键约定：
 * <ul>
 *   <li>开通单位只有市（{@code region.level = CITY}），省不设开通态，区县不单独开城；</li>
 *   <li>库内 {@code open_status} 为 NULL 与 {@code CLOSED} 同义，读路径统一按未开通处理；</li>
 *   <li>本类不查库、不缓存、不抛业务异常，所有入参由调用方准备。</li>
 * </ul>
 *
 * <p>规则来源：{@code docs/TECH-DESIGN-MVP-1-开城.md} 的「①概要设计」「计数」「开通 / 关城」。
 */
public final class CityRules {

    /** 运营手改②③时的拒绝文案；PATCH checklist 带这两个字段要整体拒绝，不能静默忽略。 */
    public static final String AUTO_FIELD_MESSAGE = "供给达标与攻略达标按实时计数自动勾选，不能手改";

    /** 门户店列表在目的地未开通时的空态提示，与 {@code destinationStatus=CLOSED} 配套。 */
    public static final String EMPTY_HINT = "该目的地暂未开通";

    /** 供给门槛 N 的兜底值：配置缺失、空串或非正整数时使用，见配置 {@code open_city.min_stores}。 */
    public static final int DEFAULT_MIN_STORES = 5;

    /** 攻略门槛 M 的兜底值：配置缺失、空串或非正整数时使用，见配置 {@code open_city.min_articles}。 */
    public static final int DEFAULT_MIN_ARTICLES = 1;

    private CityRules() {
    }

    /**
     * 把库内的可空开通态归一成枚举。
     *
     * <p>设计约定 NULL 与 CLOSED 等价：省、区县以及未初始化的市都按未开通对外呈现。
     *
     * @param status 库内读出的开通态，可为 null
     * @return 非 null 的开通态
     */
    public static RegionOpenStatus effective(RegionOpenStatus status) {
        return status == null ? RegionOpenStatus.CLOSED : status;
    }

    /**
     * 判断一个地区节点本身是否构成「已开通的市」。
     *
     * <p>三个条件缺一不可：层级是市、{@code enabled=true} 仍可维护、{@code open_status=OPEN}。
     * 区县与省即使误填了 OPEN 也不认，避免脏数据把整片区域带进门户。
     *
     * @param region 地区节点，可为 null
     * @return 是已开通的市返回 true
     */
    public static boolean isOpen(Region region) {
        return region != null
                && region.getLevel() == RegionLevel.CITY
                && Boolean.TRUE.equals(region.getEnabled())
                && region.getOpenStatus() == RegionOpenStatus.OPEN;
    }

    /**
     * 把配置里的正整数字符串解析成门槛值，坏配置回退到默认值。
     *
     * <p>缺行、空串、非数字、0 或负数都按 fallback 处理，目的是不让一条坏配置把开通接口打成 500。
     *
     * @param raw      配置原文，可为 null
     * @param fallback 解析失败时的兜底值
     * @return 解析出的值，或兜底值
     */
    public static int positiveOrDefault(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * 计算开通前的缺项列表。
     *
     * <p>顺序是固定契约（目的地 / 供给 / 攻略 / 客服 / 测数），前端与验收按这个顺序展示；
     * 只放未满足项，全部满足时返回空列表。
     *
     * @param destinationReady  ①目的地数据是否已齐（手勾）
     * @param supportCovered    ④客服覆盖是否已说明（手勾）
     * @param dirtyDataCleared  ⑤测数或脏数据是否已确认清理（手勾）
     * @param storeCount        实时供给计数（只算已开通市范围内、非测试、上架且地区启用的店）
     * @param articleCount      实时攻略计数
     * @param minStores         生效的供给门槛 N
     * @param minArticles       生效的攻略门槛 M
     * @return 不可变的缺项文案列表
     */
    public static List<String> missingReasons(
            boolean destinationReady,
            boolean supportCovered,
            boolean dirtyDataCleared,
            long storeCount,
            long articleCount,
            int minStores,
            int minArticles) {
        List<String> reasons = new ArrayList<>();
        // 顺序即契约：运营看板的缺项列表和开通 409 文案都按这个顺序展示
        if (!destinationReady) {
            reasons.add("目的地数据未齐");
        }
        if (storeCount < minStores) {
            reasons.add("供给未达标（当前 " + storeCount + " / 需要 " + minStores + "）");
        }
        if (articleCount < minArticles) {
            reasons.add("攻略未达标（当前 " + articleCount + " / 需要 " + minArticles + "）");
        }
        if (!supportCovered) {
            reasons.add("客服覆盖未说明");
        }
        if (!dirtyDataCleared) {
            reasons.add("测数或脏数据未确认清理");
        }
        return List.copyOf(reasons);
    }

    /**
     * 拼出 409 的缺项文案，缺项之间用中文分号连接。
     *
     * @param reasons {@link #missingReasons} 的结果，不应为空
     * @return 形如「未达标，不能开通：供给未达标（当前 1 / 需要 5）」的完整句子
     */
    public static String openRejectedMessage(List<String> reasons) {
        return "未达标，不能开通：" + String.join("；", reasons);
    }

    /**
     * 意向归属的市。市本身，或区县的父市。挂省、地区缺失、父节点不是市，则没有归属。
     *
     * <p>用于看板汇总：没有归属的意向既不进任何市行，也不进合计。
     *
     * @param region 意向所属门店的地区，可为 null
     * @param byId   地区索引，可为 null（此时只有市本身能归属）
     * @return 市 ID；无法归属时返回 null
     */
    public static Long owningCityId(Region region, Map<Long, Region> byId) {
        if (region == null || region.getLevel() == null) {
            return null;
        }
        if (region.getLevel() == RegionLevel.CITY) {
            return region.getId();
        }
        if (region.getLevel() == RegionLevel.DISTRICT && region.getParentId() != null && byId != null) {
            Region parent = byId.get(region.getParentId());
            if (parent != null && parent.getLevel() == RegionLevel.CITY) {
                return parent.getId();
            }
        }
        return null;
    }

    /**
     * 店或攻略是否计入该市门槛。地区必须启用，且挂在该市或父节点是该市的区县上。
     *
     * <p>直接挂省的店不计入任何市；{@code test_data} 的过滤由调用方另行处理。
     *
     * @param region 店或攻略所属地区，可为 null
     * @param cityId 目标市 ID
     * @return 计入该市返回 true
     */
    public static boolean countsTowardCity(Region region, long cityId) {
        if (region == null || !Boolean.TRUE.equals(region.getEnabled()) || region.getLevel() == null) {
            return false;
        }
        if (region.getLevel() == RegionLevel.CITY) {
            return region.getId() != null && region.getId() == cityId;
        }
        if (region.getLevel() == RegionLevel.DISTRICT) {
            // 区县只认父节点恰好是目标市的情况；挂省或父节点缺失都不计入
            return region.getParentId() != null && region.getParentId() == cityId;
        }
        // 省节点（以及未知层级）不计入任何市
        return false;
    }
}
