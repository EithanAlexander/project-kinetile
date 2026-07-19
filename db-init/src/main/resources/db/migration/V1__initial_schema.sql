CREATE TABLE place_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    label VARCHAR(128) NOT NULL,
    traffic_tier VARCHAR(16) NOT NULL
);

CREATE TABLE tile_manufacturers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE cities (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL
);

CREATE TABLE chokepoints (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL REFERENCES cities (id),
    place_type_id BIGINT NOT NULL REFERENCES place_types (id),
    code VARCHAR(32) NOT NULL,
    name VARCHAR(256) NOT NULL,
    CONSTRAINT uq_chokepoints_city_code UNIQUE (city_id, code)
);

CREATE INDEX idx_chokepoints_city_id ON chokepoints (city_id);
CREATE INDEX idx_chokepoints_place_type_id ON chokepoints (place_type_id);

CREATE TABLE tiles (
    id BIGSERIAL PRIMARY KEY,
    tile_id UUID NOT NULL UNIQUE,
    chokepoint_id BIGINT NOT NULL REFERENCES chokepoints (id),
    manufacturer_id BIGINT NOT NULL REFERENCES tile_manufacturers (id),
    size VARCHAR(32) NOT NULL,
    color VARCHAR(64) NOT NULL,
    installation_date DATE NOT NULL,
    removal_date DATE,
    last_inspection_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_tiles_chokepoint_id ON tiles (chokepoint_id);
CREATE INDEX idx_tiles_manufacturer_id ON tiles (manufacturer_id);
CREATE INDEX idx_tiles_is_active ON tiles (is_active);

CREATE TABLE tile_compression_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL,
    CONSTRAINT uq_tce_event_id UNIQUE (event_id),
    event_type VARCHAR(32) NOT NULL,
    tile_id UUID NOT NULL REFERENCES tiles (tile_id),
    mass_kg DOUBLE PRECISION NOT NULL,
    impact_multiplier DOUBLE PRECISION NOT NULL,
    calculated_force_newtons DOUBLE PRECISION NOT NULL,
    calculated_energy_joules DOUBLE PRECISION NOT NULL,
    activation_successful BOOLEAN NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_tce_tile_id_event_timestamp ON tile_compression_events (tile_id, event_timestamp);
CREATE INDEX idx_tce_event_timestamp ON tile_compression_events (event_timestamp);
CREATE INDEX idx_tce_activation ON tile_compression_events (activation_successful);

CREATE TABLE tile_compression_activity (
    tile_id UUID PRIMARY KEY REFERENCES tiles (tile_id),
    first_compression_at TIMESTAMPTZ NOT NULL,
    last_compression_at TIMESTAMPTZ NOT NULL,
    total_compressions BIGINT NOT NULL DEFAULT 0
);
