-- Keycloak stays the source of truth for credentials: nothing secret is stored here.
-- users.id is the "sub" claim of the Keycloak token, forwarded by the gateway.
CREATE TABLE users (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE profiles (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    pseudo     VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
