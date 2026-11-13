package com.eduardo.vozclientebackend.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "menciones_competencia")
@Getter
@Setter
public class MencionCompetencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analisis_id", nullable = false)
    private Long analisisId;

    @Column(nullable = false, length = 100)
    private String competidor;

    @Column(nullable = false)
    private Short frecuencia;

    @Column(length = 500)
    private String contexto;
}
