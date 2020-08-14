import java.io.Serializable;

public class Message implements Serializable {
    private MessageType messageType;
    private StoredContent storedContent;
    private Integer from;
    private VectorClock timestamp;

    public Message(MessageType messageType, StoredContent storedContent) {
        this.messageType = messageType;
        this.storedContent = storedContent;
        this.from = null;
        this.timestamp = null;
    }

    public Message(MessageType messageType, StoredContent storedContent, Integer from) {
        this.messageType = messageType;
        this.storedContent = storedContent;
        this.from = from;
    }

    public Message(MessageType messageType, StoredContent storedContent, Integer from, VectorClock timestamp) {
        this.messageType = messageType;
        this.storedContent = storedContent;
        this.from = from;
        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public StoredContent getStoredContent() {
        return storedContent;
    }

    public Integer getFrom() {
        return from;
    }

    public VectorClock getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageType=" + messageType +
                ", storedContent=" + storedContent +
                ", from=" + from +
                ", timestamp=" + timestamp +
                '}';
    }
}
