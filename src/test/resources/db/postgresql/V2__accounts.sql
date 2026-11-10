-- New accounts only. No seeds, legacy credentials, per-user databases or automatic Administrator.
CREATE TABLE sobol.account (
    account_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login varchar(254) COLLATE "C" NOT NULL,
    display_name varchar(160) NOT NULL,
    phone varchar(60),
    password_hash varchar(256) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    role varchar(16) NOT NULL DEFAULT 'MEMBER',
    auth_version bigint NOT NULL DEFAULT 1,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_account_id CHECK (account_id > 0),
    CONSTRAINT ck_account_login CHECK (login = lower(login) AND login = btrim(login) AND length(login) BETWEEN 3 AND 254),
    CONSTRAINT ck_account_name CHECK (length(btrim(display_name)) > 0 AND display_name = btrim(display_name)),
    CONSTRAINT ck_account_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    CONSTRAINT ck_account_role CHECK (role IN ('MEMBER', 'ADMIN')),
    CONSTRAINT ck_account_version CHECK (auth_version > 0),
    CONSTRAINT ck_account_hash CHECK (password_hash ~ '^pbkdf2-sha256\$v1\$600000\$[A-Za-z0-9+/]{22}==\$[A-Za-z0-9+/]{43}=$'),
    CONSTRAINT uq_account_login UNIQUE (login)
);
