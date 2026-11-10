-- Phase 6A: independent master-data slices only; no historical rows or deferred entities.
CREATE TABLE sobol.category (
    category_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(150) COLLATE "C" NOT NULL,
    description text NOT NULL DEFAULT '',
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_category_id CHECK (category_id > 0),
    CONSTRAINT ck_category_name CHECK (length(btrim(name)) > 0 AND name = btrim(name)),
    CONSTRAINT uq_category_name UNIQUE (name)
);

CREATE TABLE sobol.supplier (
    supplier_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(150) COLLATE "C" NOT NULL,
    phone varchar(30) NOT NULL,
    email varchar(150) COLLATE "C",
    city varchar(100),
    address varchar(255),
    contact_person varchar(150),
    notes varchar(400),
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_supplier_id CHECK (supplier_id > 0),
    CONSTRAINT ck_supplier_name CHECK (length(btrim(name)) > 0 AND name = btrim(name)),
    CONSTRAINT ck_supplier_phone CHECK (length(btrim(phone)) > 0 AND phone = btrim(phone)),
    CONSTRAINT ck_supplier_email CHECK (email IS NULL OR length(btrim(email)) > 0),
    CONSTRAINT uq_supplier_name UNIQUE (name)
);

CREATE TABLE sobol.customer (
    customer_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name varchar(150) COLLATE "C" NOT NULL,
    phone varchar(30) NOT NULL,
    email varchar(180) COLLATE "C",
    city varchar(100),
    address varchar(220),
    notes varchar(400),
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_customer_id CHECK (customer_id > 0),
    CONSTRAINT ck_customer_name CHECK (length(btrim(full_name)) > 0 AND full_name = btrim(full_name)),
    CONSTRAINT ck_customer_phone CHECK (length(btrim(phone)) > 0 AND phone = btrim(phone)),
    CONSTRAINT ck_customer_email CHECK (email IS NULL OR length(btrim(email)) > 0),
    CONSTRAINT uq_customer_email UNIQUE (email)
);
