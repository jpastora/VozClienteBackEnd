package com.eduardo.vozclientebackend.scheduler;

import com.eduardo.vozclientebackend.service.EntrevistaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntrevistaScheduler {

    private final EntrevistaService entrevistaService;

    // 1. Se ejecuta UNA SOLA VEZ apenas arranca el servidor
    @EventListener(ApplicationReadyEvent.class)
    public void limpiarAlIniciarServidor() {
        log.info("Servidor iniciado. Verificando si quedaron entrevistas zombies de sesiones anteriores...");

        // Asumimos 120 minutos (2 horas) de tolerancia
        entrevistaService.cerrarEntrevistasHuerfanas(120);

        log.info("Limpieza inicial completada.");
    }

    // 2. Se ejecuta AUTOMÁTICAMENTE cada 30 minutos mientras el servidor esté encendido
    @Scheduled(cron = "0 0/30 * * * *")
    public void limpiarEntrevistasZombies() {
        log.info("Iniciando tarea programada: Limpieza de entrevistas zombies...");

        entrevistaService.cerrarEntrevistasHuerfanas(120);

        log.info("Tarea de limpieza programada finalizada.");
    }
}
