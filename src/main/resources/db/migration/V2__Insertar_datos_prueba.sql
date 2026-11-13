-- ==========================================
-- V2__Insertar_datos_prueba.sql
-- ==========================================

-- 1. Insertar Empresas
INSERT INTO empresas (id, nombre)
VALUES
    (1, 'Seguros Atlas S.A.'),
    (2, 'Banco Central Metropolitano');

-- 2. Insertar Paquetes
INSERT INTO paquetes (id, nombre, cantidad_salas, precio)
VALUES
    (1, 'Paquete Básico 10', 10, 150.00),
    (2, 'Paquete Medio 50', 50, 600.00),
    (3, 'Paquete Premium 200', 200, 2000.00);

-- 3. Insertar Cuentas (Dependen de empresas)
INSERT INTO cuentas (id, empresa_id, nombre_completo, correo, rol)
VALUES
    (1, 1, 'Carlos Mendoza', 'cmendoza@atlas.com', 'ADMINISTRADOR'),
    (2, 1, 'Laura Rojas', 'lrojas@atlas.com', 'REPRESENTANTE'),
    (3, 2, 'Roberto Solís', 'rsolis@banco.com', 'ADMINISTRADOR'),
    (4, 2, 'Ana Vargas', 'avargas@banco.com', 'REPRESENTANTE');

-- 4. Insertar Productos (Dependen de empresas)
INSERT INTO productos (id, empresa_id, nombre, descripcion, categoria)
VALUES
    (1, 1, 'Seguro de Vida Premium', 'Póliza de cobertura amplia para el titular y familia', 'Seguros'),
    (2, 1, 'Seguro de Auto Básico', 'Cobertura contra daños a terceros', 'Seguros'),
    (3, 2, 'Tarjeta de Crédito Oro', 'Tarjeta con beneficios en viajes', 'Financiero'),
    (4, 2, 'Cuenta de Ahorro Joven', 'Cuenta sin comisiones para menores de 25 años', 'Financiero');

-- 5. Insertar Compras (Dependen de empresas, paquetes y cuentas)
INSERT INTO compras (id, empresa_id, paquete_id, cuenta_id, monto, salas_acreditadas, estado_pago, referencia_pago, clave_idempotencia)
VALUES
    (1, 1, 1, 1, 2500.50, 10, 'COMPLETADO', 'STRIPE-txn_109283', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
    (2, 1, 2, 2, 500.00, 2, 'PENDIENTE', NULL, 'b1f8ac99-9c0b-4ef8-bb6d-6bb9bd380a22'),
    (3, 2, 1, 3, 1200.00, 5, 'RECHAZADO', 'PAYPAL-err_8899', 'c2a8bc99-9c0b-4ef8-bb6d-6bb9bd380a33'),
    (4, 2, 3, 4, 9990.99, 50, 'COMPLETADO', 'STRIPE-txn_998822', 'd3b8bc99-9c0b-4ef8-bb6d-6bb9bd380a44');

-- 6. Sincronizar las secuencias de identidad
-- Esto es crucial porque al forzar los IDs manualmente arriba, la secuencia interna
-- de Postgres se queda en 1. Sin esto, el próximo INSERT desde Spring Boot fallaría.
SELECT setval(pg_get_serial_sequence('empresas', 'id'), (SELECT MAX(id) FROM empresas));
SELECT setval(pg_get_serial_sequence('paquetes', 'id'), (SELECT MAX(id) FROM paquetes));
SELECT setval(pg_get_serial_sequence('cuentas', 'id'), (SELECT MAX(id) FROM cuentas));
SELECT setval(pg_get_serial_sequence('productos', 'id'), (SELECT MAX(id) FROM productos));
SELECT setval(pg_get_serial_sequence('compras', 'id'), (SELECT MAX(id) FROM compras));
