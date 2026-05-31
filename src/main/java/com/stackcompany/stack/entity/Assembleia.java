package com.stackcompany.stack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assembleias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assembleia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String empresa;

    @Column(name = "tipo_empresa", nullable = false, length = 100)
    private String tipoEmpresa;

    @Column(name = "data_assembleia", nullable = false, length = 20)
    private String dataAssembleia;

    @Column(nullable = false, length = 10)
    private String hora;

    @Column(nullable = false, length = 300)
    private String local;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
