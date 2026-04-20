package com.fairdraw;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** JSON snapshot of a draw for persistence. */
public class DrawEventRecord {

    public String userCode;
    public int number;
    public String timestamp;

    public DrawEventRecord() {}

    @JsonCreator
    public DrawEventRecord(
            @JsonProperty("userCode") String userCode,
            @JsonProperty("number") int number,
            @JsonProperty("timestamp") String timestamp) {
        this.userCode = userCode;
        this.number = number;
        this.timestamp = timestamp;
    }
}
