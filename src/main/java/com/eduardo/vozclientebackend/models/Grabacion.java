package com.eduardo.vozclientebackend.models;

import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "grabaciones")
@Getter
@Setter
public class Grabacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entrevista_id", nullable = false, unique = true)
    private Long entrevistaId;

    @Column(name = "ruta_canal_cliente", nullable = false, length = 255)
    private String rutaCanalCliente;

    @Column(name = "ruta_canal_representante", length = 255)
    private String rutaCanalRepresentante;

    @Column(nullable = false, length = 20)
    private String formato;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "duracion_segundos", nullable = false)
    private Integer duracionSegundos;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "tiene_audio_cliente", nullable = false)
    private Boolean tieneAudioCliente;

    @Column(name = "es_parcial", nullable = false)
    private Boolean esParcial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoGrabacion estado;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;
}
