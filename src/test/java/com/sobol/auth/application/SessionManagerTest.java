package com.sobol.auth.application;

import com.sobol.auth.domain.Account;
import org.junit.jupiter.api.Test;
import static com.sobol.auth.application.AuthTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {
    final Memory rows = new Memory(); final Time time = new Time(); final SessionManager sessions = new SessionManager(rows, time);
    SessionManager.Handle login(long id) {
        rows.add(id, "synthetic" + id + "@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER);
        return AuthTestSupport.service(rows, new Hasher(), sessions, time, false).login("synthetic" + id + "@example.invalid", input());
    }
    @Test void noSessionExistsUntilSuccessfulAuthenticationAndLogoutInvalidatesHandles() {
        assertThrows(AuthenticationException.class, sessions::capture); var handle = login(1);
        assertEquals(1, sessions.require(handle).accountId()); sessions.logout(); assertThrows(AuthenticationException.class, () -> sessions.require(handle));
    }
    @Test void reloginReplacesIdentityAndOldHandleCannotInvalidateTheNewSession() {
        var old = login(1); var current = login(2); assertNotSame(old, current);
        assertThrows(AuthenticationException.class, () -> sessions.require(old)); assertEquals(2, sessions.require(current).accountId());
    }
    @Test void disabledDeletedRoleAndVersionChangesRequireFreshAuthentication() {
        for (int change = 0; change < 4; change++) {
            var handle = login(1); var a = rows.rows.get(1L);
            if (change == 0) rows.rows.remove(1L);
            else rows.rows.put(1L, new Account(1, a.login(), a.displayName(), change == 1 ? Account.Status.DISABLED : a.status(),
                    change == 2 ? Account.Role.ADMIN : a.role(), change == 3 ? 2 : 1));
            assertThrows(AuthenticationException.class, () -> sessions.require(handle)); assertThrows(AuthenticationException.class, sessions::capture);
        }
    }
    @Test void pollingDoesNotExtendIdleExpiryAndActivityDoesNotExtendAbsoluteExpiry() {
        var handle = login(1); time.advance(899); sessions.inspect(handle); time.advance(1);
        assertThrows(AuthenticationException.class, () -> sessions.require(handle));
        var current = login(1);
        for (int i = 0; i < 32; i++) { time.advance(899); sessions.require(current); }
        time.advance(32); assertThrows(AuthenticationException.class, () -> sessions.require(current));
    }
    @Test void storageFailureRevokesSessionAndIdentityDoesNotContainCredentials() {
        var handle = login(1); var identity = sessions.require(handle);
        assertFalse(identity.toString().contains("@"));
        assertEquals(3, identity.getClass().getRecordComponents().length);
        rows.unavailable = true; assertThrows(AuthenticationException.class, () -> sessions.require(handle));
        rows.unavailable = false; assertThrows(AuthenticationException.class, sessions::capture);
    }
}
