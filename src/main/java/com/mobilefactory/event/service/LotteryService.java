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

    // 1등 당첨을 위한 하드코딩된 제약조건 (1등 당첨 번호 조건)
    private static final String PRE_ASSIGNED_PHONE = "010-1234-5678";

    /**
     * 이벤트 참여
     */
    @Transactional
    public void participate(String name, String phone) {
        // 1. 진행 중인 이벤트 조회 (추후 홈페이지에 추가적인 이벤트가 생길 경우 사용될 부분)
        EventMaster event = eventMasterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("진행 중인 이벤트가 없습니다."));

        // 2. 중복 참여 확인
        if (eventParticipantRepository.findByPhoneAndEventId(phone, event.getEventId()).isPresent()) {
            throw new RuntimeException("이미 참여하신 전화번호입니다.");
        }

        // 3. 응모 번호 조회 (감사 목적)
        int entryNo = eventParticipantRepository.countByEventId(event.getEventId()) + 1;

        // 4. 참여를 위한 랜덤 번호 생성
        String lotteryNumber = generateRandomNumber();

        // 5. 참여자 정보 저장
        EventParticipant participant = EventParticipant.builder()
                .eventId(event.getEventId())
                .name(name)
                .phone(phone)
                .entryNo(entryNo)
                .lotteryNumber(lotteryNumber)
                .build();

        eventParticipantRepository.save(participant);

        // 6. SMS 발송
        smsService.sendSms(phone, "[이벤트] 인증번호: " + lotteryNumber + " 입니다. 4월 1일 추첨 결과를 기대해주세요!");
    }

    /**
     * 당첨 결과 확인
     */
    public Map<String, Object> checkResult(String phone) {
        Map<String, Object> result = new HashMap<>();

        // 1. 진행 중 또는 최근 이벤트 조회
        // 편의상 활성화된 이벤트나 첫 번째 이벤트를 조회합니다.
        // 실제 운영 환경에서는 특정 이벤트 ID가 필요할 수 있습니다.
        EventMaster event = eventMasterRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("이벤트 정보가 없습니다."));

        // 2. 참여 이력 확인
        Optional<EventParticipant> participantOpt = eventParticipantRepository.findByPhoneAndEventId(phone,
                event.getEventId());

        if (participantOpt.isEmpty()) {
            result.put("status", "lose");
            result.put("message", "참여 이력이 없습니다.");
            return result;
        }

        EventParticipant participant = participantOpt.get();

        // 3. 당첨 여부 확인
        Optional<EventWinner> winnerOpt = eventWinnerRepository.findByParticipantId(participant.getParticipantId());

        if (winnerOpt.isPresent()) {
            EventWinner winner = winnerOpt.get();
            result.put("status", "win");
            result.put("rank", winner.getWinningRank());
            result.put("message", "축하합니다! " + winner.getPrizeName() + "에 당첨되셨습니다.");
        } else {
            // 추첨 진행 여부 확인
            // 참여 시점에 번호가 발급되므로, 당첨자 테이블에 없으면 대기 중이거나 낙첨입니다.
            // 여기서는 이벤트 활성 여부로 판단합니다.
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
     * 현재 진행 중인 이벤트에 대한 추첨 진행
     */
    @Transactional
    public void drawActiveEvent() {
        EventMaster event = eventMasterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("진행 중인 이벤트가 없습니다."));
        drawWinners(event.getEventId());
    }

    /**
     * 추첨 진행 (관리자 기능)
     * DB에 저장된 참여자를 대상으로 추첨을 진행합니다.
     * 주의: 이 로직은 당첨자를 보장하기 위해 로또 번호를 '재할당(6자리)' 합니다.
     * 즉, 참여 시점에 발송된 SMS 번호와 실제 추첨 번호가 달라질 수 있습니다 (당첨/낙첨 조작).
     * 실제 로또라면 기존 번호를 기준으로 추첨해야 하지만, 과제 요구사항(특정 인원 당첨 보장)을 위해 덮어씁니다.
     */
    @Transactional
    public void drawWinners(Integer eventId) {
        EventMaster event = eventMasterRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다."));

        // 모든 참여자 조회
        List<EventParticipant> allEntries = eventParticipantRepository.findByEventId(eventId);

        if (allEntries.isEmpty())
            return;

        Set<String> winningPhones = new HashSet<>();
        List<EventWinner> winners = new ArrayList<>();

        // 1. 랜덤 ID 할당 (섞기)
        Collections.shuffle(allEntries);
        // 섞인 리스트의 인덱스를 랜덤 ID처럼 사용합니다.

        // 2. 1등 당첨 번호 생성
        String grandWinNum = generateRandomNumber();
        System.out.println("Winning Number: " + grandWinNum);

        // 3. 당첨자 선정 로직 (이관됨)

        // --- 1등: 특정 전화번호 ---
        EventParticipant rank1 = findByPhone(allEntries, PRE_ASSIGNED_PHONE);
        if (rank1 == null && !allEntries.isEmpty()) {
            rank1 = allEntries.get(0); // 대상자가 없으면 첫 번째 사람을 당첨시킴
        }

        if (rank1 != null) {
            assignWinningNumber(rank1, grandWinNum, 1);
            addWinner(winners, winningPhones, rank1, 1, "1등 상품");
        }

        // 다음 등수 추첨을 위해 당첨자 제외 (불필요해짐, 아래 스트림에서 필터링함)

        // --- 2등: 5명 ---
        // 조건: 기존 당첨자 제외 AND 참여번호 2000 ~ 7000번 사이
        List<EventParticipant> rank2Winners = allEntries.stream()
                .filter(p -> !winningPhones.contains(p.getPhone()))
                .filter(p -> p.getEntryNo() >= 2000 && p.getEntryNo() <= 7000)
                .collect(Collectors.toList());

        Collections.shuffle(rank2Winners);
        rank2Winners.stream().limit(5).forEach(p -> {
            assignWinningNumber(p, grandWinNum, 2);
            addWinner(winners, winningPhones, p, 2, "2등 상품");
        });

        // --- 3등: 44명 ---
        // 조건: 기존 당첨자 제외 AND 참여번호 1000 ~ 8000번 사이
        List<EventParticipant> rank3Winners = allEntries.stream()
                .filter(p -> !winningPhones.contains(p.getPhone()))
                .filter(p -> p.getEntryNo() >= 1000 && p.getEntryNo() <= 8000)
                .collect(Collectors.toList());

        Collections.shuffle(rank3Winners);
        rank3Winners.stream().limit(44).forEach(p -> {
            assignWinningNumber(p, grandWinNum, 3);
            addWinner(winners, winningPhones, p, 3, "3등 상품");
        });

        // --- 4등: 950명 ---
        // 조건: 기존 당첨자 제외 (남은 사람 중 랜덤)
        List<EventParticipant> rank4Valid = allEntries.stream()
                .filter(p -> !winningPhones.contains(p.getPhone()))
                .collect(Collectors.toList());

        Collections.shuffle(rank4Valid);
        rank4Valid.stream().limit(950).forEach(p -> {
            assignWinningNumber(p, grandWinNum, 4);
            addWinner(winners, winningPhones, p, 4, "4등 상품");
        });

        // 4. 낙첨자 처리
        // 4등 추첨 후 남은 인원 (rank4Valid에서 당첨된 사람 제외한 나머지)
        // 위에서 rank4Valid를 셔플했으므로, 950명 이후의 사람들은 자동으로 낙첨
        rank4Valid.stream().skip(950).forEach(p -> {
            assignWinningNumber(p, grandWinNum, 0);
        });

        // 5. 변경사항 저장
        eventParticipantRepository.saveAll(allEntries); // 할당된 로또 번호 저장 (참여 시점의 번호를 덮어씀)
        eventWinnerRepository.saveAll(winners); // 당첨 내역 저장

        // 6. 이벤트 종료 처리
        event.setIsActive(false);
        eventMasterRepository.save(event);
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

        // 1. 일치시킬 인덱스 선택
        int matches = 0;
        while (matches < matchCount) {
            int idx = rnd.nextInt(6);
            if (!isMatch[idx]) {
                isMatch[idx] = true;
                matches++;
            }
        }

        // 2. 불일치 인덱스 변경
        for (int i = 0; i < 6; i++) {
            if (!isMatch[i]) {
                char original = result[i];
                char changed;
                do {
                    changed = (char) ('0' + rnd.nextInt(10));
                } while (changed == original); // 기존 숫자와 다른 숫자로 변경
                result[i] = changed;
            }
        }
        return new String(result);
    }
}
