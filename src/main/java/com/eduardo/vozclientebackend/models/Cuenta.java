package com.eduardo.vozclientebackend.models;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
public class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 254)
    private String correo;

    @Column(name = "google_sub", length = 255)
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolCuenta rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "ultimo_ingreso_en")
    private OffsetDateTime ultimoIngresoEn;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}
