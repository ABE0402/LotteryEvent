package com.mobilefactory.event;

import com.mobilefactory.event.entity.EventMaster;
import com.mobilefactory.event.entity.EventParticipant;
import com.mobilefactory.event.entity.EventWinner;
import com.mobilefactory.event.repository.EventMasterRepository;
import com.mobilefactory.event.repository.EventParticipantRepository;
import com.mobilefactory.event.repository.EventWinnerRepository;
import com.mobilefactory.event.service.LotteryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * JUnit Test for Lottery Logic (Mocked).
 */
@ExtendWith(MockitoExtension.class)
public class LotteryLogicTest {

    @Mock
    private EventMasterRepository eventMasterRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private EventWinnerRepository eventWinnerRepository;

    @InjectMocks
    private LotteryService lotteryService;

    @Test
    public void verifyLotteryLogic() {
        System.out.println("Running Lottery Logic Verification (Refactored with Mocks)...");

        // 1. Setup Data
        int eventId = 1;
        String targetPhone = "010-1234-5678";

        // Mock Event
        EventMaster mockEvent = EventMaster.builder().eventId(eventId).eventName("Test Event").build();
        when(eventMasterRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        // Mock Participants (10,000)
        List<EventParticipant> entries = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            String phone = String.format("010-%04d-%04d", i / 10000, i % 10000);
            entries.add(EventParticipant.builder()
                    .participantId(i) // Set Mock ID
                    .eventId(eventId)
                    .phone(phone)
                    .entryNo(i)
                    .build());
        }
        // Ensure Target exists
        entries.set(0, EventParticipant.builder()
                .participantId(1) // Set Mock ID
                .eventId(eventId)
                .phone(targetPhone)
                .entryNo(1)
                .build());

        when(eventParticipantRepository.findByEventId(eventId)).thenReturn(entries);

        // 2. Run Draw
        lotteryService.drawWinners(eventId);

        // 3. Verify Results via Captor
        ArgumentCaptor<List<EventWinner>> winnerCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventWinnerRepository).saveAll(winnerCaptor.capture());

        List<EventWinner> winners = winnerCaptor.getValue();

        // 4. Verify Logic
        Map<Integer, List<EventWinner>> winnersByRank = winners.stream()
                .collect(Collectors.groupingBy(EventWinner::getWinningRank));

        // No changes needed for map keys as they are integers representing the rank.
        // Just ensuring no direct .rank access remains.
        // The previous replace_file_content already handled
        // .collect(Collectors.groupingBy(EventWinner::getWinningRank));
        // and map keys are Integers.
        // Total Count
        if (winners.size() != 1000)
            throw new RuntimeException("Total winners mismatch: " + winners.size());

        // Rank 1
        if (winnersByRank.get(1).size() != 1)
            throw new RuntimeException("Rank 1 count mismatch");
        Integer rank1Id = winnersByRank.get(1).get(0).getParticipantId();
        if (rank1Id != 1) {
            throw new RuntimeException("Rank 1 participant ID mismatch. Expected 1, got " + rank1Id);
        }

        // Rank 2
        if (winnersByRank.get(2).size() != 5)
            throw new RuntimeException("Rank 2 count mismatch");

        // Rank 3
        if (winnersByRank.get(3).size() != 44)
            throw new RuntimeException("Rank 3 count mismatch");

        // Rank 4
        if (winnersByRank.get(4).size() != 950)
            throw new RuntimeException("Rank 4 count mismatch");

        System.out.println("SUCCESS: All Logic Verified!");
    }
}
