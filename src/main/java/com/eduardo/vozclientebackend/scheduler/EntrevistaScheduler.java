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

    // Se ejecuta al iniciar el servidor
    @EventListener(ApplicationReadyEvent.class)
    public void limpiarAlIniciarServidor() {
        log.info("Verificando si quedaron entrevistas vencidas de más de 7 días...");
        // 7 días = 10,080 minutos (asumiendo que tu método recibe minutos)
        entrevistaService.cerrarEntrevistasHuerfanas(10080);
    }

    // Se ejecuta AUTOMÁTICAMENTE una vez al día (a la medianoche)
    @Scheduled(cron = "0 0 0 * * *")
    public void limpiarEntrevistasZombies() {
        log.info("Iniciando limpieza diaria: buscando entrevistas de más de 7 días...");

        // 10080 minutos = exactamente 7 días
        // Si tu método recibe segundos, debes poner: 604800
        entrevistaService.cerrarEntrevistasHuerfanas(10080);

        log.info("Tarea de limpieza programada finalizada.");
    }
}
