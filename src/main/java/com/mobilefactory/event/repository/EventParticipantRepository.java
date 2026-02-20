package com.mobilefactory.event.repository;

import com.mobilefactory.event.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Integer> {
    Optional<EventParticipant> findByPhoneAndEventId(String phone, Integer eventId);

    Integer countByEventId(Integer eventId);

    java.util.List<EventParticipant> findByEventId(Integer eventId);
}
