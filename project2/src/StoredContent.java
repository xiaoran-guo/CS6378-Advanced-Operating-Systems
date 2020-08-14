import java.io.Serializable;

public class StoredContent implements Serializable {
    private Integer objectId;
    private Integer objectValue;
    private VectorClock vectorClock;

    public StoredContent(int objectId) {
        this.objectId = objectId;
        this.objectValue = null;
        this.vectorClock = null;
    }

    public StoredContent(Integer objectId, Integer objectValue) {
        this.objectId = objectId;
        this.objectValue = objectValue;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public Integer getObjectValue() {
        return objectValue;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(int[] vectorClock) {
        this.vectorClock = new VectorClock(vectorClock.length);
        this.vectorClock.setTimestamp(vectorClock);
    }

    @Override
    public String toString() {
        return "StoredContent{" +
                "objectId=" + objectId +
                ", objectValue=" + objectValue +
                ", vectorClock=" + vectorClock +
                '}';
    }
}
