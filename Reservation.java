/** Reservation - one booking request of one passenger on one train. */
public class Reservation {
    public static final String CONFIRMED = "CONFIRMED";
    public static final String WAITING   = "WAITING";
    public static final String CANCELLED = "CANCELLED";

    private int reservationId;
    private final int trainId;
    private final int passengerId;
    private int seatNo;            // 0 means "no seat"
    private String status;
    private final String bookedAt; // text copied from the database
    private final String passengerName;
    private int waitingPosition;   // 1 = first in the waiting queue, 0 = not waiting

    public Reservation(int reservationId, int trainId, int passengerId, int seatNo,
                       String status, String bookedAt, String passengerName) {
        this.reservationId = reservationId;
        this.trainId = trainId;
        this.passengerId = passengerId;
        this.seatNo = seatNo;
        this.status = status;
        this.bookedAt = (bookedAt == null) ? "just now" : bookedAt;
        this.passengerName = passengerName;
    }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    public int getTrainId() { return trainId; }
    public int getPassengerId() { return passengerId; }
    public int getSeatNo() { return seatNo; }
    public void setSeatNo(int seatNo) { this.seatNo = seatNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBookedAt() { return bookedAt; }
    public String getPassengerName() { return passengerName; }
    public int getWaitingPosition() { return waitingPosition; }
    public void setWaitingPosition(int waitingPosition) { this.waitingPosition = waitingPosition; }

    /** A reservation is active while it is CONFIRMED or WAITING. */
    public boolean isActive() {
        return CONFIRMED.equals(status) || WAITING.equals(status);
    }

    /** Text shown in the "Seat" column: seat number, queue position or a dash. */
    public String seatText() {
        if (CONFIRMED.equals(status)) return String.valueOf(seatNo);
        if (WAITING.equals(status)) return "WL-" + waitingPosition;
        return "-";
    }

    @Override
    public String toString() {
        return "Reservation " + reservationId + " [train " + trainId + ", " + passengerName
                + ", seat " + seatText() + ", " + status + "]";
    }
}
