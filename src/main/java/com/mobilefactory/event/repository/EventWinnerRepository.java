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
    // Join을 사용하는 것이 더 나을 수 있지만, 서비스에서 전화번호로 간단히 확인하기 위해
    // 커스텀 쿼리나 로직이 필요할 수 있습니다.
    // 실제로 전화번호로 결과를 확인하려면 보통 참가자를 먼저 조회한 후
    // 당첨 여부를 확인합니다.
}
