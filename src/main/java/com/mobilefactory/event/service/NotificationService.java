package com.mobilefactory.event.service;

import com.mobilefactory.event.entity.EventMaster;
import com.mobilefactory.event.entity.EventWinner;
import com.mobilefactory.event.repository.EventMasterRepository;
import com.mobilefactory.event.repository.EventParticipantRepository;
import com.mobilefactory.event.repository.EventWinnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EventMasterRepository eventMasterRepository;
    private final EventWinnerRepository eventWinnerRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final SmsService smsService;

    // 매일 오전 10시에 실행
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void sendLateWinnerNotification() {
        log.info("Starting Late Winner Notification Job");

        // 1. 종료된 이벤트 조회 (단순화: 모든 이벤트 조회 후 필터링)
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);

        List<EventMaster> endedEvents = eventMasterRepository.findAll().stream()
                .filter(e -> !e.getIsActive()) // 비활성 이벤트
                .filter(e -> e.getEndDt() != null && e.getEndDt().isBefore(tenDaysAgo)) // 종료일이 10일 지났는지
                .collect(Collectors.toList());

        for (EventMaster event : endedEvents) {
            // 2. 미확인 당첨자 조회
            List<EventWinner> uncheckedWinners = eventWinnerRepository.findByEventId(event.getEventId()).stream()
                    .filter(w -> w.getCheckCount() == 0)
                    .collect(Collectors.toList());

            for (EventWinner winner : uncheckedWinners) {
                // 3. 참여자 정보 조회
                eventParticipantRepository.findById(winner.getParticipantId()).ifPresent(participant -> {
                    // 4. 문자 발송
                    String message = String.format("[이벤트] %s님, 축하합니다! %s에 당첨되셨으나 아직 확인하지 않으셨습니다. 홈페이지에서 확인해주세요.",
                            participant.getName(), winner.getPrizeName());
                    smsService.sendSms(participant.getPhone(), message);

                    log.info("Sent late notification to: {}, Event: {}", participant.getPhone(), event.getEventName());
                });
            }
        }
    }
}
