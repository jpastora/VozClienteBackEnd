package com.eduardo.vozclientebackend.models;

import com.eduardo.vozclientebackend.enums.EstadoSala;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "salas")
@Getter
@Setter
public class Sala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "creado_por_id", nullable = false)
    private Long creadoPorId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSala estado;

    @Column(name = "emitida_en", nullable = false)
    private OffsetDateTime emitidaEn;

    @Column(name = "vence_en", nullable = false)
    private OffsetDateTime venceEn;

    @Column(name = "nombre_consumidor", length = 60)
    private String nombreConsumidor;

    @Column(name = "acepto_grabacion_en")
    private OffsetDateTime aceptoGrabacionEn;
}