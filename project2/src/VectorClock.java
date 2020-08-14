import java.io.Serializable;
import java.util.Arrays;

public class VectorClock implements Comparable<VectorClock>, Serializable {
    private int[] timestamp = null;

    public VectorClock(int n) {
        timestamp = new int[n];
        Arrays.fill(timestamp, 0);
    }

    public int[] getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int[] timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VectorClock{" +
                "timestamp=" + Arrays.toString(timestamp) +
                '}';
    }

    @Override
    public int compareTo(VectorClock v) {
        int res = 0;
        for (int i = 0; i < this.timestamp.length; i++) {
            if (this.timestamp[i] < v.timestamp[i]) {
                if (res == 0) {
                    res = -1;
                } else if (res == 1) {
                    return 0;
                }
            } else if (this.timestamp[i] > v.timestamp[i]) {
                if (res == 0) {
                    res = 1;
                } else if (res == -1) {
                    return 0;
                }
            }
        }
        return res;
    }

    public void ticktock(int nodeId) {
        timestamp[nodeId]++;
    }

    public void ticktock(int nodeId, VectorClock vectorClock, int fromId) {
        timestamp[nodeId]++;

        if (vectorClock == null) {
            return;
        }

        for (int i = 0; i < vectorClock.timestamp.length; i++) {
            if (i != fromId) {
                if (this.timestamp[i] < vectorClock.timestamp[i]) {
                    this.timestamp[i] = vectorClock.timestamp[i];
                }
            }
        }
    }

    public void adjustTimestamp(int nodeId, VectorClock vectorClock) {
        for (int i = 0; i < vectorClock.timestamp.length; i++) {
            if (i != nodeId) {
                if (this.timestamp[i] < vectorClock.timestamp[i]) {
                    this.timestamp[i] = vectorClock.timestamp[i];
                }
            }
        }
    }
}
