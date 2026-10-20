-- =====================================================================
-- Railway Reservation and Platform Allocation System  (Payanam Junction)
-- Run this whole script once in MySQL. Running it again RESETS all data.
-- =====================================================================

DROP DATABASE IF EXISTS railway_reservation;
CREATE DATABASE railway_reservation CHARACTER SET utf8mb4;
USE railway_reservation;

-- ---------------------------------------------------------------------
-- 1. TRAINS : timetable of the station (times are time-of-day, 24 hour)
--    One seat class with 10 seats per train (seats are numbered 1..10
--    in the reservations table and in the Java array).
-- ---------------------------------------------------------------------
CREATE TABLE trains (
    train_id            INT AUTO_INCREMENT PRIMARY KEY,
    train_name          VARCHAR(60) NOT NULL,
    origin              VARCHAR(40) NOT NULL,
    destination         VARCHAR(40) NOT NULL,
    arrival_time        TIME        NOT NULL,
    departure_time      TIME        NOT NULL,
    train_length        INT         NOT NULL,          -- metres
    seat_class          VARCHAR(30) NOT NULL DEFAULT 'Second Sitting',
    arrival_delay_min   INT         NOT NULL DEFAULT 0, -- minutes late at arrival
    departure_delay_min INT         NOT NULL DEFAULT 0, -- minutes late at departure
    CONSTRAINT chk_train_times  CHECK (departure_time > arrival_time),
    CONSTRAINT chk_train_length CHECK (train_length > 0)
) AUTO_INCREMENT = 101;

-- ---------------------------------------------------------------------
-- 2. PASSENGERS
-- ---------------------------------------------------------------------
CREATE TABLE passengers (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(60) NOT NULL,
    age          INT         NOT NULL,
    phone        VARCHAR(15) NOT NULL,
    CONSTRAINT chk_passenger_age CHECK (age BETWEEN 1 AND 120)
);

-- ---------------------------------------------------------------------
-- 3. RESERVATIONS : one row per booking request
--    CONFIRMED -> has a seat_no (1..10)
--    WAITING   -> seat_no is NULL, FIFO order = order of reservation_id
--    CANCELLED -> seat_no is NULL
--    UNIQUE(train_id, seat_no) makes it impossible to give one seat to
--    two passengers (NULLs are ignored by the UNIQUE rule).
-- ---------------------------------------------------------------------
CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    train_id       INT NOT NULL,
    passenger_id   INT NOT NULL,
    seat_no        INT NULL,
    status         ENUM('CONFIRMED','WAITING','CANCELLED') NOT NULL,
    booked_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_train     FOREIGN KEY (train_id)     REFERENCES trains(train_id),
    CONSTRAINT fk_res_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(passenger_id),
    CONSTRAINT uq_train_seat    UNIQUE (train_id, seat_no),
    CONSTRAINT chk_seat_range   CHECK (seat_no IS NULL OR seat_no BETWEEN 1 AND 10)
);

-- ---------------------------------------------------------------------
-- 4. PLATFORMS : the 3 platforms of the station
-- ---------------------------------------------------------------------
CREATE TABLE platforms (
    platform_id   INT PRIMARY KEY,
    platform_name VARCHAR(30) NOT NULL,
    length_meters INT NOT NULL,
    CONSTRAINT chk_platform_length CHECK (length_meters > 0)
);

-- ---------------------------------------------------------------------
-- 5. PLATFORM_REQUESTS : a train asking for a platform
--    request_type : EMERGENCY (higher priority) or SCHEDULED
--    status       : PENDING -> ALLOCATED -> OCCUPYING -> DEPARTED
--    platform_id  : NULL until a platform is allocated
--    One request per train (UNIQUE train_id).
-- ---------------------------------------------------------------------
CREATE TABLE platform_requests (
    request_id   INT AUTO_INCREMENT PRIMARY KEY,
    train_id     INT NOT NULL UNIQUE,
    request_type ENUM('EMERGENCY','SCHEDULED') NOT NULL,
    status       ENUM('PENDING','ALLOCATED','OCCUPYING','DEPARTED') NOT NULL DEFAULT 'PENDING',
    platform_id  INT NULL,
    CONSTRAINT fk_req_train    FOREIGN KEY (train_id)    REFERENCES trains(train_id),
    CONSTRAINT fk_req_platform FOREIGN KEY (platform_id) REFERENCES platforms(platform_id)
);

-- =====================================================================
-- SAMPLE DATA
-- =====================================================================

-- 6 trains (101..106). Train 106 (500 m) is longer than every platform.
INSERT INTO trains (train_id, train_name, origin, destination, arrival_time, departure_time, train_length) VALUES
(101, 'Malabar Express',    'Chennai Central', 'Kannur',          '08:00:00', '08:20:00', 420),
(102, 'Kerala Link',        'Coimbatore',      'Thiruvananthapuram','08:10:00', '08:30:00', 300),
(103, 'Coastal Passenger',  'Mangaluru',       'Kozhikode',       '08:15:00', '08:35:00', 200),
(104, 'Rajdhani Connect',   'New Delhi',       'Ernakulam',       '08:12:00', '08:45:00', 300),
(105, 'Netravati Express',  'Mumbai',          'Thiruvananthapuram','09:30:00', '09:50:00', 280),
(106, 'Heritage Special',   'Mysuru',          'Kochi',           '08:20:00', '08:50:00', 500);

-- 3 platforms
INSERT INTO platforms (platform_id, platform_name, length_meters) VALUES
(1, 'Platform 1', 450),
(2, 'Platform 2', 350),
(3, 'Platform 3', 250);

-- 15 passengers
INSERT INTO passengers (name, age, phone) VALUES
('Arun Kumar',     34, '9840011001'),
('Meena Lakshmi',  29, '9840011002'),
('Rahul Nair',     41, '9840011003'),
('Divya Menon',    26, '9840011004'),
('Suresh Babu',    52, '9840011005'),
('Anitha Raj',     38, '9840011006'),
('Vijay Anand',    31, '9840011007'),
('Fathima Beevi',  45, '9840011008'),
('Joseph Mathew',  60, '9840011009'),
('Kavitha Devi',   33, '9840011010'),
('Mohan Das',      47, '9840011011'),
('Priya Sharma',   24, '9840011012'),
('Ibrahim Khan',   36, '9840011013'),
('Lakshmi Narayan',55, '9840011014'),
('Sneha George',   28, '9840011015');

-- 30 booking requests (ids 1..30 are created in this order)
--  Train 101: 10 confirmed (FULL) + 3 waiting   -> ids 1..13
--  Train 102:  7 confirmed                        -> ids 14..20
--  Train 103:  6 confirmed                        -> ids 21..26
--  Train 104:  4 confirmed                        -> ids 27..30
INSERT INTO reservations (train_id, passenger_id, seat_no, status) VALUES
(101,  1,  1, 'CONFIRMED'),
(101,  2,  2, 'CONFIRMED'),
(101,  3,  3, 'CONFIRMED'),
(101,  4,  4, 'CONFIRMED'),
(101,  5,  5, 'CONFIRMED'),
(101,  6,  6, 'CONFIRMED'),
(101,  7,  7, 'CONFIRMED'),
(101,  8,  8, 'CONFIRMED'),
(101,  9,  9, 'CONFIRMED'),
(101, 10, 10, 'CONFIRMED'),
(101, 11, NULL, 'WAITING'),
(101, 12, NULL, 'WAITING'),
(101, 13, NULL, 'WAITING'),
(102,  2,  1, 'CONFIRMED'),
(102,  3,  2, 'CONFIRMED'),
(102,  4,  3, 'CONFIRMED'),
(102,  5,  4, 'CONFIRMED'),
(102,  6,  5, 'CONFIRMED'),
(102,  7,  6, 'CONFIRMED'),
(102,  8,  7, 'CONFIRMED'),
(103,  9,  1, 'CONFIRMED'),
(103, 10,  2, 'CONFIRMED'),
(103, 11,  3, 'CONFIRMED'),
(103, 12,  4, 'CONFIRMED'),
(103, 13,  5, 'CONFIRMED'),
(103, 14,  6, 'CONFIRMED'),
(104,  1,  1, 'CONFIRMED'),
(104,  5,  2, 'CONFIRMED'),
(104,  9,  3, 'CONFIRMED'),
(104, 15,  4, 'CONFIRMED');

-- Platform requests (all PENDING: use "Allocate platforms" to process them)
INSERT INTO platform_requests (train_id, request_type) VALUES
(101, 'SCHEDULED'),
(102, 'SCHEDULED'),
(103, 'SCHEDULED'),
(104, 'EMERGENCY'),
(105, 'SCHEDULED'),
(106, 'SCHEDULED');

-- Quick check
SELECT 'trains' AS tbl, COUNT(*) AS rows_count FROM trains
UNION ALL SELECT 'passengers', COUNT(*) FROM passengers
UNION ALL SELECT 'reservations', COUNT(*) FROM reservations
UNION ALL SELECT 'platforms', COUNT(*) FROM platforms
UNION ALL SELECT 'platform_requests', COUNT(*) FROM platform_requests;
