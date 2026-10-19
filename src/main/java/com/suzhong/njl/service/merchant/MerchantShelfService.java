package com.suzhong.njl.service.merchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.ShelfApplication;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.mapper.ShelfApplicationMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationRequest;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationResponse;

/**
 * 商家端上下架申请服务（商家端）。
 *
 * <p>职责：商家对自营门店提交上架或下架申请，并分页查询本主体的申请记录。申请本身只写
 * {@code shelf_application}，不改变门店状态；必须由运营审批通过后才改写 {@code store.shelf_status}，
 * 因此商家无法自行上下架，也没有直接改上下架或首页推荐的接口。
 *
 * <p>主体隔离：入口用 {@link MerchantAccess#requireOrgId} 取当前主体 ID，门店归属由
 * {@link MerchantStoreService#requireOwned} 校验；列表只按 {@code merchant_org_id} 过滤，
 * 申请单的主体字段取自服务端校验结果，不接受请求传入。
 *
 * <p>唯一性：同一门店同时只允许一条 {@code PENDING} 申请，重复提交返回 409。该校验是
 * 「先 count 再 insert」的应用层约束，表上没有唯一索引兜底，极端并发下仍可能产生两条待审记录。
 *
 * <p>事务边界：{@link #apply} 标注 {@code @Transactional}，count 与 insert 在同一事务内；
 * {@link #list} 为纯读，不开事务。
 */
@Service
public class MerchantShelfService {

    private final MerchantAccess merchantAccess;
    private final MerchantStoreService merchantStoreService;
    private final ShelfApplicationMapper shelfApplicationMapper;
    private final StoreMapper storeMapper;

    public MerchantShelfService(
            MerchantAccess merchantAccess,
            MerchantStoreService merchantStoreService,
            ShelfApplicationMapper shelfApplicationMapper,
            StoreMapper storeMapper) {
        this.merchantAccess = merchantAccess;
        this.merchantStoreService = merchantStoreService;
        this.shelfApplicationMapper = shelfApplicationMapper;
        this.storeMapper = storeMapper;
    }

    /**
     * 提交上架或下架申请。
     *
     * <p>流程：解析当前主体 ID → 校验门店属于本主体 → 统计该门店是否已有待审申请 →
     * 组装申请单（主体、门店、动作、原因、待审状态）→ 落库 → 返回申请详情。
     *
     * <p>动作取值见 {@link com.suzhong.njl.domain.enums.ShelfAction}：{@code PUBLISH} 申请上架、
     * {@code UNPUBLISH} 申请下架；非法枚举值在请求反序列化阶段即返回 400。
     *
     * @param principal 当前登录商家
     * @param storeId   门店 ID，来自路径参数
     * @param request   申请请求，动作必填，原因可空且最长 500 字
     * @return 新建的申请单，状态固定 {@code PENDING}，含门店名与主体 ID
     * @throws ApiException 账号或主体停用、越权时 403；门店不存在或不属于本主体时 404「门店不存在」；
     *                      该门店已有待审申请时 409「已有待审的上下架申请」
     */
    @Transactional
    public ShelfApplicationResponse apply(AuthenticatedUser principal, Long storeId, ShelfApplicationRequest request) {
        long orgId = merchantAccess.requireOrgId(principal);
        // 以「门店 ID + 主体 ID」取店，非本主体门店按不存在处理，避免越权提交
        Store store = merchantStoreService.requireOwned(orgId, storeId);
        // 待审唯一性：只拦 PENDING，已通过或已驳回的历史申请不影响再次提交
        Long pending = shelfApplicationMapper.selectCount(Wrappers.<ShelfApplication>lambdaQuery()
                .eq(ShelfApplication::getStoreId, store.getId())
                .eq(ShelfApplication::getStatus, ReviewStatus.PENDING));
        if (pending != null && pending > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "已有待审的上下架申请");
        }
        ShelfApplication application = new ShelfApplication();
        application.setStoreId(store.getId());
        // 主体 ID 取自服务端校验结果，不能由请求体指定，否则可伪造归属
        application.setMerchantOrgId(orgId);
        application.setAction(request.action());
        application.setReason(request.reason());
        // 初始状态固定待审，审批与状态流转只在运营端进行
        application.setStatus(ReviewStatus.PENDING);
        shelfApplicationMapper.insert(application);
        return toResponse(application, store.getName());
    }

    /**
     * 分页查询本主体的上下架申请，按创建时间倒序。
     *
     * <p>流程：解析当前主体 ID → 规范化分页参数 → 按主体 count → 取当页 → 批量补门店名 → 组装响应。
     * 不按状态筛选，待审与已审记录一并返回，审批结果与驳回原因由响应字段体现。
     *
     * @param principal 当前登录商家
     * @param page      页码，从 1 开始，为空取 1
     * @param pageSize  每页条数，为空取 20，超过 100 或小于 1 返回 400
     * @return 当页申请 + 总数 + 分页信息
     * @throws ApiException 分页参数非法时 400；账号或主体停用、越权时 403
     */
    public PageResult<ShelfApplicationResponse> list(AuthenticatedUser principal, Integer page, Integer pageSize) {
        long orgId = merchantAccess.requireOrgId(principal);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        // 商家只能看到本主体的申请单，运营侧按全站查询走另一套服务
        Long total = shelfApplicationMapper.selectCount(
                Wrappers.<ShelfApplication>lambdaQuery().eq(ShelfApplication::getMerchantOrgId, orgId));
        List<ShelfApplication> rows = shelfApplicationMapper.selectList(Wrappers.<ShelfApplication>lambdaQuery()
                .eq(ShelfApplication::getMerchantOrgId, orgId)
                .orderByDesc(ShelfApplication::getCreatedAt)
                .orderByDesc(ShelfApplication::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, String> storeNames = storeNames(orgId, rows);
        List<ShelfApplicationResponse> items = new ArrayList<>(rows.size());
        for (ShelfApplication row : rows) {
            items.add(toResponse(row, storeNames.get(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /** 批量查询本主体门店名，返回「门店 ID → 名称」映射；用于列表避免逐条查库。 */
    private Map<Long, String> storeNames(long orgId, List<ShelfApplication> rows) {
        Map<Long, String> names = new HashMap<>();
        if (rows.isEmpty()) {
            return names;
        }
        List<Long> storeIds = new ArrayList<>();
        for (ShelfApplication row : rows) {
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

    /** 把申请实体转成商家端响应，门店名可为 null（门店被删除时）。 */
    private ShelfApplicationResponse toResponse(ShelfApplication application, String storeName) {
        return new ShelfApplicationResponse(
                application.getId(),
                application.getStoreId(),
                storeName,
                application.getMerchantOrgId(),
                application.getAction(),
                application.getReason(),
                application.getStatus(),
                application.getRejectReason(),
                application.getCreatedAt());
    }
}
