/**
 * PlatformRequest - a train asking for a platform.
 * ABSTRACTION: this class is abstract, so you can never create a plain PlatformRequest.
 * You must create an EmergencyArrival or a ScheduledArrival (INHERITANCE).
 * Each subclass OVERRIDES priorityRank() (POLYMORPHISM).
 * COMPOSITION: a PlatformRequest HAS-A TrainService (it owns the train's times).
 */
public abstract class PlatformRequest implements Comparable<PlatformRequest> {
    public static final int CLEARANCE_BUFFER = 5;   // minutes added after departure

    public static final String PENDING   = "PENDING";
    public static final String ALLOCATED = "ALLOCATED";
    public static final String OCCUPYING = "OCCUPYING";
    public static final String DEPARTED  = "DEPARTED";

    private final int requestId;
    private final TrainService train;
    private String status;
    private int platformId;   // 0 = no platform yet

    protected PlatformRequest(int requestId, TrainService train, String status, int platformId) {
        this.requestId = requestId;
        this.train = train;
        this.status = status;
        this.platformId = platformId;
    }

    /** Lower number = higher priority. Every subclass must decide its own rank. */
    public abstract int priorityRank();

    /** "EMERGENCY" or "SCHEDULED" (stored in the database column request_type). */
    public abstract String getTypeName();

    public int getRequestId() { return requestId; }
    public TrainService getTrain() { return train; }
    public int getTrainId() { return train.getTrainId(); }
    public int getTrainLength() { return train.getTrainLength(); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPlatformId() { return platformId; }
    public void setPlatformId(int platformId) { this.platformId = platformId; }

    /** Occupation starts when the train arrives (including arrival delay). */
    public int getStart() { return train.getEffectiveArrival(); }

    /** Occupation ends at departure (including delay) PLUS the clearance buffer. */
    public int getEnd() { return train.getEffectiveDeparture() + CLEARANCE_BUFFER; }

    /** True while the request holds a platform (ALLOCATED or OCCUPYING). */
    public boolean isActive() {
        return ALLOCATED.equals(status) || OCCUPYING.equals(status);
    }

    /** Two time intervals overlap when each one starts before the other ends. */
    public boolean overlaps(PlatformRequest other) {
        return getStart() < other.getEnd() && other.getStart() < getEnd();
    }

    /**
     * Ordering used by the PriorityQueue:
     * 1) priority rank  2) earlier arrival  3) smaller request id (so ties are always the same).
     */
    @Override
    public int compareTo(PlatformRequest other) {
        int byRank = Integer.compare(priorityRank(), other.priorityRank());
        if (byRank != 0) return byRank;
        int byStart = Integer.compare(getStart(), other.getStart());
        if (byStart != 0) return byStart;
        return Integer.compare(requestId, other.requestId);
    }
}
