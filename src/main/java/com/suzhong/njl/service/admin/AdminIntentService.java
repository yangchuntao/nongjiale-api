package com.suzhong.njl.service.admin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.IntentNote;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.NoteAction;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.IntentNoteMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminIntentDetail;
import com.suzhong.njl.web.admin.dto.AdminIntentItem;
import com.suzhong.njl.web.admin.dto.IntentNoteItem;
import com.suzhong.njl.web.admin.dto.IntentNoteRequest;
import com.suzhong.njl.web.admin.dto.IntentStatusRequest;

/**
 * 预订意向管理服务（运营端，{@code /api/admin/intents}）。
 *
 * <p>职责：意向列表与详情、状态迁移、追加备注、CSV 导出。运营看完整手机号，不做脱敏。
 * 状态机与合法迁移集中在 {@link IntentStatus#canTransitTo}，本类只负责校验、追加备注与写审计。
 *
 * <p>事务边界：{@link #changeStatus}、{@link #addNote} 各一个事务，
 * 意向更新、备注与审计同事务；列表、详情、导出只读，不开事务。
 *
 * <p>关键约束：导出不分页，条数上限见 {@link #EXPORT_LIMIT}，超限返回 400；
 * CSV 带 UTF-8 BOM，单元格转义与公式注入防护见 {@link #cell}。
 */
@Service
public class AdminIntentService {

    /** 单次导出的最大条数，超过返回 400「导出条数过多」，避免一次拉爆内存。 */
    private static final int EXPORT_LIMIT = 10000;

    private final BookingIntentMapper bookingIntentMapper;
    private final IntentNoteMapper intentNoteMapper;
    private final StoreMapper storeMapper;
    private final AuditRecorder auditRecorder;

    public AdminIntentService(
            BookingIntentMapper bookingIntentMapper,
            IntentNoteMapper intentNoteMapper,
            StoreMapper storeMapper,
            AuditRecorder auditRecorder) {
        this.bookingIntentMapper = bookingIntentMapper;
        this.intentNoteMapper = intentNoteMapper;
        this.storeMapper = storeMapper;
        this.auditRecorder = auditRecorder;
    }

    /**
     * 分页查询预订意向。
     *
     * <p>流程：规范化分页 → 先 count → 按条件取当页（更新时间、id 倒序）→ 批量补门店名 → 组装。
     * 筛选条件全部下推到 SQL；{@code checkInFrom/To} 含当天，{@code createdFrom/To} 也按整天闭区间。
     *
     * @param status       意向状态，null 表示不筛
     * @param storeId      门店 ID，null 表示不筛
     * @param merchantOrgId 商家主体 ID，null 表示不筛
     * @param checkInFrom  到店日起，含当天
     * @param checkInTo    到店日止，含当天
     * @param createdFrom  创建日起，含当天
     * @param createdTo    创建日止，含当天
     * @param page         页码，从 1 开始，空取 1
     * @param pageSize     每页条数，空取 20，上限 100
     * @return 当页意向与总数
     * @throws com.suzhong.njl.web.ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<AdminIntentItem> list(
            IntentStatus status,
            Long storeId,
            Long merchantOrgId,
            LocalDate checkInFrom,
            LocalDate checkInTo,
            LocalDate createdFrom,
            LocalDate createdTo,
            Integer page,
            Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        // count 与明细各建一份 wrapper，避免 orderBy/limit 污染 count 语句
        LambdaQueryWrapper<BookingIntent> countQuery = filter(status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo);
        Long total = bookingIntentMapper.selectCount(countQuery);
        List<BookingIntent> rows = bookingIntentMapper.selectList(
                filter(status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo)
                        .orderByDesc(BookingIntent::getUpdatedAt)
                        .orderByDesc(BookingIntent::getId)
                        .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        Map<Long, String> storeNames = storeNames(rows);
        List<AdminIntentItem> items = new ArrayList<>(rows.size());
        for (BookingIntent row : rows) {
            items.add(toItem(row, storeNames.get(row.getStoreId())));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 查询意向详情，含按 id 从早到晚的备注流水。
     *
     * @param id 意向 ID
     * @return 意向完整字段与 notes 列表
     * @throws com.suzhong.njl.web.ApiException 意向不存在时 404「意向不存在」
     */
    public AdminIntentDetail detail(Long id) {
        return toDetail(requireIntent(id));
    }

    /**
     * 变更意向状态。
     *
     * <p>流程：取意向 → 校验合法迁移 → 按请求 version 更新状态 → 追加 {@code STATUS_CHANGE} 备注
     * → 写 {@code INTENT_STATUS} 审计 → 回读详情。
     * 已入住、已关闭是终态，不能再迁；同一状态重复提交也按非法迁移处理。
     *
     * @param adminId 当前运营账号 ID，写入备注与审计
     * @param id      意向 ID
     * @param request 目标状态、可选备注与必填 version
     * @return 更新后的意向详情，含新增备注与递增后的 version
     * @throws com.suzhong.njl.web.ApiException 意向不存在 404；状态为空或迁移不合法 409；
     *         version 与库内不一致（乐观锁冲突）409「数据已被修改」
     */
    @Transactional
    public AdminIntentDetail changeStatus(long adminId, Long id, IntentStatusRequest request) {
        BookingIntent intent = requireIntent(id);
        IntentStatus target = request.targetStatus();
        IntentStatus from = intent.getStatus();
        // 脏数据里状态可能为 null，与非法迁移一样按 409 拒绝，不给越级变更留口子
        if (from == null) {
            throw new ApiException(HttpStatus.CONFLICT, "不能改为" + target.label());
        }
        if (!from.canTransitTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT, "不能从" + from.label() + "改为" + target.label());
        }
        intent.setStatus(target);
        // version 取请求值，@Version 在 SQL 层比对；影响行数不为 1 即并发冲突
        intent.setVersion(request.version());
        int updated = bookingIntentMapper.updateById(intent);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        IntentNote note = new IntentNote();
        note.setIntentId(id);
        note.setAdminId(adminId);
        note.setAction(NoteAction.STATUS_CHANGE);
        note.setFromStatus(from);
        note.setToStatus(target);
        note.setNote(blankToNull(request.note()));
        intentNoteMapper.insert(note);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from", from.code());
        detail.put("to", target.code());
        auditRecorder.record(adminId, AuditAction.INTENT_STATUS, AuditTarget.BOOKING_INTENT, id, detail);
        return toDetail(requireIntent(id));
    }

    /**
     * 只追加一条运营备注，不改意向状态与版本。
     *
     * @param adminId 当前运营账号 ID
     * @param id      意向 ID
     * @param request 备注内容，必填，最长 1000，由 DTO 校验
     * @return 追加备注后的意向详情
     * @throws com.suzhong.njl.web.ApiException 意向不存在时 404「意向不存在」
     */
    @Transactional
    public AdminIntentDetail addNote(long adminId, Long id, IntentNoteRequest request) {
        requireIntent(id);
        IntentNote note = new IntentNote();
        note.setIntentId(id);
        note.setAdminId(adminId);
        note.setAction(NoteAction.COMMENT);
        note.setNote(request.note().trim());
        intentNoteMapper.insert(note);
        return toDetail(requireIntent(id));
    }

    /**
     * 按与列表相同的筛选导出 CSV，不分页。
     *
     * <p>先 count 控总量，超过 {@link #EXPORT_LIMIT} 直接 400；列顺序固定，
     * 手机号输出完整号码，没有佣金、金额、支付列。
     *
     * @param status        意向状态，null 表示不筛
     * @param storeId       门店 ID
     * @param merchantOrgId 商家主体 ID
     * @param checkInFrom   到店日起
     * @param checkInTo     到店日止
     * @param createdFrom   创建日起
     * @param createdTo     创建日止
     * @return UTF-8 编码的 CSV 字节，首部带 BOM
     * @throws com.suzhong.njl.web.ApiException 结果超过 10000 条时 400「导出条数过多」
     */
    public byte[] export(
            IntentStatus status,
            Long storeId,
            Long merchantOrgId,
            LocalDate checkInFrom,
            LocalDate checkInTo,
            LocalDate createdFrom,
            LocalDate createdTo) {
        // 导出不分页，先按同一筛选 count 控总量，避免一次把大表拉进内存
        Long total = bookingIntentMapper.selectCount(
                filter(status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo));
        if (total != null && total > EXPORT_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "导出条数过多");
        }
        List<BookingIntent> rows = bookingIntentMapper.selectList(
                filter(status, storeId, merchantOrgId, checkInFrom, checkInTo, createdFrom, createdTo)
                        .orderByDesc(BookingIntent::getUpdatedAt)
                        .orderByDesc(BookingIntent::getId));
        Map<Long, String> storeNames = storeNames(rows);
        StringBuilder csv = new StringBuilder();
        // BOM 让 Excel 直接按 UTF-8 打开，避免中文乱码
        csv.append('\uFEFF');
        csv.append("单号,门店,到店日期,离店日期,晚数,人数,状态,联系人,手机号,游客备注\n");
        for (BookingIntent row : rows) {
            csv.append(cell(row.getIntentNo())).append(',');
            csv.append(cell(storeNames.get(row.getStoreId()))).append(',');
            csv.append(cell(text(row.getCheckIn()))).append(',');
            csv.append(cell(text(row.getCheckOut()))).append(',');
            csv.append(cell(text(row.getNights()))).append(',');
            csv.append(cell(text(row.getGuests()))).append(',');
            csv.append(cell(statusLabel(row.getStatus()))).append(',');
            csv.append(cell(row.getContactName())).append(',');
            csv.append(cell(row.getContactPhone())).append(',');
            csv.append(cell(row.getUserRemark())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 组装列表/导出的筛选条件，条件全部下推到 SQL。
     * 创建日的结束值用次日零点开区间，等价于包含结束日全天。
     */
    private LambdaQueryWrapper<BookingIntent> filter(
            IntentStatus status,
            Long storeId,
            Long merchantOrgId,
            LocalDate checkInFrom,
            LocalDate checkInTo,
            LocalDate createdFrom,
            LocalDate createdTo) {
        LambdaQueryWrapper<BookingIntent> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(BookingIntent::getStatus, status);
        }
        if (storeId != null) {
            query.eq(BookingIntent::getStoreId, storeId);
        }
        if (merchantOrgId != null) {
            query.eq(BookingIntent::getMerchantOrgId, merchantOrgId);
        }
        if (checkInFrom != null) {
            query.ge(BookingIntent::getCheckIn, checkInFrom);
        }
        if (checkInTo != null) {
            query.le(BookingIntent::getCheckIn, checkInTo);
        }
        if (createdFrom != null) {
            query.ge(BookingIntent::getCreatedAt, createdFrom.atStartOfDay());
        }
        if (createdTo != null) {
            // 用次日零点开区间，避免 23:59:59 之后的毫秒数据被漏掉
            query.lt(BookingIntent::getCreatedAt, createdTo.plusDays(1).atStartOfDay());
        }
        return query;
    }

    /** 按 ID 取意向，不存在统一抛 404。 */
    private BookingIntent requireIntent(Long id) {
        BookingIntent intent = bookingIntentMapper.selectById(id);
        if (intent == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "意向不存在");
        }
        return intent;
    }

    /** 批量补当页门店名，避免逐条查询；门店被删时名称留空。 */
    private Map<Long, String> storeNames(List<BookingIntent> rows) {
        Map<Long, String> names = new HashMap<>();
        if (rows.isEmpty()) {
            return names;
        }
        List<Long> ids = new ArrayList<>();
        for (BookingIntent row : rows) {
            ids.add(row.getStoreId());
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery().in(Store::getId, ids));
        for (Store store : stores) {
            names.put(store.getId(), store.getName());
        }
        return names;
    }

    /** 组装意向详情，备注按 id 升序（从早到晚）。 */
    private AdminIntentDetail toDetail(BookingIntent intent) {
        Store store = storeMapper.selectById(intent.getStoreId());
        List<IntentNote> notes = intentNoteMapper.selectList(Wrappers.<IntentNote>lambdaQuery()
                .eq(IntentNote::getIntentId, intent.getId())
                .orderByAsc(IntentNote::getId));
        List<IntentNoteItem> items = new ArrayList<>(notes.size());
        for (IntentNote note : notes) {
            items.add(new IntentNoteItem(
                    note.getId(),
                    note.getAdminId(),
                    note.getAction(),
                    note.getFromStatus(),
                    note.getToStatus(),
                    note.getNote(),
                    note.getCreatedAt()));
        }
        return new AdminIntentDetail(
                intent.getId(),
                intent.getIntentNo(),
                intent.getUserId(),
                intent.getStoreId(),
                store == null ? null : store.getName(),
                intent.getMerchantOrgId(),
                intent.getCheckIn(),
                intent.getCheckOut(),
                intent.getNights(),
                intent.getGuests(),
                intent.getContactName(),
                intent.getContactPhone(),
                intent.getUserRemark(),
                intent.getStatus(),
                statusLabel(intent.getStatus()),
                intent.getVersion(),
                intent.getCreatedAt(),
                intent.getUpdatedAt(),
                items);
    }

    /** 组装列表项，门店名由批量查询结果传入。 */
    private AdminIntentItem toItem(BookingIntent intent, String storeName) {
        return new AdminIntentItem(
                intent.getId(),
                intent.getIntentNo(),
                intent.getUserId(),
                intent.getStoreId(),
                storeName,
                intent.getMerchantOrgId(),
                intent.getCheckIn(),
                intent.getCheckOut(),
                intent.getNights(),
                intent.getGuests(),
                intent.getContactName(),
                intent.getContactPhone(),
                intent.getUserRemark(),
                intent.getStatus(),
                statusLabel(intent.getStatus()),
                intent.getVersion(),
                intent.getCreatedAt(),
                intent.getUpdatedAt());
    }

    /**
     * 库内空串或无法对应 {@link IntentStatus} 的码，MyBatis 枚举处理器会读成 null。
     * 未筛选列表会扫到这些行；标签留空，避免 label() 空指针把整页打成 500。
     */
    private String statusLabel(IntentStatus status) {
        return status == null ? "" : status.label();
    }

    /** 空白备注转 null，避免库里出现空串备注。 */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /** 任意值转 CSV 文本，null 输出空串。 */
    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * CSV 单元格转义：公式开头加单引号，含逗号、引号或换行时整体加双引号并把内部引号翻倍。
     */
    private String cell(String value) {
        String text = value == null ? "" : value;
        if (text.startsWith("=") || text.startsWith("+") || text.startsWith("-") || text.startsWith("@")) {
            text = "'" + text;
        }
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
