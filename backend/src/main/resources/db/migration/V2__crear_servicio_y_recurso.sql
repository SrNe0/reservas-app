CREATE TABLE servicio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id   BIGINT        NOT NULL,
    nombre       TEXT          NOT NULL,
    duracion_min INTEGER       NOT NULL,
    precio       NUMERIC(10,2) NOT NULL,
    activo       BOOLEAN       NOT NULL DEFAULT true,
    creado_en    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_servicio_negocio FOREIGN KEY (negocio_id)
        REFERENCES negocio (id) ON DELETE RESTRICT,
    CONSTRAINT ck_servicio_duracion CHECK (duracion_min > 0),
    CONSTRAINT ck_servicio_precio CHECK (precio >= 0),
    -- Redundante con la clave primaria, pero necesaria para que reserva pueda
    -- apuntar con una clave foránea compuesta (servicio_id, negocio_id)
    CONSTRAINT uq_servicio_id_negocio UNIQUE (id, negocio_id)
);

CREATE INDEX idx_servicio_negocio ON servicio (negocio_id);

-- Lo que se reserva dentro de un negocio: una cancha, un sillón, un consultorio
CREATE TABLE recurso (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id BIGINT      NOT NULL,
    nombre     TEXT        NOT NULL,
    activo     BOOLEAN     NOT NULL DEFAULT true,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_recurso_negocio FOREIGN KEY (negocio_id)
        REFERENCES negocio (id) ON DELETE RESTRICT,
    CONSTRAINT uq_recurso_id_negocio UNIQUE (id, negocio_id)
);

CREATE INDEX idx_recurso_negocio ON recurso (negocio_id);
