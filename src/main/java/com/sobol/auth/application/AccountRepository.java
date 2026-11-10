package com.sobol.auth.application;

import com.sobol.auth.domain.Account;
import java.util.*;

public interface AccountRepository {
    /** The opaque credential object must never enter presentation/session state. */
    final class Credential {
        private final Account account;
        private final String encoded;
        public Credential(Account account, String encoded) { this.account = Objects.requireNonNull(account); this.encoded = Objects.requireNonNull(encoded); }
        public Account account() { return account; }
        public String encoded() { return encoded; }
        @Override public String toString() { return "Account credential [redacted]"; }
    }
    Optional<Credential> findCredential(String normalizedLogin);
    Optional<Account> findById(long id);
    /** Registration cannot select status/role. Database defaults must yield PENDING/MEMBER. */
    Account createPending(String login, String displayName, String phone, String encoded);
}
