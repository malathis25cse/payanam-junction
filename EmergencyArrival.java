/** EmergencyArrival - INHERITS PlatformRequest. Highest priority (rank 1). */
public class EmergencyArrival extends PlatformRequest {

    public EmergencyArrival(int requestId, TrainService train, String status, int platformId) {
        super(requestId, train, status, platformId);   // call the parent constructor
    }

    @Override   // METHOD OVERRIDING
    public int priorityRank() { return 1; }

    @Override
    public String getTypeName() { return "EMERGENCY"; }
}
