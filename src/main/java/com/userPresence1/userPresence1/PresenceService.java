package com.userPresence1.userPresence1;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private final Map<String, Set<String>> usersViewingNotes = new ConcurrentHashMap<>();

    /** ✅ Add user to presence list */
    public void addUser(String noteId, String username) {
        usersViewingNotes.computeIfAbsent(noteId, k -> ConcurrentHashMap.newKeySet()).add(username);
    }

    /** ✅ Remove user from presence list */
    public void removeUser(String noteId, String username) {
        usersViewingNotes.computeIfPresent(noteId, (key, users) -> {
            users.remove(username);
            return users.isEmpty() ? null : users;
        });
    }

    /** ✅ Get current users viewing a note */
    public Set<String> getUsers(String noteId) {
        return usersViewingNotes.getOrDefault(noteId, Set.of());
    }
}
