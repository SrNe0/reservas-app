CREATE TABLE reserva (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id  BIGINT        NOT NULL,
    servicio_id BIGINT        NOT NULL,
    recurso_id  BIGINT        NOT NULL,
    -- Acepta nulos hasta la Etapa 3 (usuarios); entonces se pasará a NOT NULL
    cliente_id  BIGINT,
    inicio      TIMESTAMPTZ   NOT NULL,
    fin         TIMESTAMPTZ   NOT NULL,
    -- Copia del precio del servicio al reservar, para conservar el histórico
    precio      NUMERIC(10,2) NOT NULL,
    estado      TEXT          NOT NULL DEFAULT 'CONFIRMADA',
    creado_en   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_reserva_negocio FOREIGN KEY (negocio_id)
        REFERENCES negocio (id) ON DELETE RESTRICT,
    -- Claves compuestas: el servicio y el recurso deben pertenecer al mismo negocio
    CONSTRAINT fk_reserva_servicio FOREIGN KEY (servicio_id, negocio_id)
        REFERENCES servicio (id, negocio_id) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_recurso FOREIGN KEY (recurso_id, negocio_id)
        REFERENCES recurso (id, negocio_id) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_cliente FOREIGN KEY (cliente_id)
        REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_reserva_fechas CHECK (fin > inicio),
    CONSTRAINT ck_reserva_precio CHECK (precio >= 0),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('CONFIRMADA', 'CANCELADA'))
);

CREATE INDEX idx_reserva_negocio_inicio ON reserva (negocio_id, inicio);
CREATE INDEX idx_reserva_recurso_inicio ON reserva (recurso_id, inicio);
CREATE INDEX idx_reserva_cliente ON reserva (cliente_id);
-- Sin este índice, borrar un servicio obligaría a recorrer toda la tabla de reservas
CREATE INDEX idx_reserva_servicio ON reserva (servicio_id);
