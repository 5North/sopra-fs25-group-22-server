package ch.uzh.ifi.hase.soprafs24.websocket;

import static org.junit.jupiter.api.Assertions.*;

import java.security.Principal;

import org.junit.jupiter.api.Test;

class UserPrincipalTest {

    @Test
    void getName_returnsConstructorValue() {
        UserPrincipal principal = new UserPrincipal("alice");
        assertEquals("alice", principal.getName(), "getName() should return the name passed into the constructor");
    }

    @Test
    void implementsPrincipalInterface() {
        UserPrincipal principal = new UserPrincipal("bob");
        assertTrue(principal instanceof Principal, "UserPrincipal should implement java.security.Principal");
    }

    @Test
    void differentNames_areDistinctInstances() {
        UserPrincipal p1 = new UserPrincipal("x");
        UserPrincipal p2 = new UserPrincipal("y");
        assertNotEquals(p1.getName(), p2.getName(),
                "Two principals with different names should return different names");
    }
}
