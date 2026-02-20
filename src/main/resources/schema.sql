-- ==========================================
-- Mobile Factory Lottery Event System Schema
-- Database: MySQL / MariaDB
-- ==========================================

-- 1. Event Master Table
-- Manages event period and status.
CREATE TABLE TB_EVENT_MASTER (
    event_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Event ID',
    event_name VARCHAR(100) NOT NULL COMMENT 'Event Name',
    start_dt DATETIME NOT NULL COMMENT 'Event Start Date (2025-02-01)',
    end_dt DATETIME NOT NULL COMMENT 'Event End Date (2025-03-31)',
    announce_dt DATETIME NOT NULL COMMENT 'Announcement Date (2025-04-01)',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Active Status',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Event Participant Table
-- Stores participant info.
-- 'entry_no' tracks the join order (Audit purpose).
-- Actual ranking logic uses an internal Random ID generated at draw time.
CREATE TABLE TB_EVENT_PARTICIPANT (
    participant_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Internal ID',
    event_id INT NOT NULL,
    phone VARCHAR(20) NOT NULL COMMENT 'Phone Number (Unique per Event)',
    name VARCHAR(50) COMMENT 'User Name',
    entry_no INT NOT NULL COMMENT 'Participation Order (1 ~ 10000)',
    ip_address VARCHAR(40) COMMENT 'IP Address for auditing',
    reg_dt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Participation Time',
    lottery_number CHAR(6) COMMENT 'Assigned Lottery Number (Generated on Draw Day)',
    
    UNIQUE KEY uk_phone_event (event_id, phone),
    UNIQUE KEY uk_entry_no (event_id, entry_no),
    FOREIGN KEY (event_id) REFERENCES TB_EVENT_MASTER(event_id)
);

-- 3. Event Winner Table
-- Stores the draw results.
CREATE TABLE TB_EVENT_WINNER (
    winner_id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    participant_id INT NOT NULL,
    winning_rank INT NOT NULL COMMENT '1=1st, 2=2nd, 3=3rd, 4=4th',
    prize_name VARCHAR(100) NOT NULL COMMENT 'Prize Description',
    win_dt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Winning Determination Time',
    
    FOREIGN KEY (event_id) REFERENCES TB_EVENT_MASTER(event_id),
    FOREIGN KEY (participant_id) REFERENCES TB_EVENT_PARTICIPANT(participant_id)
);

-- 4. SMS Log Table (Optional)
-- Records sent SMS messages.
CREATE TABLE TB_SMS_LOG (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    sent_dt DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(10) DEFAULT 'SENT' COMMENT 'SENT/FAIL'
);

-- ==========================================
-- Index Strategy Override
-- ==========================================
-- TB_EVENT_MASTER: Frequent lookup for active event
CREATE INDEX idx_master_active ON TB_EVENT_MASTER(is_active);

-- TB_EVENT_PARTICIPANT: 
-- 1. Phone search is covered by UK(event_id, phone) for specific event
-- 2. Global phone search (history) might need:
CREATE INDEX idx_part_phone ON TB_EVENT_PARTICIPANT(phone);

-- TB_EVENT_WINNER:
--FK columns usually benefit from indexes for JOIN performance
CREATE INDEX idx_winner_event ON TB_EVENT_WINNER(event_id);
CREATE INDEX idx_winner_part ON TB_EVENT_WINNER(participant_id);

-- Initial Data Seeding
INSERT INTO TB_EVENT_MASTER (event_name, start_dt, end_dt, announce_dt)
VALUES ('2025 Happy Lotto Event', '2025-02-01 00:00:00', '2025-03-31 23:59:59', '2025-04-01 00:00:00');
