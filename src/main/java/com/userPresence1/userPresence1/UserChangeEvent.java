package com.userPresence1.userPresence1;

public class UserChangeEvent {
    private final Object source;

    public UserChangeEvent(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }
}
