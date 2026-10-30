package com.eduardo.vozclientebackend.models;

import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;



@Entity
@Table(name = "transcripciones")
@Getter
@Setter
public class Transcripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grabacion_id", nullable = false, unique = true)
    private Long grabacionId;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(name = "idioma_detectado", length = 10)
    private String idiomaDetectado;

    @Column(name = "probabilidad_idioma", precision = 4, scale = 3)
    private BigDecimal probabilidadIdioma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTranscripcion estado;

    @Column(nullable = false)
    private Integer intentos;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
