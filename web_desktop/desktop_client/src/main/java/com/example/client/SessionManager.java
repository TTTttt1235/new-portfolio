package com.example.client;

import jakarta.websocket.Session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SessionManager {
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    public static void add(Session session) {
        sessions.add(session);
    }

    public static void remove(Session session) {
        sessions.remove(session);
    }

    public static Set<Session> getSessions() {
        return sessions;
    }
}
