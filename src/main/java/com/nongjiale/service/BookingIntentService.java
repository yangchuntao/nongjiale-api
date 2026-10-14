package com.nongjiale.service;

import com.nongjiale.domain.BookingIntent;
import com.nongjiale.domain.Homestay;
import com.nongjiale.domain.IntentNote;
import com.nongjiale.repository.BookingIntentRepository;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.IntentNoteRepository;
import com.nongjiale.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingIntentService {
    private static final Set<String> ALLOWED = Set.of("CONFIRMED", "RESCHEDULED", "CLOSED");

    private final BookingIntentRepository intents;
    private final HomestayRepository homestays;
    private final IntentNoteRepository notes;

    public BookingIntentService(BookingIntentRepository intents, HomestayRepository homestays, IntentNoteRepository notes) {
        this.intents = intents;
        this.homestays = homestays;
        this.notes = notes;
    }

    @Transactional
    public BookingIntent create(Long userId, Long homestayId, LocalDate checkIn, LocalDate checkOut,
                                int guests, String travelMode, String contactName, String contactPhone) {
        if (guests < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "人数至少为 1");
        }
        if (checkOut == null || checkIn == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "离店日期必须晚于到店日期");
        }
        Homestay h = homestays.findById(homestayId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "农家乐不存在"));
        // 详情接口对不可订返回 410；提交意向固定 409，避免和「资源已消失」混用。
        if (!h.isOnSale()) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "该农家乐暂不可预订");
        }
        String key = userId + ":" + homestayId + ":" + checkIn;
        return intents.findByIdempotencyKey(key).orElseGet(() -> {
            BookingIntent bi = new BookingIntent();
            bi.setIntentNo(genNo());
            bi.setUserId(userId);
            bi.setHomestayId(homestayId);
            bi.setCheckIn(checkIn);
            bi.setCheckOut(checkOut);
            bi.setGuests(guests);
            bi.setTravelMode(travelMode);
            bi.setContactName(contactName);
            bi.setContactPhone(contactPhone);
            bi.setStatus("PENDING");
            bi.setIdempotencyKey(key);
            return intents.save(bi);
        });
    }

    @Transactional
    public BookingIntent updateStatus(Long intentId, Long adminId, String targetStatus, String note, Long expectedVersion) {
        BookingIntent bi = intents.findById(intentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "意向单不存在"));
        if (expectedVersion != null && !expectedVersion.equals(bi.getVersion())) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "单据已被他人更新，请刷新后重试");
        }
        if (!"PENDING".equals(bi.getStatus()) && !ALLOWED.contains(targetStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "非法状态流转");
        }
        if (!ALLOWED.contains(targetStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "目标状态不合法");
        }
        bi.setStatus(targetStatus);
        intents.save(bi);
        IntentNote n = new IntentNote();
        n.setIntentId(bi.getId());
        n.setAdminId(adminId);
        n.setAction(targetStatus);
        n.setNote(note);
        notes.save(n);
        return bi;
    }

    public List<IntentNote> notes(Long intentId) {
        return notes.findByIntentIdOrderByCreatedAtAsc(intentId);
    }

    private static String genNo() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int r = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "NI" + day + r;
    }
}
