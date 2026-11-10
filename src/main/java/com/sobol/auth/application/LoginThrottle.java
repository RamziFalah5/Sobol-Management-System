package com.sobol.auth.application;

import java.time.*;
import java.util.*;

/** Bounded process-local protection. Server-wide/distributed throttling requires a future trusted backend. */
public final class LoginThrottle {
    private final Clock clock;
    private final Deque<Instant> global = new ArrayDeque<>();
    private final LinkedHashMap<String, Deque<Instant>> identifiers = new LinkedHashMap<>(256, 0.75f, true);
    public LoginThrottle(Clock clock) { this.clock = Objects.requireNonNull(clock); }
    public synchronized void acquire(String identifier) {
        Instant now = clock.instant();
        global.removeIf(t -> !t.plusSeconds(60).isAfter(now));
        if (global.size() >= 10) throw new AuthenticationException(AuthenticationException.Reason.THROTTLED);
        Deque<Instant> local = identifiers.computeIfAbsent(identifier, ignored -> new ArrayDeque<>());
        local.removeIf(t -> !t.plusSeconds(300).isAfter(now));
        global.addLast(now); // Rotating identifiers cannot bypass the process-wide budget.
        if (local.size() >= 5) throw new AuthenticationException(AuthenticationException.Reason.THROTTLED);
        local.addLast(now);
        if (identifiers.size() > 256) identifiers.remove(identifiers.keySet().iterator().next());
    }
}
