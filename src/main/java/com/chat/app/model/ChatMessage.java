package com.chat.app.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single chat message or system event.
 *
 * Types:
 *   CHAT   - a regular user message
 *   JOIN   - user joined a room
 *   LEAVE  - user left a room
 *   TYPING - typing indicator (not persisted)
 *   ERROR  - server error notification
 */
public class ChatMessage {

    public enum Type { CHAT, JOIN, LEAVE, TYPING, ERROR }

    private String  id;
    private String  content;
    private String  sender;
    private Type    type;
    private String  room;
    private Instant timestamp;

    public ChatMessage() {
        this.id        = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public ChatMessage(String content, String sender, Type type, String room) {
        this();
        this.content = content;
        this.sender  = sender;
        this.type    = type;
        this.room    = room;
    }

    public String getId()                         { return id; }
    public void   setId(String id)                { this.id = id; }
    public String getContent()                    { return content; }
    public void   setContent(String content)      { this.content = content; }
    public String getSender()                     { return sender; }
    public void   setSender(String sender)        { this.sender = sender; }
    public Type   getType()                       { return type; }
    public void   setType(Type type)              { this.type = type; }
    public String getRoom()                       { return room; }
    public void   setRoom(String room)            { this.room = room; }
    public Instant getTimestamp()                 { return timestamp; }
    public void    setTimestamp(Instant ts)       { this.timestamp = ts; }
}
