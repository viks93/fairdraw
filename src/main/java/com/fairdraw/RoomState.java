package com.fairdraw;

import java.util.ArrayList;
import java.util.List;

/** Serializable room snapshot stored as JSON in the database. */
public class RoomState {

    public String roomCode;
    public List<String> userCodes = new ArrayList<>();
    public List<Integer> deck = new ArrayList<>();
    public List<DrawEventRecord> history = new ArrayList<>();
}
