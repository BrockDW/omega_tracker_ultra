package com.tracker.demo.controller;

import com.tracker.demo.dto.KeyBrPracticeResult;
import com.tracker.demo.service.KeybrScraperServiceV2;
import com.tracker.demo.sql.entity.KeyBrPracticeRecord;
import com.tracker.demo.sql.repository.KeyBrPracticeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class KeyBrController {

    @Autowired
    private KeybrScraperServiceV2 keybrScraperServiceV2;

    @Autowired
    private KeyBrPracticeRecordRepository keyBrPracticeRecordRepository;

    // Single-day endpoint remains the same (optional)
    @GetMapping("/keybr/day/{dateStr}")
    public KeyBrPracticeResult getPracticeForDay(@PathVariable String dateStr, @RequestParam(defaultValue = "false") Boolean isForced) {
        LocalDate date = LocalDate.parse(dateStr);
        return fetchKeyBrPracticeResult(date, isForced);
    }

    /**
     * Range endpoint:
     *   GET /keybr/range?start=YYYY-MM-DD&end=YYYY-MM-DD
     * Fetch all records in [start, end] via a single DB query,
     * fill zeros for missing days, and override "today" with scraped data.
     */
    @GetMapping("/keybr/range")
    public Map<String, KeyBrPracticeResult> getPracticeForRange(
            @RequestParam String start,
            @RequestParam String end
    ) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate   = LocalDate.parse(end);

        // 1) Single DB query for the entire range (excluding any "if" inside the loop)
        List<KeyBrPracticeRecord> records = keyBrPracticeRecordRepository
                .findAllByPracticeDateBetween(startDate, endDate);

        // 2) Build a map { date -> record } for quick lookups
        Map<LocalDate, KeyBrPracticeRecord> recordMap = new HashMap<>();
        for (KeyBrPracticeRecord r : records) {
            recordMap.put(r.getPracticeDate(), r);
        }

        // 3) Fill in every date from start..end with DB data or zeros
        Map<String, KeyBrPracticeResult> resultMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            KeyBrPracticeRecord rec = recordMap.get(date);
            resultMap.put(date.toString(), toPracticeResult(rec));
        }

        return resultMap;
    }

    /**
     * Helper method: if there's no record in DB, return zeros.
     */
    private KeyBrPracticeResult toPracticeResult(KeyBrPracticeRecord rec) {
        if (rec == null) {
            return new KeyBrPracticeResult(0, 0, 0);
        }
        return new KeyBrPracticeResult(
                rec.getPercentage(),
                rec.getTotalMinutes(),
                rec.getMinutesPracticed()
        );
    }

    /**
     * [Optional] This is your existing day-level logic if you still need it:
     */
    private KeyBrPracticeResult fetchKeyBrPracticeResult(LocalDate date, Boolean isForced) {
        if (date.equals(LocalDate.now()) && isForced) {
            return keybrScraperServiceV2.getPracticeTimeWithSession();
        }
        KeyBrPracticeRecord record = keyBrPracticeRecordRepository.findByPracticeDate(date);
        if (record != null) {
            return new KeyBrPracticeResult(
                    record.getPercentage(),
                    record.getTotalMinutes(),
                    record.getMinutesPracticed()
            );
        }
        return new KeyBrPracticeResult(0, 0, 0);
    }
}
