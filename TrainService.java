
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * TrainService - one train of the timetable.
 *
 * COMPOSITION:
 * Array       -> Fixed 10 seats
 * ArrayDeque  -> FIFO waiting list
 * ArrayList   -> Reservation history
 */

public class TrainService implements Comparable<TrainService> {

    public static final int TOTAL_SEATS = 10;

    private final int trainId;
    private final String trainName;
    private final String origin;
    private final String destination;
    private final String seatClass;

    private final int arrivalMin;
    private final int departureMin;
    private final int trainLength;

    private int arrivalDelay;
    private int departureDelay;

    private final Reservation[] seats =
            new Reservation[TOTAL_SEATS];

    private final ArrayDeque<Reservation> waitingList =
            new ArrayDeque<>();

    private final ArrayList<Reservation> history =
            new ArrayList<>();

    // Constructor
    public TrainService(int trainId, String trainName,
                        String origin, String destination,
                        int arrivalMin, int departureMin,
                        int trainLength, String seatClass) {

        this.trainId = trainId;
        this.trainName = trainName;
        this.origin = origin;
        this.destination = destination;
        this.arrivalMin = arrivalMin;
        this.departureMin = departureMin;
        this.trainLength = trainLength;
        this.seatClass = seatClass;
    }

    // Validate train information
    public boolean isValidTrain() {

        return trainId > 0
                && trainName != null
                && !trainName.trim().isEmpty()
                && origin != null
                && !origin.trim().isEmpty()
                && destination != null
                && !destination.trim().isEmpty()
                && !origin.trim().equalsIgnoreCase(destination.trim())
                && arrivalMin >= 0
                && arrivalMin < 1440
                && departureMin >= 0
                && departureMin < 1440
                && arrivalMin != departureMin
                && trainLength > 0
                && seatClass != null
                && !seatClass.trim().isEmpty();
    }

    // Getters
    public int getTrainId() {
        return trainId;
    }

    public String getTrainName() {
        return trainName;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public int getArrivalMin() {
        return arrivalMin;
    }

    public int getDepartureMin() {
        return departureMin;
    }

    public int getTrainLength() {
        return trainLength;
    }

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    // Delay setters
    public void setArrivalDelay(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException(
                    "Arrival delay cannot be negative.");
        }
        this.arrivalDelay = minutes;
    }

    public void setDepartureDelay(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException(
                    "Departure delay cannot be negative.");
        }
        this.departureDelay = minutes;
    }

    // Effective times
    public int getEffectiveArrival() {
        return arrivalMin + arrivalDelay;
    }

    public int getEffectiveDeparture() {
        return departureMin + departureDelay;
    }

    // Find first available seat
    private int firstFreeSeat() {

        for (int i = 0; i < TOTAL_SEATS; i++) {

            if (seats[i] == null) {
                return i;
            }
        }

        return -1;
    }

    // Count available seats
    public int availableSeats() {

        int free = 0;

        for (Reservation s : seats) {

            if (s == null) {
                free++;
            }
        }

        return free;
    }

    // Waiting list count
    public int waitingCount() {
        return waitingList.size();
    }

    // Return waiting list
    public List<Reservation> getWaitingList() {
        return new ArrayList<>(waitingList);
    }

    // Return reservation history
    public List<Reservation> getHistory() {
        return new ArrayList<>(history);
    }

    // Check passenger booking
    public boolean hasActiveBooking(int passengerId) {

        for (Reservation s : seats) {

            if (s != null &&
                    s.getPassengerId() == passengerId) {
                return true;
            }
        }

        for (Reservation w : waitingList) {

            if (w.getPassengerId() == passengerId) {
                return true;
            }
        }

        return false;
    }

    // Restore reservations from database
    public void restore(Reservation r) {

        if (r == null) {
            throw new IllegalArgumentException(
                    "Reservation cannot be null.");
        }

        if (Reservation.CONFIRMED.equals(r.getStatus())) {

            int seatNo = r.getSeatNo();

            if (seatNo < 1 || seatNo > TOTAL_SEATS) {
                throw new IllegalArgumentException(
                        "Invalid seat number.");
            }

            if (seats[seatNo - 1] != null) {
                throw new IllegalStateException(
                        "Seat is already occupied.");
            }

            seats[seatNo - 1] = r;

        } else if (Reservation.WAITING.equals(r.getStatus())) {

            waitingList.addLast(r);
            r.setWaitingPosition(waitingList.size());

        }

        history.add(r);
    }

    // Book seat
    public Reservation bookSeat(Reservation r) {

        if (r == null) {
            throw new IllegalArgumentException(
                    "Reservation cannot be null.");
        }

        if (r.getPassengerId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid passenger ID.");
        }

        if (hasActiveBooking(r.getPassengerId())) {
            throw new IllegalStateException(
                    "Passenger already has an active booking.");
        }

        int free = firstFreeSeat();

        if (free >= 0) {

            seats[free] = r;

            r.setSeatNo(free + 1);
            r.setStatus(Reservation.CONFIRMED);

            r.setWaitingPosition(0);

        } else {

            waitingList.addLast(r);

            r.setSeatNo(0);
            r.setStatus(Reservation.WAITING);

            r.setWaitingPosition(waitingList.size());
        }

        history.add(r);

        return r;
    }

    // Cancel booking
    public Reservation cancelBooking(Reservation r) {

        if (r == null) {
            throw new IllegalArgumentException(
                    "Reservation cannot be null.");
        }

        if (Reservation.CONFIRMED.equals(r.getStatus())) {

            int seatNo = r.getSeatNo();

            if (seatNo < 1 || seatNo > TOTAL_SEATS
                    || seats[seatNo - 1] != r) {
                throw new IllegalStateException(
                        "Reservation seat information is invalid.");
            }

            seats[seatNo - 1] = null;

            r.setSeatNo(0);
            r.setStatus(Reservation.CANCELLED);

            r.setWaitingPosition(0);

            List<Reservation> promoted = promoteWaiting();

            return promoted.isEmpty()
                    ? null : promoted.get(0);
        }

        if (Reservation.WAITING.equals(r.getStatus())) {

            boolean removed = waitingList.removeIf(
                    w -> w.getReservationId()
                            == r.getReservationId());

            if (!removed) {
                throw new IllegalStateException(
                        "Waiting reservation not found.");
            }

            r.setStatus(Reservation.CANCELLED);
            r.setWaitingPosition(0);
            r.setSeatNo(0);

            renumberWaitingList();

            return null;
        }

        throw new IllegalStateException(
                "Reservation is already cancelled.");
    }

    // Promote waiting passengers
    public List<Reservation> promoteWaiting() {

        List<Reservation> promoted = new ArrayList<>();

        int free = firstFreeSeat();

        while (!waitingList.isEmpty() && free >= 0) {

            Reservation next = waitingList.pollFirst();

            seats[free] = next;

            next.setSeatNo(free + 1);
            next.setStatus(Reservation.CONFIRMED);
            next.setWaitingPosition(0);

            promoted.add(next);

            free = firstFreeSeat();
        }

        renumberWaitingList();

        return promoted;
    }

    // Update waiting positions
    private void renumberWaitingList() {

        int position = 1;

        for (Reservation w : waitingList) {

            w.setWaitingPosition(position++);
        }
    }

    // Sort trains by arrival time
    @Override
    public int compareTo(TrainService other) {

        int byTime = Integer.compare(
                getEffectiveArrival(),
                other.getEffectiveArrival());

        return byTime != 0
                ? byTime
                : Integer.compare(trainId, other.trainId);
    }

    // Display train details
    @Override
    public String toString() {

        return trainId + " - " + trainName;
    }
}