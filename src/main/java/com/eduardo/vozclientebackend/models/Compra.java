package com.eduardo.vozclientebackend.models;

import com.eduardo.vozclientebackend.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "compras")
@Getter
@Setter
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "paquete_id", nullable = false)
    private Long paqueteId;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "salas_acreditadas", nullable = false)
    private Integer salasAcreditadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;    // <-- Cambiado a Enum

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private UUID claveIdempotencia;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;
}
