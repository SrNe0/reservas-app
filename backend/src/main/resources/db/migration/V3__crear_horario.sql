-- No se define UNIQUE (negocio_id, dia_semana) para permitir horario partido
-- en el mismo día (por ejemplo, turno de mañana 09:00-13:00 y tarde 15:00-19:00)
CREATE TABLE horario
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id    BIGINT      NOT NULL,
    dia_semana    SMALLINT    NOT NULL,
    hora_apertura TIME        NOT NULL,
    hora_cierre   TIME        NOT NULL,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_horario_negocio FOREIGN KEY (negocio_id)
        REFERENCES negocio (id) ON DELETE RESTRICT,
    CONSTRAINT ck_horario_dia_semana CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_horas CHECK (hora_cierre > hora_apertura)
);

CREATE INDEX idx_horario_negocio ON horario (negocio_id);
