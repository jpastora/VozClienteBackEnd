package com.eduardo.vozclientebackend.models;

import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "entrevistas")
@Getter
@Setter
public class Entrevista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "representante_id", nullable = false)
    private Long representanteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEntrevista estado;

    @Column(name = "inicio_en", nullable = false)
    private OffsetDateTime inicioEn;

    @Column(name = "fin_en")
    private OffsetDateTime finEn;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;
}
