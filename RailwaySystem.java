import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

/**
 * RailwaySystem - the "brain". Both Main (console) and WebServer (browser) call these methods,
 * so the rules are written only once and both programs behave the same.
 *
 * Every method re-reads the database first (reload), so a change made by the other program
 * is always seen. Changes are saved inside a transaction (all-or-nothing).
 *
 * HashMap       -> fast lookup of trains, passengers, reservations, platforms, requests by id
 * PriorityQueue -> platform requests ordered by priority; also used as a min-heap of release times
 */
public class RailwaySystem {

    private final HashMap<Integer, TrainService> trains = new HashMap<>();
    private final HashMap<Integer, Passenger> passengers = new HashMap<>();
    private final HashMap<Integer, Reservation> reservations = new HashMap<>();
    private final HashMap<Integer, Platform> platforms = new HashMap<>();
    private final HashMap<Integer, PlatformRequest> requests = new HashMap<>();

    // =====================================================================
    // Time helpers (times are kept as minutes after midnight)
    // =====================================================================
    public static int parseTime(String text) {
        String[] parts = text.trim().split(":");
        if (parts.length < 2) throw new IllegalArgumentException("Time must look like HH:MM (example 08:30).");
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException("Time must be between 00:00 and 23:59.");
            return h * 60 + m;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Time must look like HH:MM (example 08:30).");
        }
    }

    public static String formatTime(int minutes) {
        return String.format("%02d:%02d", (minutes / 60) % 24, minutes % 60);
    }

    // =====================================================================
    // Loading everything from MySQL into the Java collections
    // =====================================================================
    private void reload(Connection conn) throws SQLException {
        trains.clear(); passengers.clear(); reservations.clear(); platforms.clear(); requests.clear();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM trains");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TrainService t = new TrainService(rs.getInt("train_id"), rs.getString("train_name"),
                        rs.getString("origin"), rs.getString("destination"),
                        parseTime(rs.getString("arrival_time")), parseTime(rs.getString("departure_time")),
                        rs.getInt("train_length"), rs.getString("seat_class"));
                t.setArrivalDelay(rs.getInt("arrival_delay_min"));
                t.setDepartureDelay(rs.getInt("departure_delay_min"));
                trains.put(t.getTrainId(), t);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM passengers");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Passenger p = new Passenger(rs.getInt("passenger_id"), rs.getString("name"),
                        rs.getInt("age"), rs.getString("phone"));
                passengers.put(p.getPassengerId(), p);
            }
        }

        String resSql = "SELECT r.*, p.name AS passenger_name FROM reservations r "
        + "JOIN passengers p ON p.passenger_id = r.passenger_id ORDER BY r.reservation_id";
        try (PreparedStatement ps = conn.prepareStatement(resSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Reservation r = new Reservation(rs.getInt("reservation_id"), rs.getInt("train_id"),
                        rs.getInt("passenger_id"), rs.getInt("seat_no"),   // NULL becomes 0
                        rs.getString("status"), rs.getString("booked_at"), rs.getString("passenger_name"));
                reservations.put(r.getReservationId(), r);
                trains.get(r.getTrainId()).restore(r);   // seats array / waiting queue / history
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM platforms");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Platform p = new Platform(rs.getInt("platform_id"), rs.getString("platform_name"),
                        rs.getInt("length_meters"));
                platforms.put(p.getPlatformId(), p);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM platform_requests");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TrainService t = trains.get(rs.getInt("train_id"));
                PlatformRequest req = createRequest(rs.getString("request_type"), rs.getInt("request_id"),
                        t, rs.getString("status"), rs.getInt("platform_id"));   // NULL becomes 0
                requests.put(req.getRequestId(), req);
                if (req.isActive() && platforms.containsKey(req.getPlatformId())) {
                    platforms.get(req.getPlatformId()).addExistingBooking(req);
                }
            }
        }
    }

    /** POLYMORPHISM: the variable type is PlatformRequest, the object is Emergency or Scheduled. */
    private PlatformRequest createRequest(String type, int id, TrainService t, String status, int platformId) {
        if ("EMERGENCY".equals(type)) return new EmergencyArrival(id, t, status, platformId);
        return new ScheduledArrival(id, t, status, platformId);
    }

    private PlatformRequest findRequestByTrain(int trainId) {
        for (PlatformRequest r : requests.values()) {
            if (r.getTrainId() == trainId) return r;
        }
        return null;
    }

    private void lockTrain(Connection conn, int trainId) throws SQLException {
    String sql = "SELECT train_id FROM trains WHERE train_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, trainId);

        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("Train ID not found.");
            }
        }
    }
}

    
private void lockPlatforms(Connection conn) throws SQLException {
    String sql = "SELECT platform_id FROM platforms";

    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            rs.getInt("platform_id");
        }
    }
}




    private void saveRequest(Connection conn, PlatformRequest r) throws SQLException {
        String sql = "UPDATE platform_requests SET platform_id = ?, status = ? WHERE request_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (r.getPlatformId() == 0) ps.setNull(1, Types.INTEGER); else ps.setInt(1, r.getPlatformId());
            ps.setString(2, r.getStatus());
            ps.setInt(3, r.getRequestId());
            ps.executeUpdate();
        }
    }

    // =====================================================================
    // Read-only methods (used by the "view" menu items and web pages)
    // =====================================================================
    /** Timetable sorted by arrival time (Collections.sort on an ArrayList). */
    public List<TrainService> getTrains() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            List<TrainService> list = new ArrayList<>(trains.values());
            Collections.sort(list);
            return list;
        }
    }

    public List<Passenger> getPassengers() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            List<Passenger> list = new ArrayList<>(passengers.values());
            list.sort(Comparator.comparingInt(Passenger::getPassengerId));
            return list;
        }
    }

    /** trainId = 0 means all trains. Newest reservations last. */
    public List<Reservation> getReservations(int trainId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            List<Reservation> list = new ArrayList<>();
            for (Reservation r : reservations.values()) {
                if (trainId == 0 || r.getTrainId() == trainId) list.add(r);
            }
            list.sort(Comparator.comparingInt(Reservation::getReservationId));
            return list;
        }
    }

    /** Waiting list in FIFO order. trainId = 0 means all trains (train by train). */
    public List<Reservation> getWaitingList(int trainId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            List<Reservation> list = new ArrayList<>();
            List<TrainService> ordered = new ArrayList<>(trains.values());
            ordered.sort(Comparator.comparingInt(TrainService::getTrainId));
            for (TrainService t : ordered) {
                if (trainId == 0 || t.getTrainId() == trainId) list.addAll(t.getWaitingList());
            }
            return list;
        }
    }

    public List<PlatformRequest> getPlatformRequests() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            List<PlatformRequest> list = new ArrayList<>(requests.values());
            list.sort(Comparator.comparingInt(PlatformRequest::getRequestId));
            return list;
        }
    }

    public List<Platform> getPlatformStatus() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            List<Platform> list = new ArrayList<>(platforms.values());
            list.sort(Comparator.comparingInt(Platform::getPlatformId));
            return list;
        }
    }

    /** MIN-HEAP of release times: the platform that becomes free first is at the top. */
    public String getNextReleaseSummary(List<Platform> list) {
        PriorityQueue<Platform> heap = new PriorityQueue<>(Comparator.comparingInt(Platform::getNextRelease));
        for (Platform p : list) {
            if (p.getNextRelease() != Integer.MAX_VALUE) heap.add(p);
        }
        if (heap.isEmpty()) return "No platform has a booking, so all platforms are free.";
        Platform first = heap.peek();
        return "Next platform release: " + first.getName() + " at " + formatTime(first.getNextRelease())
                + " (departure + " + PlatformRequest.CLEARANCE_BUFFER + " min clearance).";
    }

    // =====================================================================
    // Trains and passengers
    // =====================================================================
    public String addTrain(String name, String origin, String destination,
                           String arrival, String departure, int length) throws SQLException {
        if (name.trim().isEmpty() || origin.trim().isEmpty() || destination.trim().isEmpty()) {
            return "ERROR: Name, origin and destination cannot be empty.";
        }
        int a, d;
        try {
            a = parseTime(arrival);
            d = parseTime(departure);
        } catch (IllegalArgumentException e) {
            return "ERROR: " + e.getMessage();
        }
        if (d <= a) return "ERROR: Departure time must be after arrival time.";
        if (length <= 0) return "ERROR: Train length must be more than 0 metres.";

        String sql = "INSERT INTO trains (train_name, origin, destination, arrival_time, departure_time, train_length) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.setString(2, origin.trim());
            ps.setString(3, destination.trim());
            ps.setString(4, formatTime(a) + ":00");
            ps.setString(5, formatTime(d) + ":00");
            ps.setInt(6, length);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return "OK: Train added with ID " + keys.getInt(1) + " (" + TrainService.TOTAL_SEATS + " seats created).";
            }
        }
    }

    public String addPassenger(String name, int age, String phone) throws SQLException {
        Passenger p;
        try {
            p = new Passenger(0, name, age, phone);    // the constructor validates the data
        } catch (IllegalArgumentException e) {
            return "ERROR: " + e.getMessage();
        }
        String sql = "INSERT INTO passengers (name, age, phone) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return "OK: Passenger registered with ID " + keys.getInt(1) + ".";
            }
        }
    }

    // =====================================================================
    // Booking and cancelling
    // =====================================================================
    public String bookTicket(int trainId, int passengerId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);                     // start a transaction
            try {
                lockTrain(conn, trainId);                  // nobody else can book this train until we finish
                reload(conn);
                TrainService train = trains.get(trainId);
                Passenger passenger = passengers.get(passengerId);
                if (train == null) return "ERROR: Train " + trainId + " does not exist.";
                if (passenger == null) return "ERROR: Passenger " + passengerId + " does not exist.";
                PlatformRequest req = findRequestByTrain(trainId);
                if (req != null && PlatformRequest.DEPARTED.equals(req.getStatus())) {
                    return "ERROR: Train " + trainId + " has already departed.";
                }
                if (train.hasActiveBooking(passengerId)) {
                    return "ERROR: " + passenger.getName() + " already has an active booking on this train.";
                }

                Reservation r = new Reservation(0, trainId, passengerId, 0, Reservation.WAITING, null, passenger.getName());
                train.bookSeat(r);                          // seat from the array, or end of the waiting queue

                String sql = "INSERT INTO reservations (train_id, passenger_id, seat_no, status) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, trainId);
                    ps.setInt(2, passengerId);
                    if (r.getSeatNo() == 0) ps.setNull(3, Types.INTEGER); else ps.setInt(3, r.getSeatNo());
                    ps.setString(4, r.getStatus());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        r.setReservationId(keys.getInt(1));
                    }
                }
                conn.commit();

                if (Reservation.CONFIRMED.equals(r.getStatus())) {
                    return "OK: Ticket CONFIRMED. Reservation ID " + r.getReservationId() + ", seat "
                            + r.getSeatNo() + " on train " + train.getTrainName() + " for " + passenger.getName() + ".";
                }
                return "OK: Train is FULL. " + passenger.getName() + " added to the WAITING list at position "
                        + r.getWaitingPosition() + ". Reservation ID " + r.getReservationId() + ".";
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public String cancelTicket(int reservationId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int trainId = 0;
                try (PreparedStatement ps = conn.prepareStatement("SELECT train_id FROM reservations WHERE reservation_id = ?")) {
                    ps.setInt(1, reservationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) trainId = rs.getInt(1);
                    }
                }
                if (trainId == 0) return "ERROR: Reservation " + reservationId + " does not exist.";

                lockTrain(conn, trainId);
                reload(conn);
                Reservation r = reservations.get(reservationId);
                if (!r.isActive()) return "ERROR: Reservation " + reservationId + " is already cancelled.";
                TrainService train = trains.get(trainId);

                boolean hadSeat = Reservation.CONFIRMED.equals(r.getStatus());
                Reservation promoted = train.cancelBooking(r);  
                 // may promote the oldest waiting passenger

                // free the seat first, then give it to the promoted passenger (seat numbers are UNIQUE)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE reservations SET status = 'CANCELLED', seat_no = NULL WHERE reservation_id = ?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }
                if (promoted != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE reservations SET status = 'CONFIRMED', seat_no = ? WHERE reservation_id = ?")) {
                        ps.setInt(1, promoted.getSeatNo());
                        ps.setInt(2, promoted.getReservationId());
                        ps.executeUpdate();
                    }
                }
                conn.commit();

                String msg = "OK: Reservation " + reservationId + " (" + r.getPassengerName() + ") CANCELLED.";
                if (promoted != null) {
                    msg += " Waiting passenger " + promoted.getPassengerName() + " (reservation "
                            + promoted.getReservationId() + ") promoted to CONFIRMED seat " + promoted.getSeatNo() + ".";
                } else if (hadSeat) {
                    msg += " Seat is now free (nobody was waiting).";
                }
                return msg;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Manual "manage waiting list" action: fill free seats from the waiting queue. */
    public String promoteWaiting(int trainId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockTrain(conn, trainId);
                reload(conn);
                TrainService train = trains.get(trainId);
                if (train == null) return "ERROR: Train " + trainId + " does not exist.";
                List<Reservation> promoted = train.promoteWaiting();
                if (promoted.isEmpty()) return "OK: Nothing to promote (no free seat or empty waiting list).";
                StringBuilder sb = new StringBuilder("OK: Promoted " + promoted.size() + " passenger(s):");
                for (Reservation p : promoted) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE reservations SET status = 'CONFIRMED', seat_no = ? WHERE reservation_id = ?")) {
                        ps.setInt(1, p.getSeatNo());
                        ps.setInt(2, p.getReservationId());
                        ps.executeUpdate();
                    }
                    sb.append(" ").append(p.getPassengerName()).append(" -> seat ").append(p.getSeatNo()).append(";");
                }
                conn.commit();
                return sb.toString();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // =====================================================================
    // Platform requests and allocation
    // =====================================================================
    public String addPlatformRequest(int trainId, String type) throws SQLException {
        String t = type.trim().toUpperCase();
        if (t.startsWith("E")) t = "EMERGENCY";
        else if (t.startsWith("S")) t = "SCHEDULED";
        else return "ERROR: Type must be EMERGENCY or SCHEDULED.";

        try (Connection conn = DBConnection.getConnection()) {
            reload(conn);
            if (!trains.containsKey(trainId)) return "ERROR: Train " + trainId + " does not exist.";
            if (findRequestByTrain(trainId) != null) return "ERROR: Train " + trainId + " already has a platform request.";
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO platform_requests (train_id, request_type) VALUES (?, ?)")) {
                ps.setInt(1, trainId);
                ps.setString(2, t);
                ps.executeUpdate();
            }
            return "OK: " + t + " platform request added for train " + trainId + " (status PENDING).";
        }
    }

    public String allocatePlatforms() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockPlatforms(conn);
                reload(conn);
                List<String> log = new ArrayList<>();
                runAllocation(conn, log);
                conn.commit();
                return String.join("\n", log);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
    


public String confirmDeparture(String trainId) {
    String sql = "UPDATE trains SET " +
                 "departure_status = 'DEPARTED', " +
                 "actual_departure_time = time('now', 'localtime') " +
                 "WHERE train_id = ? " +
                 "AND departure_status = 'SCHEDULED'";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, trainId);

        int rows = ps.executeUpdate();

        if (rows > 0) {
            return "Train " + trainId +
                   " departure confirmed successfully.";
        }

        return "Train not found or departure already confirmed.";

    } catch (Exception e) {
        return "Departure Confirmation Error: " + e.getMessage();
    }
}
public String delayDepartureTrain(String trainId, int delayMinutes) {
    try {
        if (delayMinutes <= 0) {
            return "Delay must be greater than zero.";
        }

        String sql = "UPDATE trains SET " +
                     "departure_time = time(departure_time, '+' || ? || ' minutes'), " +
                     "departure_delay_min = COALESCE(departure_delay_min, 0) + ? " +
                     "WHERE train_id = ? " +
                     "AND departure_status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, delayMinutes);
            ps.setInt(2, delayMinutes);
            ps.setString(3, trainId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                return "Train " + trainId +
                       " departure delayed successfully by " +
                       delayMinutes + " minutes.";
            }

            return "Train not found or departure already confirmed.";
        }

    } catch (Exception e) {
        return "Departure Delay Error: " + e.getMessage();
    }
}

    /**
     * The allocation algorithm (written by us, not a library):
     * 1. Put every PENDING request into a PriorityQueue (emergency first, then earlier arrival, then id).
     * 2. Take requests one by one from the top of the queue.
     * 3. Try platforms from the shortest to the longest; the first one that is long enough
     *    AND has no overlapping booking is used (smallest suitable platform = best fit).
     * 4. ALLOCATED / OCCUPYING requests are never touched, so an occupying train is never displaced.
     */
    private void runAllocation(Connection conn, List<String> log) throws SQLException {
        PriorityQueue<PlatformRequest> queue = new PriorityQueue<>();
        for (PlatformRequest r : requests.values()) {
            if (PlatformRequest.PENDING.equals(r.getStatus())) queue.add(r);
        }
        if (queue.isEmpty()) {
            log.add("No pending platform requests.");
            return;
        }
        List<Platform> byLength = new ArrayList<>(platforms.values());
        byLength.sort(Comparator.comparingInt(Platform::getLengthMeters).thenComparingInt(Platform::getPlatformId));

        while (!queue.isEmpty()) {
            PlatformRequest req = queue.poll();
            Platform chosen = null;
            for (Platform p : byLength) {
                if (p.occupyPlatform(req)) { chosen = p; break; }
            }
            String who = "[" + req.getTypeName() + "] Train " + req.getTrainId() + " " + req.getTrain().getTrainName()
                    + " (" + req.getTrainLength() + " m, " + formatTime(req.getStart()) + "-" + formatTime(req.getEnd()) + ")";
            if (chosen != null) {
                req.setPlatformId(chosen.getPlatformId());
                req.setStatus(PlatformRequest.ALLOCATED);
                saveRequest(conn, req);
                log.add(who + " -> ALLOCATED to " + chosen.getName());
            } else {
                log.add(who + " -> WAITING. " + explainFailure(req, byLength));
            }
        }
    }

    private String explainFailure(PlatformRequest req, List<Platform> byLength) {
        int longest = 0;
        for (Platform p : byLength) longest = Math.max(longest, p.getLengthMeters());
        if (req.getTrainLength() > longest) {
            return "Train is " + req.getTrainLength() + " m but the longest platform is only " + longest + " m.";
        }
        PriorityQueue<Integer> releases = new PriorityQueue<>();   // min-heap of release times
        for (Platform p : byLength) {
            if (p.getLengthMeters() >= req.getTrainLength()) {
                for (PlatformRequest b : p.getBookings()) {
                    if (b.getEnd() > req.getStart()) releases.add(b.getEnd());
                }
            }
        }
        String msg = "Every long-enough platform is busy in that time window.";
        if (!releases.isEmpty()) msg += " Earliest release: " + formatTime(releases.peek()) + ".";
        return msg;
    }

    public String confirmArrival(int trainId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockPlatforms(conn);
                reload(conn);
                PlatformRequest req = findRequestByTrain(trainId);
                if (req == null) return "ERROR: Train " + trainId + " has no platform request.";
                if (!PlatformRequest.ALLOCATED.equals(req.getStatus())) {
                    return "ERROR: Train " + trainId + " is " + req.getStatus() + ". It needs a platform (ALLOCATED) before arrival.";
                }
                req.setStatus(PlatformRequest.OCCUPYING);
                saveRequest(conn, req);
                conn.commit();
                return "OK: Train " + trainId + " arrived and is now OCCUPYING platform " + req.getPlatformId() + ".";
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public String confirmDeparture(int trainId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockPlatforms(conn);
                reload(conn);
                PlatformRequest req = findRequestByTrain(trainId);
                if (req == null) return "ERROR: Train " + trainId + " has no platform request.";
                if (!PlatformRequest.OCCUPYING.equals(req.getStatus())) {
                    return "ERROR: Train " + trainId + " is " + req.getStatus() + ". Only an OCCUPYING train can depart.";
                }
                req.setStatus(PlatformRequest.DEPARTED);
                saveRequest(conn, req);          // platform_id is kept as history
                conn.commit();
                return "OK: Train " + trainId + " departed. Platform " + req.getPlatformId()
                        + " is free. Run 'Allocate platforms' to place waiting trains.";
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * A delay can break the prepared platform plan:
     * - train not yet arrived: its platform plan is cancelled and the request is queued again;
     * - train already on a platform: only its departure moves later (occupation is extended);
     *   any ALLOCATED train that now overlaps on the same platform goes back to the queue
     *   (the occupying train is NEVER displaced).
     * Afterwards the allocation algorithm runs again.
     */
    
    public String delayTrain(String trainId, int delayMinutes) {
    try {
        if (delayMinutes <= 0) {
            return "Delay must be greater than zero.";
        }

        String sql = "UPDATE trains SET " +
                     "arrival_time = time(arrival_time, '+' || ? || ' minutes'), " +
                     "arrival_delay_min = COALESCE(arrival_delay_min, 0) + ? " +
                     "WHERE train_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, delayMinutes);
            ps.setInt(2, delayMinutes);
            ps.setString(3, trainId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                return "Train " + trainId +
                       " arrival delayed successfully by " +
                       delayMinutes + " minutes.";
            }

            return "Train ID not found.";
        }

    } catch (Exception e) {
        return "Arrival Delay Error: " + e.getMessage();
    }
}
}
