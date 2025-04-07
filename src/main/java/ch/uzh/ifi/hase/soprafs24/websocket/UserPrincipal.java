package ch.uzh.ifi.hase.soprafs24.websocket;

import java.security.Principal;

/**
 * Custom principal class to assign the userId
 * as name.
 */

public class UserPrincipal implements Principal {
    private final String name;

    public UserPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
