public enum MessageType {
    /**
     *  Client specifies the content and its value to update/insert.
     */
    clientPut,
    /**
     *  Server acknowledges the success of write operation.
     */
    clientPutAck,
    /**
     *  Server informs the client about the failure of write operation.
     */
    clientPutFail,

    /**
     *  Client specifies the object value to be retrieved.
     */
    clientGet,
    /**
     *  Server sends the acknowledgment together with the wanted object value.
     */
    clientGetAck,

    /**
     *  Server informs other servers to commit changes.
     */
    serverPut,
    /**
     *  Server acknowledges the success of commit operation to the source server.
     */
    serverPutAck,
    /**
     *  Server requests other servers to write an object.
     */
    serverPutReq,
    /**
     *  Server acknowledge that it is ready to write the specific object.
     */
    serverPutReqAck,
    /**
     *  Server informs the source server about the failure of write operation.
     */
    serverPutFail,
    /**
     *  Server informs the target server to close the socket connection.
     */
    serverPutEnd,

    /**
     *  Server informs the client that it is unavailable.
     */
    serverUnavailable
}
