import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Platform - a platform with a fixed length and a sorted list of booked time slots.
 * occupyPlatform() is the ONLY way to add a booking, so two overlapping
 * trains can never be placed on the same platform (ENCAPSULATION).
 */
public class Platform {
    private final int platformId;
    private final String name;
    private final int lengthMeters;
    private final ArrayList<PlatformRequest> bookings = new ArrayList<>();   // sorted by start time

    public Platform(int platformId, String name, int lengthMeters) {
        this.platformId = platformId;
        this.name = name;
        this.lengthMeters = lengthMeters;
    }

    public int getPlatformId() { return platformId; }
    public String getName() { return name; }
    public int getLengthMeters() { return lengthMeters; }
    public List<PlatformRequest> getBookings() { return new ArrayList<>(bookings); }

    /** Adds a booking read from the database (no checks, the database already holds valid data). */
    public void addExistingBooking(PlatformRequest r) {
        bookings.add(r);
        bookings.sort(Comparator.comparingInt(PlatformRequest::getStart));
    }

    /** Checks length and time conflicts; books the platform only when both are fine. */
    public boolean occupyPlatform(PlatformRequest r) {
        if (r.getTrainLength() > lengthMeters) return false;          // train too long
        for (PlatformRequest b : bookings) {
            if (b.getRequestId() != r.getRequestId() && b.overlaps(r)) return false;   // time conflict
        }
        addExistingBooking(r);
        return true;
    }

    public void removeBooking(int requestId) {
        bookings.removeIf(b -> b.getRequestId() == requestId);
    }

    /** The train standing on the platform right now (status OCCUPYING), or null. */
    public PlatformRequest getOccupyingRequest() {
        for (PlatformRequest b : bookings) {
            if (PlatformRequest.OCCUPYING.equals(b.getStatus())) return b;
        }
        return null;
    }

    /** Earliest time (minutes) when this platform becomes free; Integer.MAX_VALUE if it has no booking. */
    public int getNextRelease() {
        int best = Integer.MAX_VALUE;
        for (PlatformRequest b : bookings) best = Math.min(best, b.getEnd());
        return best;
    }

    public String getStatusText() {
        PlatformRequest occ = getOccupyingRequest();
        if (occ == null) return "FREE";
        return "OCCUPIED by Train " + occ.getTrainId() + " " + occ.getTrain().getTrainName();
    }

    @Override
    public String toString() {
        return name + " (" + lengthMeters + " m)";
    }
}
