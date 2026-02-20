package com.mobilefactory.event.repository;

import com.mobilefactory.event.entity.EventMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventMasterRepository extends JpaRepository<EventMaster, Integer> {
    // Optionally find active event
    Optional<EventMaster> findByIsActiveTrue();
}
