package com.suzhong.njl.service.merchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.service.support.PhoneMask;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantIntentResponse;

/**
 * 商家端预订意向查询服务（商家端，只读）。
 *
 * <p>职责：向商家展示本主体名下门店收到的预订意向列表与详情。本端不提供任何写操作，
 * 意向状态只能由运营推进；游客侧提交意向走门户的 {@code POST /api/booking-intents}。
 *
 * <p>主体隔离：入口先用 {@link MerchantAccess#requireOrgId} 取当前主体 ID，列表、计数、详情、
 * 门店名查询全部以 {@code merchant_org_id} 作为 SQL 过滤条件，商家无法通过改 URL 中的 ID
 * 读取其他主体的意向；详情查不到统一按 404 处理，不暴露记录是否存在。
 *
 * <p>脱敏：响应中的联系电话经 {@link PhoneMask#maskContact} 保留前 3 位与后 4 位，
 * 完整号码只在运营端可见。
 *
 * <p>事务边界：纯读，不开启事务。
 */
@Service
public class MerchantIntentService {

    private final MerchantAccess merchantAccess;
    private final BookingIntentMapper bookingIntentMapper;
    private final StoreMapper storeMapper;

    public MerchantIntentService(
            MerchantAccess merchantAccess,
            BookingIntentMapper bookingIntentMapper,
            StoreMapper storeMapper) {
        this.merchantAccess = merchantAccess;
        this.bookingIntentMapper = bookingIntentMapper;
        this.storeMapper = storeMapper;
    }

    /**
     * 分页查询本主体的预订意向，按创建时间倒序。
     *
     * <p>流程：解析当前主体 ID → 规范化分页参数 → 按主体 count → 取当页记录 → 批量补门店名 →
     * 逐条脱敏联系电话后组装响应。
     *
     * <p>排序用 {@code created_at DESC, id DESC}，后者保证同一秒创建的记录顺序稳定；
     * 门店名按本主体范围批量查询，避免逐条查库。
     *
     * @param principal 当前登录商家，由控制器从 {@code SecurityContext} 取得
     * @param page      页码，从 1 开始，为空取 1
     * @param pageSize  每页条数，为空取 20，超过 100 或小于 1 返回 400
     * @return 当页意向 + 总数 + 分页信息；无数据时 items 为空列表
     * @throws ApiException 分页参数非法时 400；账号或主体停用、越权时 403
     */
    public PageResult<MerchantIntentResponse> list(AuthenticatedUser principal, Integer page, Integer pageSize) {
        // 先过主体闸门：返回的 orgId 是后续所有查询的唯一租户过滤条件
        long orgId = merchantAccess.requireOrgId(principal);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        // count 与明细共用同一个 merchant_org_id 条件，避免总数与列表口径不一致
        Long total = bookingIntentMapper.selectCount(
                Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getMerchantOrgId, orgId));
        // LIMIT/OFFSET 的值已由 PageQuery 校验为整数，直接拼接不会引入注入
        List<BookingIntent> rows = bookingIntentMapper.selectList(Wrappers.<BookingIntent>lambdaQuery()
                .eq(BookingIntent::getMerchantOrgId, orgId)
                .orderByDesc(BookingIntent::getCreatedAt)
                .orderByDesc(BookingIntent::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        // 批量取门店名，避免每条意向量化一次查库
        Map<Long, String> storeNames = storeNames(orgId, rows);
        List<MerchantIntentResponse> items = new ArrayList<>(rows.size());
        for (BookingIntent row : rows) {
            items.add(toResponse(row, storeNames.get(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 查询本主体某条意向的详情。
     *
     * <p>流程：解析当前主体 ID → 以「意向 ID + 主体 ID」联合查询 → 补门店名 → 脱敏后返回。
     *
     * @param principal 当前登录商家
     * @param id        意向 ID，来自路径参数
     * @return 意向详情，联系电话已脱敏
     * @throws ApiException 意向不存在或不属于本主体时 404「意向不存在」，两种情况不区分以避免探测；
     *                      账号或主体停用、越权时 403
     */
    public MerchantIntentResponse detail(AuthenticatedUser principal, Long id) {
        long orgId = merchantAccess.requireOrgId(principal);
        // 主体条件必须与主键同时生效：仅按 id 查会放行其他主体的单号
        BookingIntent intent = bookingIntentMapper.selectOne(Wrappers.<BookingIntent>lambdaQuery()
                .eq(BookingIntent::getId, id)
                .eq(BookingIntent::getMerchantOrgId, orgId));
        if (intent == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "意向不存在");
        }
        return toResponse(intent, storeName(orgId, intent.getStoreId()));
    }

    /** 批量查询本主体门店名，返回「门店 ID → 名称」映射；门店缺失的键由调用方按 null 处理。 */
    private Map<Long, String> storeNames(long orgId, List<BookingIntent> rows) {
        Map<Long, String> names = new HashMap<>();
        if (rows.isEmpty()) {
            return names;
        }
        List<Long> storeIds = new ArrayList<>();
        for (BookingIntent row : rows) {
            storeIds.add(row.getStoreId());
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery()
                .eq(Store::getMerchantOrgId, orgId)
                .in(Store::getId, storeIds));
        for (Store store : stores) {
            names.put(store.getId(), store.getName());
        }
        return names;
    }

    /** 按「门店 ID + 主体 ID」取单个门店名，门店不存在或不属于本主体时返回 null。 */
    private String storeName(long orgId, Long storeId) {
        Store store = storeMapper.selectOne(Wrappers.<Store>lambdaQuery()
                .eq(Store::getId, storeId)
                .eq(Store::getMerchantOrgId, orgId));
        return store == null ? null : store.getName();
    }

    /** 组装商家端意向响应；联系电话在本出口统一脱敏，完整号码不下发商家。 */
    private MerchantIntentResponse toResponse(BookingIntent intent, String storeName) {
        return new MerchantIntentResponse(
                intent.getId(),
                intent.getIntentNo(),
                intent.getStatus(),
                intent.getStoreId(),
                storeName,
                intent.getCheckIn(),
                intent.getCheckOut(),
                intent.getNights(),
                intent.getGuests(),
                intent.getContactName(),
                PhoneMask.maskContact(intent.getContactPhone()),
                intent.getUserRemark(),
                intent.getCreatedAt());
    }
}
