package com.mobilefactory.event.controller;

import com.mobilefactory.event.service.LotteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    /**
     * Participate in the event
     */
    @PostMapping("/participate")
    public Map<String, Object> participate(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String phone = payload.get("phone");

        Map<String, Object> response = new HashMap<>();
        try {
            lotteryService.participate(name, phone);
            response.put("status", "success");
            response.put("message", "참여가 완료되었습니다.");
        } catch (RuntimeException e) {
            response.put("status", "fail");
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Check Result
     */
    @GetMapping("/check")
    public Map<String, Object> checkResult(@RequestParam String phone) {
        return lotteryService.checkResult(phone);
    }

    /**
     * Trigger Draw (Admin)
     */
    @PostMapping("/draw")
    public Map<String, Object> draw() {
        Map<String, Object> response = new HashMap<>();
        try {
            lotteryService.drawActiveEvent();
            response.put("status", "success");
            response.put("message", "추첨이 완료되었습니다.");
        } catch (RuntimeException e) {
            response.put("status", "fail");
            response.put("message", e.getMessage());
        }
        return response;
    }
}
