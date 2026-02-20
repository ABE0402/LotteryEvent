package com.mobilefactory.event.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_EVENT_PARTICIPANT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Integer participantId;

    @Column(name = "event_id", nullable = false)
    private Integer eventId;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "name")
    private String name;

    @Column(name = "entry_no", nullable = false)
    private Integer entryNo;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "lottery_number")
    private String lotteryNumber;

    @PrePersist
    protected void onCreate() {
        regDt = LocalDateTime.now();
    }
}
