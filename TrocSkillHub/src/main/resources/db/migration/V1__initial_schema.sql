--V1__initial_schema.sql
-- Baseline de la base de données existante créée par Hibernate
-- Cette migration est documentaire uniquement

-- Table user créée initialement par Hibernate
-- Structure existante :
-- V1__initial_schema.sql
-- Baseline de la base de données créée initialement par Hibernate

CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    picture BYTEA,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(255),
    country VARCHAR(255),
    phone_number VARCHAR(255),
    description TEXT,
    created_at DATE NOT NULL,
    updated_at DATE
);

-- Index pour améliorer les performances
CREATE INDEX idx_user_email ON "user"(email);
