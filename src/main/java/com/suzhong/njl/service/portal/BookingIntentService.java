package com.suzhong.njl.service.portal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.WechatMessage;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.CreateIntentRequest;
import com.suzhong.njl.web.portal.dto.IntentResponse;

/**
 * 预订意向服务（门户端）。
 *
 * <p>职责：创建意向、查看「我的意向」。本平台无支付、无锁房，意向只是把游客需求转给商家/运营跟进。
 *
 * <p>关键约束：
 * <ul>
 *   <li>提交前必须已绑定手机号：{@code portal_user.phone} 为空时 403，且在插入之前抛出，不落库；</li>
 *   <li>门店必须当前可订：校验复用 {@link StoreQueryService#requireBookable(Long)}，未开通市与下架同为 410；</li>
 *   <li>只能看自己的意向：列表按 {@code user_id} 过滤，详情不匹配时返回 404 而不是 403，避免探测他人单号；</li>
 *   <li>事务边界：创建为单表插入，{@code @Transactional} 保证「校验 + 生成单号 + 插入」在同一事务内；</li>
 *   <li>无缓存。初始状态固定 {@code PENDING}，版本号从 0 开始，后续由运营侧乐观锁推进。</li>
 * </ul>
 */
@Service
public class BookingIntentService {

    private final BookingIntentMapper bookingIntentMapper;
    private final PortalUserMapper portalUserMapper;
    private final StoreMapper storeMapper;
    private final StoreQueryService storeQueryService;
    private final IntentNumbers intentNumbers;
    private final Clock applicationClock;

    public BookingIntentService(
            BookingIntentMapper bookingIntentMapper,
            PortalUserMapper portalUserMapper,
            StoreMapper storeMapper,
            StoreQueryService storeQueryService,
            IntentNumbers intentNumbers,
            Clock applicationClock) {
        this.bookingIntentMapper = bookingIntentMapper;
        this.portalUserMapper = portalUserMapper;
        this.storeMapper = storeMapper;
        this.storeQueryService = storeQueryService;
        this.intentNumbers = intentNumbers;
        this.applicationClock = applicationClock;
    }

    /**
     * 提交预订意向。
     *
     * <p>流程：确认账号存在且有手机号 → 校验门店可订 → 校验到店/离店日期与晚数 →
     * 组装意向（状态 PENDING、版本 0）→ 带唯一单号重试插入 → 回填门店名。
     *
     * @param user    当前登录游客
     * @param request 意向请求：门店、日期、晚数、人数、联系人、备注
     * @return 新创建的意向，含生成的意向单号
     * @throws ApiException 账号不存在 401；未绑定手机号 403；门店不存在 404；
     *                      门店已下架或城市未开通 410；到店日期早于今天、离店早于到店 409；
     *                      离店日期与晚数同时缺失 400
     */
    @Transactional
    public IntentResponse create(AuthenticatedUser user, CreateIntentRequest request) {
        PortalUser owner = portalUserMapper.selectById(user.userId());
        if (owner == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        // 手机号是打通网页版账号的前提；为空时在插入之前拒绝，保证不会产生无主意向
        if (owner.getPhone() == null || owner.getPhone().isBlank()) {
            throw WechatMessage.PHONE_REQUIRED_FOR_INTENT.toException();
        }
        Store store = storeQueryService.requireBookable(request.storeId());
        // 日期校验用应用时钟（Asia/Shanghai），避免服务器时区不同导致「今天」判断漂移
        if (request.checkIn().isBefore(LocalDate.now(applicationClock))) {
            throw new ApiException(HttpStatus.CONFLICT, "到店日期不正确");
        }
        if (request.checkOut() != null && request.checkOut().isBefore(request.checkIn())) {
            throw new ApiException(HttpStatus.CONFLICT, "离店日期不能早于到店日期");
        }
        if (request.checkOut() == null && request.nights() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写离店日期或晚数");
        }
        BookingIntent intent = new BookingIntent();
        intent.setUserId(user.userId());
        intent.setStoreId(store.getId());
        // 商家主体冗余落库：后续商家端按主体过滤意向时不依赖门店当前归属
        intent.setMerchantOrgId(store.getMerchantOrgId());
        intent.setCheckIn(request.checkIn());
        intent.setCheckOut(request.checkOut());
        intent.setNights(request.nights());
        intent.setGuests(request.guests());
        intent.setContactName(request.contactName());
        intent.setContactPhone(request.contactPhone());
        intent.setUserRemark(request.userRemark());
        intent.setStatus(IntentStatus.PENDING);
        intent.setVersion(0L);
        insertWithUniqueNo(intent);
        return toResponse(intent, store.getName());
    }

    /**
     * 分页查询当前游客自己的意向。
     *
     * <p>按创建时间倒序、ID 倒序；总数与明细都用同一个 {@code user_id} 条件。
     *
     * @param user     当前登录游客
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数，上限 100
     * @return 当页意向 + 总数 + 分页信息
     * @throws ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<IntentResponse> mine(AuthenticatedUser user, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = bookingIntentMapper.selectCount(
                Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getUserId, user.userId()));
        List<BookingIntent> rows = bookingIntentMapper.selectList(
                mineQuery(user.userId()).last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<IntentResponse> items = new ArrayList<>(rows.size());
        for (BookingIntent row : rows) {
            items.add(toResponse(row, storeName(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 查询单条意向。
     *
     * <p>不存在与不属于当前用户都返回 404：不用 403，避免通过 ID 探测他人意向是否存在。
     *
     * @param user 当前登录游客
     * @param id   意向 ID
     * @return 意向详情
     * @throws ApiException 意向不存在或不属于当前用户时 404「意向不存在」
     */
    public IntentResponse mineOne(AuthenticatedUser user, Long id) {
        BookingIntent intent = bookingIntentMapper.selectById(id);
        if (intent == null || !user.userId().equals(intent.getUserId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "意向不存在");
        }
        return toResponse(intent, storeName(intent.getStoreId()));
    }

    /** 我的意向查询条件：按用户隔离，创建时间倒序，同一时刻用 ID 兜底保证分页稳定。 */
    private LambdaQueryWrapper<BookingIntent> mineQuery(Long userId) {
        return Wrappers.<BookingIntent>lambdaQuery()
                .eq(BookingIntent::getUserId, userId)
                .orderByDesc(BookingIntent::getCreatedAt)
                .orderByDesc(BookingIntent::getId);
    }

    /**
     * 带唯一单号重试插入。
     *
     * <p>单号是「日期 + 随机十六进制」，理论上有碰撞概率；靠唯一索引兜底，最多重试 3 次，
     * 第 3 次仍冲突则把冲突异常抛给全局异常处理，不静默吞掉。
     */
    private void insertWithUniqueNo(BookingIntent intent) {
        for (int attempt = 0; attempt < 3; attempt++) {
            intent.setIntentNo(intentNumbers.next());
            try {
                bookingIntentMapper.insert(intent);
                return;
            } catch (DuplicateKeyException ex) {
                if (attempt == 2) {
                    throw ex;
                }
            }
        }
    }

    /** 按门店 ID 取展示名；门店已物理删除时返回 null，不影响意向本身可读。 */
    private String storeName(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        return store == null ? null : store.getName();
    }

    /** 把实体转成门户响应，只暴露游客需要看的字段，不带商家主体与版本号。 */
    private IntentResponse toResponse(BookingIntent intent, String storeName) {
        return new IntentResponse(
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
                intent.getContactPhone(),
                intent.getUserRemark(),
                intent.getCreatedAt());
    }
}
