package com.eduardo.vozclientebackend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cambios_rol")
@Getter
@Setter
public class CambioRol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "rol_anterior", nullable = false, length = 30)
    private String rolAnterior;

    @Column(name = "rol_nuevo", nullable = false, length = 30)
    private String rolNuevo;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;
}
