package com.suzhong.njl.service.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.DestinationStatus;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.ApiException;

/**
 * 城市可见性判定（门户读路径）。
 *
 * <p>职责：把「这个地区对客能不能看 / 能不能订」「列表该查哪些地区」「攻略能不能主推」三类判断
 * 收敛到本类，控制器与服务只消费结果。规则源为 {@code docs/TECH-DESIGN-MVP-1-开城.md}
 * 的「门户」与「缓存」两节。
 *
 * <p>关键约束：
 * <ul>
 *   <li>开通单位是市；区县跟随父市，父市未开通则区县一律不可见；省不设开通态；</li>
 *   <li>两个入口语义不同：{@link #bookableRegion(Long)} 用于详情/下单的单点判断（失败由调用方抛 410），
 *       {@link #listScope(Long)} 用于列表范围（未开通时返回 {@code CLOSED} 空范围而不是抛错）；</li>
 *   <li>本类每次调用都直接查 {@code region} 全表，不走 Redis，因此开城/关城后立刻生效，
 *       不依赖缓存失效是否成功；</li>
 *   <li>只读，无事务。{@code enabled=false} 的地区视为不可维护也不可看。</li>
 * </ul>
 */
@Service
public class CityVisibility {

    private final RegionMapper regionMapper;

    public CityVisibility(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    /**
     * 读取全部地区（含未启用、含省/市/区县）并建成 ID 索引。
     *
     * <p>供首页等需要批量判定的场景复用，避免在循环里逐条查库。
     *
     * @return 地区 ID 到地区对象的映射，不含 null 键
     */
    public Map<Long, Region> byId() {
        return index(regionMapper.selectList(Wrappers.<Region>lambdaQuery()));
    }

    /**
     * 判断单个地区是否对客可订（门店详情与创建意向共用）。
     *
     * <p>会重新拉一次地区表，适合单次调用；批量场景请用带索引的重载，避免 N+1。
     *
     * @param regionId 门店所属地区 ID，可为 null
     * @return 可订返回 true；地区不存在、未启用、市未开通、区县父市未开通均返回 false
     */
    public boolean bookableRegion(Long regionId) {
        return isCustomerStoreRegion(regionId, byId());
    }

    /**
     * 判断单个地区是否对客可订（复用已加载的地区索引）。
     *
     * <p>三级规则：市看自身是否 OPEN；区县看父节点是否 OPEN；省及未知层级一律不可订。
     * 区县自身 {@code enabled=false} 也直接判不可订，与「节点不可维护」的约定保持一致。
     *
     * @param regionId 地区 ID，可为 null
     * @param byId     地区索引，可为 null（视为无法判定）
     * @return 可订返回 true
     */
    public boolean isCustomerStoreRegion(Long regionId, Map<Long, Region> byId) {
        if (regionId == null || byId == null) {
            return false;
        }
        Region region = byId.get(regionId);
        if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
            return false;
        }
        if (region.getLevel() == RegionLevel.CITY) {
            // 市：自身 OPEN 即可订，不看子节点
            return CityRules.isOpen(region);
        }
        if (region.getLevel() == RegionLevel.DISTRICT) {
            // 区县：父级市未开通即整片不可见；父节点缺失同样按不可订处理
            Region parent = region.getParentId() == null ? null : byId.get(region.getParentId());
            return CityRules.isOpen(parent);
        }
        // 省不设开通态；直接挂省的店也不作为可订对象
        return false;
    }

    /**
     * 计算门店列表的地区过滤范围。
     *
     * <p>流程：加载地区 → 校验入参地区（不存在 400、未启用 400）→ 按层级展开：
     * 市取该市及其启用区县；区县只取自身；省取省内所有已开通市及其启用区县；不传地区取全站已开通范围。
     *
     * <p>与 {@link #bookableRegion(Long)} 的区别：这里不抛 410，未开通的市/区县返回
     * {@link StoreListingScope#closed()}（空列表 + CLOSED + 空态提示），由控制器包装成 200 空页。
     *
     * @param regionId 地区 ID，可为省/市/区县；null 表示全站
     * @return 地区范围 + 目的地状态 + 空态提示
     * @throws ApiException 地区不存在或 {@code enabled=false} 时 400
     */
    public StoreListingScope listScope(Long regionId) {
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        Map<Long, Region> byId = index(regions);
        if (regionId == null) {
            // 未传地区：全站已开通市 + 其启用区县；destinationStatus 为 NONE，不展示「未开通」空态
            return new StoreListingScope(openMarketIds(regions, null), DestinationStatus.NONE, null);
        }
        Region region = byId.get(regionId);
        if (region == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不存在");
        }
        if (!Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
        if (region.getLevel() == RegionLevel.CITY) {
            if (!CityRules.isOpen(region)) {
                // 市已关闭：200 + 空页，不把其他市的店混进来
                return StoreListingScope.closed();
            }
            return new StoreListingScope(openMarketIds(regions, region.getId()), DestinationStatus.OPEN, null);
        }
        if (region.getLevel() == RegionLevel.DISTRICT) {
            // 区县只看父市：父市 CLOSED（或父节点缺失）同样走空页，而不是只筛这个区县
            Region parent = region.getParentId() == null ? null : byId.get(region.getParentId());
            if (!CityRules.isOpen(parent)) {
                return StoreListingScope.closed();
            }
            return new StoreListingScope(List.of(region.getId()), DestinationStatus.OPEN, null);
        }
        // 省：锚点用省 ID 展开其下所有已开通市；省内没有开通市时为空列表，但不给「暂未开通」提示
        return new StoreListingScope(openMarketIds(regions, region.getId()), DestinationStatus.NONE, null);
    }

    /**
     * 门户攻略列表和首页可以露出的地区：空地区另计；省；已开通的市及其启用区县。
     *
     * <p>只用于「主推」过滤：攻略直链详情不经过这里，所以已关闭市的已发布攻略仍能按 id 打开。
     * 省无条件保留，是为了让挂在省上的攻略继续出现在列表里（设计第 10 条）。
     *
     * @return 可露出攻略的地区 ID 集合，可能为空
     */
    public Set<Long> promotedArticleRegionIds() {
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        Set<Long> openCityIds = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        for (Region region : regions) {
            if (region.getLevel() == RegionLevel.PROVINCE) {
                ids.add(region.getId());
            }
            if (CityRules.isOpen(region)) {
                openCityIds.add(region.getId());
                ids.add(region.getId());
            }
        }
        // 区县要单独一轮：必须先收齐已开通市，再判断区县的父节点是否在其中
        for (Region region : regions) {
            if (region.getLevel() == RegionLevel.DISTRICT
                    && Boolean.TRUE.equals(region.getEnabled())
                    && region.getParentId() != null
                    && openCityIds.contains(region.getParentId())) {
                ids.add(region.getId());
            }
        }
        return ids;
    }

    /**
     * 收集某锚点下可查的地区 ID。
     *
     * <p>锚点为 null 时取全站；为省 ID 时取该省下的已开通市；为市 ID 时只取该市。
     * 之后统一补上这些市下面 {@code enabled=true} 的区县。
     */
    private List<Long> openMarketIds(List<Region> regions, Long anchorId) {
        Set<Long> cityIds = new HashSet<>();
        for (Region region : regions) {
            if (!CityRules.isOpen(region)) {
                continue;
            }
            // anchorId 为省时用 parentId 命中；为市时用 id 命中，不会带入同省的其他市
            if (anchorId == null || anchorId.equals(region.getId()) || anchorId.equals(region.getParentId())) {
                cityIds.add(region.getId());
            }
        }
        List<Long> ids = new ArrayList<>(cityIds);
        // 区县只在父市已开通且自身启用时加入；区县不单独开城
        for (Region region : regions) {
            if (region.getLevel() == RegionLevel.DISTRICT
                    && Boolean.TRUE.equals(region.getEnabled())
                    && region.getParentId() != null
                    && cityIds.contains(region.getParentId())) {
                ids.add(region.getId());
            }
        }
        return ids;
    }

    /** 把地区列表建成 ID 索引；id 为 null 的行直接跳过。 */
    private Map<Long, Region> index(List<Region> regions) {
        Map<Long, Region> byId = new HashMap<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
        }
        return byId;
    }
}
