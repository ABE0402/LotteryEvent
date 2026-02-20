package com.mobilefactory.event.service;

import com.mobilefactory.event.entity.EventMaster;
import com.mobilefactory.event.entity.EventParticipant;
import com.mobilefactory.event.entity.EventWinner;
import com.mobilefactory.event.repository.EventMasterRepository;
import com.mobilefactory.event.repository.EventParticipantRepository;
import com.mobilefactory.event.repository.EventWinnerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final EventMasterRepository eventMasterRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventWinnerRepository eventWinnerRepository;
    private final SmsService smsService;

    // Hardcoded constraint for 1st place (Demo requirement)
    private static final String PRE_ASSIGNED_PHONE = "010-1234-5678";

    /**
     * Participate in the event.
     */
    @Transactional
    public void participate(String name, String phone) {
        // 1. Find Active Event
        EventMaster event = eventMasterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("진행 중인 이벤트가 없습니다."));

        // 2. Check Duplicate
        if (eventParticipantRepository.findByPhoneAndEventId(phone, event.getEventId()).isPresent()) {
            throw new RuntimeException("이미 참여하신 전화번호입니다.");
        }

        // 3. Get Entry Number (Audit purpose)
        int entryNo = eventParticipantRepository.countByEventId(event.getEventId()) + 1;

        // 4. Generate Random Number for Participation
        String lotteryNumber = generateRandomNumber();

        // 5. Save Participant
        EventParticipant participant = EventParticipant.builder()
                .eventId(event.getEventId())
                .name(name)
                .phone(phone)
                .entryNo(entryNo)
                .lotteryNumber(lotteryNumber)
                .build();

        eventParticipantRepository.save(participant);

        // 6. Send SMS
        smsService.sendSms(phone, "[이벤트] 인증번호: " + lotteryNumber + " 입니다. 4월 1일 추첨 결과를 기대해주세요!");
    }

    /**
     * Check Lottery Result.
     */
    public Map<String, Object> checkResult(String phone) {
        Map<String, Object> result = new HashMap<>();

        // 1. Find Active or Latest Event
        // For simplicity, we just pick the first active one or any event.
        // In real world, we might need a specific event ID.
        // If no active event, maybe looking for past events?
        // Let's assume there is only one "Main" event for now.
        EventMaster event = eventMasterRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("이벤트 정보가 없습니다."));

        // 2. Check Participation
        Optional<EventParticipant> participantOpt = eventParticipantRepository.findByPhoneAndEventId(phone,
                event.getEventId());

        if (participantOpt.isEmpty()) {
            result.put("status", "lose");
            result.put("message", "참여 이력이 없습니다.");
            return result;
        }

        EventParticipant participant = participantOpt.get();

        // 3. Check Winner
        Optional<EventWinner> winnerOpt = eventWinnerRepository.findByParticipantId(participant.getParticipantId());

        if (winnerOpt.isPresent()) {
            EventWinner winner = winnerOpt.get();
            result.put("status", "win");
            result.put("rank", winner.getWinningRank());
            result.put("message", "축하합니다! " + winner.getPrizeName() + "에 당첨되셨습니다.");
        } else {
            // Check if draw has happened?
            // If lotteryNumber is null, draw hasn't happened.
            // But now we generate lotteryNumber on participate, so this logic needs update
            // if we want to distinguish "before draw" vs "after draw, lost"
            // For now, let's assume if not in Winner table, they are pending or lost.
            // A simple way is to check if Event is still active.
            if (event.getIsActive()) {
                result.put("status", "pending");
                result.put("message", "추첨 전입니다. 부여된 번호: " + participant.getLotteryNumber());
            } else {
                result.put("status", "lose");
                result.put("message", "아쉽게도 당첨되지 않았습니다.");
            }
        }
        return result;
    }

    /**
     * Draw Winners for the currently active event.
     */
    @Transactional
    public void drawActiveEvent() {
        EventMaster event = eventMasterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("진행 중인 이벤트가 없습니다."));
        drawWinners(event.getEventId());
    }

    /**
     * Draw Winners (Admin function)
     * Using the logic from previous 'drawWinners' but applying to DB.
     * NOTE: This logic RE-ASSIGNS lottery numbers to ensure winners.
     * This means the number sent via SMS might CHANGE if the user wins (or loses in
     * specific way).
     * For a real lottery, we should pick winners based on their EXISTING numbers.
     * But for this Demo/Assignment with "Rigged" requirement, we keep this
     * overwrite logic.
     */
    @Transactional
    public void drawWinners(Integer eventId) {
        EventMaster event = eventMasterRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다."));

        // Load all participants
        List<EventParticipant> allEntries = eventParticipantRepository.findByEventId(eventId);

        if (allEntries.isEmpty())
            return;

        Set<String> winningPhones = new HashSet<>();
        List<EventWinner> winners = new ArrayList<>();

        // 1. Assign Random IDs (Shuffle)
        Collections.shuffle(allEntries);
        // We can just use the index in the shuffled list as the "Random ID" logic

        // 2. Generate Grand Win Number
        String grandWinNum = generateRandomNumber();
        System.out.println("Winning Number: " + grandWinNum);

        // 3. Selection Logic (Migrated)

        // --- Rank 1: Specific Phone ---
        EventParticipant rank1 = findByPhone(allEntries, PRE_ASSIGNED_PHONE);
        if (rank1 == null && !allEntries.isEmpty()) {
            rank1 = allEntries.get(0); // Fallback
        }

        if (rank1 != null) {
            assignWinningNumber(rank1, grandWinNum, 1);
            addWinner(winners, winningPhones, rank1, 1, "1등 상품");
        }

        // Filter out winners for next ranks
        List<EventParticipant> candidates = allEntries.stream()
                .filter(e -> !winningPhones.contains(e.getPhone()))
                .collect(Collectors.toList());

        // --- Rank 2: 5 People (Randomly picked from remaining) ---
        // Original logic had "Random ID 2000~7000".
        // Since we shuffled the list, we can just pick from the list.
        // To strictly follow "Random ID" logic, we would assign an ID to each and
        // filter.
        // Let's simplify: just pick random N from candidates.
        pickAndAssign(candidates, 5, grandWinNum, 2, "2등 상품", winners, winningPhones);

        // --- Rank 3: 44 People ---
        pickAndAssign(candidates, 44, grandWinNum, 3, "3등 상품", winners, winningPhones);

        // --- Rank 4: 950 People ---
        pickAndAssign(candidates, 950, grandWinNum, 4, "4등 상품", winners, winningPhones);

        // 4. Losers
        for (EventParticipant p : candidates) {
            if (!winningPhones.contains(p.getPhone())) {
                assignWinningNumber(p, grandWinNum, 0);
            }
        }

        // 5. Save Changes
        eventParticipantRepository.saveAll(allEntries); // Save assigned lottery numbers (Overwrites initial SMS number)
        eventWinnerRepository.saveAll(winners); // Save winner records

        // 6. Deactivate Event
        event.setIsActive(false);
        eventMasterRepository.save(event);
    }

    private void pickAndAssign(List<EventParticipant> candidates, int count, String grandWinNum, int rank, String prize,
            List<EventWinner> winners, Set<String> winningPhones) {
        // Filter candidates again (though reference is passed, we iterate and modify
        // winningPhones)
        List<EventParticipant> pool = candidates.stream()
                .filter(e -> !winningPhones.contains(e.getPhone()))
                .collect(Collectors.toList());

        Collections.shuffle(pool);
        int limit = Math.min(count, pool.size());

        for (int i = 0; i < limit; i++) {
            EventParticipant p = pool.get(i);
            assignWinningNumber(p, grandWinNum, rank);
            addWinner(winners, winningPhones, p, rank, prize);
        }
    }

    private EventParticipant findByPhone(List<EventParticipant> list, String phone) {
        return list.stream().filter(e -> e.getPhone().equals(phone)).findFirst().orElse(null);
    }

    private void addWinner(List<EventWinner> winners, Set<String> phones, EventParticipant p, int rank, String prize) {
        EventWinner winner = EventWinner.builder()
                .eventId(p.getEventId())
                .participantId(p.getParticipantId())
                .winningRank(rank)
                .prizeName(prize)
                .build();
        winners.add(winner);
        phones.add(p.getPhone());
    }

    private void assignWinningNumber(EventParticipant p, String grandWinNum, int rank) {
        int matchCount;
        switch (rank) {
            case 1:
                matchCount = 6;
                break;
            case 2:
                matchCount = 5;
                break;
            case 3:
                matchCount = 4;
                break;
            case 4:
                matchCount = 3;
                break;
            default:
                matchCount = 0;
                break;
        }

        if (rank == 0) {
            p.setLotteryNumber(generatePartialMatch(grandWinNum, 0));
        } else {
            p.setLotteryNumber(generatePartialMatch(grandWinNum, matchCount));
        }
    }

    private String generateRandomNumber() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private String generatePartialMatch(String target, int matchCount) {
        char[] result = target.toCharArray();
        Random rnd = new Random();
        boolean[] isMatch = new boolean[6];

        // 1. Select indices to match
        int matches = 0;
        while (matches < matchCount) {
            int idx = rnd.nextInt(6);
            if (!isMatch[idx]) {
                isMatch[idx] = true;
                matches++;
            }
        }

        // 2. Change non-matching indices
        for (int i = 0; i < 6; i++) {
            if (!isMatch[i]) {
                char original = result[i];
                char changed;
                do {
                    changed = (char) ('0' + rnd.nextInt(10));
                } while (changed == original); // Ensure it's different from the target digit at this position
                result[i] = changed;
            }
        }
        return new String(result);
    }
}
