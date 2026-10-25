CREATE TABLE usuario (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    nombre        TEXT        NOT NULL,
    rol           TEXT        NOT NULL,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'NEGOCIO', 'CLIENTE'))
);

-- El email es único sin distinguir mayúsculas
CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(email));

CREATE TABLE negocio (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         TEXT        NOT NULL,
    descripcion    TEXT,
    -- Acepta nulos hasta la Etapa 3 (usuarios); entonces se pasará a NOT NULL
    propietario_id BIGINT,
    zona_horaria   TEXT        NOT NULL,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_negocio_propietario FOREIGN KEY (propietario_id)
        REFERENCES usuario (id) ON DELETE RESTRICT
);

CREATE INDEX idx_negocio_propietario ON negocio (propietario_id);
