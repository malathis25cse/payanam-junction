/** ScheduledArrival - INHERITS PlatformRequest. Normal priority (rank 2). */
public class ScheduledArrival extends PlatformRequest {

    public ScheduledArrival(int requestId, TrainService train, String status, int platformId) {
        super(requestId, train, status, platformId);
    }

    @Override   // METHOD OVERRIDING
    public int priorityRank() { return 2; }

    @Override
    public String getTypeName() { return "SCHEDULED"; }
}
