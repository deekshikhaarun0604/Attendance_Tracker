package com.smartattend.dto;

import java.time.Instant;

public class ChatMessageResponse {

    private String reply;
    private String source; // GEMINI_AI, SMARTATTEND_ENGINE
    private Instant timestamp;

    public ChatMessageResponse() {
        this.timestamp = Instant.now();
    }

    public ChatMessageResponse(String reply, String source) {
        this.reply = reply;
        this.source = source;
        this.timestamp = Instant.now();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
