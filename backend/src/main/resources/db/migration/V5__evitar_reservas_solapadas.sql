-- btree_gist permite usar el operador = (para recurso_id) dentro de un índice GiST
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Un recurso no puede tener dos reservas confirmadas que se solapen en el tiempo.
-- El rango [inicio, fin) es cerrado al inicio y abierto al final: 10:00-11:00 y 11:00-12:00 conviven.
-- Las reservas canceladas no cuentan, así que su turno vuelve a quedar libre.
ALTER TABLE reserva
    ADD CONSTRAINT ex_reserva_recurso_solape
        EXCLUDE USING gist (
            recurso_id WITH =,
            tstzrange(inicio, fin, '[)') WITH &&
        ) WHERE (estado = 'CONFIRMADA');
