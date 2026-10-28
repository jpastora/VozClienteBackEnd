package com.eduardo.vozclientebackend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "fragmentos_transcripcion")
@Getter
@Setter
public class FragmentoTranscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transcripcion_id", nullable = false)
    private Long transcripcionId;

    @Column(name = "segundo_inicio", nullable = false, precision = 8, scale = 2)
    private BigDecimal segundoInicio;

    @Column(name = "segundo_fin", nullable = false, precision = 8, scale = 2)
    private BigDecimal segundoFin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;
}
