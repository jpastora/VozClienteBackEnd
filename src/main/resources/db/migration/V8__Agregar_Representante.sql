ALTER TABLE productos
    ADD COLUMN representante_id BIGINT NULL,
    ADD CONSTRAINT fk_productos_representante
        FOREIGN KEY (representante_id) REFERENCES cuentas (id);

CREATE INDEX idx_productos_representante ON productos (representante_id);
