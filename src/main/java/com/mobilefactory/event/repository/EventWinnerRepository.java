package com.mobilefactory.event.repository;

import com.mobilefactory.event.entity.EventWinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventWinnerRepository extends JpaRepository<EventWinner, Integer> {
    List<EventWinner> findByEventId(Integer eventId);

    Optional<EventWinner> findByParticipantId(Integer participantId);
    // Join might be better, but for simple checking by phone in service we might
    // need custom query or logic
    // Actually, to check result by phone, we usually look up participant first then
    // winner
}
