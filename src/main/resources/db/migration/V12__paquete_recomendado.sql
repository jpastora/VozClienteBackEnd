-- El administrador de la plataforma elige qué paquete se muestra como «Recomendado» (RF-17).
-- Antes lo decidía la pantalla (siempre el del medio por precio); ahora se guarda en la base.
ALTER TABLE paquetes
    ADD COLUMN es_recomendado BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN paquetes.es_recomendado IS
    'Verdadero en el único paquete que el catálogo muestra como «Recomendado». Lo elige el administrador.';

-- Como máximo un paquete recomendado.
CREATE UNIQUE INDEX uq_paquetes_un_recomendado ON paquetes (es_recomendado) WHERE es_recomendado;

-- Se conserva lo que se veía hasta ahora: el paquete activo del medio por precio (si hay más de uno).
UPDATE paquetes
SET es_recomendado = TRUE
WHERE (SELECT COUNT(*) FROM paquetes WHERE es_activo) > 1
  AND id = (SELECT id
            FROM paquetes
            WHERE es_activo
            ORDER BY precio, id
            OFFSET (SELECT COUNT(*) FROM paquetes WHERE es_activo) / 2
            LIMIT 1);
