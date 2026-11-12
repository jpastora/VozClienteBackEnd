package com.eduardo.vozclientebackend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guias")
@Getter
@Setter
public class Guia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "guias_preguntas",
        joinColumns = @JoinColumn(name = "guia_id")
    )
    @Column(name = "pregunta", nullable = false, columnDefinition = "TEXT")
    private List<String> preguntas = new ArrayList<>();

    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "actualizado_en")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
