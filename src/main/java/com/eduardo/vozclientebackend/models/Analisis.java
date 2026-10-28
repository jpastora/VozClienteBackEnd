package com.eduardo.vozclientebackend.models;


import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.IntencionAnalisis;
import com.eduardo.vozclientebackend.enums.SentimientoAnalisis;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "analisis")
@Getter
@Setter
public class Analisis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transcripcion_id", nullable = false, unique = true)
    private Long transcripcionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAnalisis estado;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IntencionAnalisis intencion;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SentimientoAnalisis sentimiento;

    @Column(name = "fragmento_apoyo", length = 1000)
    private String fragmentoApoyo;

    @Column(name = "tiene_menciones", nullable = false)
    private Boolean tieneMenciones;

    @Column(name = "proveedor_ia", length = 60)
    private String proveedorIa;

    @Column(nullable = false)
    private Integer intentos;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}