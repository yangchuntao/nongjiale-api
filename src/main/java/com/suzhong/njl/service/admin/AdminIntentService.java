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

@Service
public class AdminIntentService {

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

    public AdminIntentDetail detail(Long id) {
        return toDetail(requireIntent(id));
    }

    @Transactional
    public AdminIntentDetail changeStatus(long adminId, Long id, IntentStatusRequest request) {
        BookingIntent intent = requireIntent(id);
        IntentStatus target = request.targetStatus();
        IntentStatus from = intent.getStatus();
        if (from == null) {
            throw new ApiException(HttpStatus.CONFLICT, "不能改为" + target.label());
        }
        if (!from.canTransitTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT, "不能从" + from.label() + "改为" + target.label());
        }
        intent.setStatus(target);
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

    public byte[] export(
            IntentStatus status,
            Long storeId,
            Long merchantOrgId,
            LocalDate checkInFrom,
            LocalDate checkInTo,
            LocalDate createdFrom,
            LocalDate createdTo) {
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
            query.lt(BookingIntent::getCreatedAt, createdTo.plusDays(1).atStartOfDay());
        }
        return query;
    }

    private BookingIntent requireIntent(Long id) {
        BookingIntent intent = bookingIntentMapper.selectById(id);
        if (intent == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "意向不存在");
        }
        return intent;
    }

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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

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
