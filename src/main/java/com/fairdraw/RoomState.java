package com.fairdraw;

import java.util.ArrayList;
import java.util.List;

public class RoomState {

    public String roomCode;
    public List<String> userCodes = new ArrayList<>();
    public List<Integer> deck = new ArrayList<>();
    public List<DrawEventRecord> history = new ArrayList<>();
}
