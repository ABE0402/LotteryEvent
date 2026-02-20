package com.mobilefactory.event.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_EVENT_WINNER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "winner_id")
    private Integer winnerId;

    @Column(name = "event_id", nullable = false)
    private Integer eventId;

    @Column(name = "participant_id", nullable = false)
    private Integer participantId;

    @Column(name = "winning_rank", nullable = false)
    private Integer winningRank;

    @Column(name = "prize_name", nullable = false)
    private String prizeName;

    @Column(name = "win_dt")
    private LocalDateTime winDt;

    @Column(name = "check_count")
    private Integer checkCount;

    @Column(name = "last_check_dt")
    private LocalDateTime lastCheckDt;

    @PrePersist
    protected void onCreate() {
        winDt = LocalDateTime.now();
        if (checkCount == null) {
            checkCount = 0;
        }
    }

}
