package com.integrador.lab.service;

import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.modelo.ReservaIntegrante;
import com.integrador.lab.repository.ReservaIntegranteRepository;
import com.integrador.lab.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TareasProgramadasService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ReservaIntegranteRepository reservaIntegranteRepositorio;

    @Autowired
    private EmailService emailService;

    // 🔑 Zona horaria de Perú (UTC-5)
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    // Tiempo de tolerancia en minutos después de la hora de inicio
    private static final int MINUTOS_TOLERANCIA = 15;

    // =========================================================================
    // TAREA 1: CANCELAR RESERVAS NO ASISTIDAS (Las que se quedaron en CONFIRMADA)
    // =========================================================================
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void cancelarReservasNoAsistidas() {
        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);

        List<Reserva> reservasConfirmadas = reservaRepository.findByEstado("CONFIRMADA");

        int canceladas = 0;
        for (Reserva r : reservasConfirmadas) {
            if (r.getHoraInicio() == null) continue;

            boolean noAsistio = false;

            // Si es de hoy, y pasaron más de 15 minutos
            if (r.getFechaReserva().equals(hoy)) {
                LocalTime limiteTolerancia = r.getHoraInicio().plusMinutes(15);
                if (ahora.isAfter(limiteTolerancia)) {
                    noAsistio = true;
                }
            }
            // Si es de días anteriores
            else if (r.getFechaReserva().isBefore(hoy)) {
                noAsistio = true;
            }

            if (noAsistio) {
                r.setEstado("NO_ASISTIO");
                reservaRepository.save(r);
                canceladas++;
                System.out.println("❌ " + r.getIdReserva() + " cancelada por NO ASISTIR.");
            }
        }
    }

    // =========================================================================
    // TAREA 2: EXPIRAR RESERVAS EN USO (Si pasó la hora y no hicieron checkout)
    // =========================================================================
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirarReservasAntiguas() {
        LocalDate hoy = LocalDate.now();
        LocalTime horaActual = LocalTime.now();
        
        List<Reserva> candidatas = reservaRepository.findByEstadoAndFechaReservaLessThanEqual("EN_USO", hoy);

        int contador = 0;
        for (Reserva reserva : candidatas) {
            boolean esDeDiasPasados = reserva.getFechaReserva().isBefore(hoy);
            boolean esDeHoyPeroYaTermino = reserva.getFechaReserva().isEqual(hoy) && reserva.getHoraFin().isBefore(horaActual);

            if (esDeDiasPasados || esDeHoyPeroYaTermino) {
                reserva.setEstado("DEVUELTA");
                reservaRepository.save(reserva);
                contador++;
            }
        }

        if (contador > 0) {
            System.out.println("⏰ [SCHEDULER] Se cerraron " + contador + " reservas.");
        }
    }

    // =========================================================================
    // TAREA 3: VENCER INSPECCIONES (Si el admin no revisó en 30 min)
    // =========================================================================
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void vencerInspecciones() {
        LocalDate hoy = LocalDate.now();

        List<Reserva> reservasPendientes = reservaRepository.findByEstadoAndFechaReservaLessThanEqual("DEVUELTA", hoy);
        
        for (Reserva r : reservasPendientes) {
            LocalDateTime horaFin = LocalDateTime.of(r.getFechaReserva(), r.getHoraFin());
            LocalDateTime limite = horaFin.plusMinutes(30);

            if (LocalDateTime.now().isAfter(limite)) {
                r.setEstado("NO_INSPECCIONADO");
                reservaRepository.save(r);
                System.out.println("⚠️ [SCHEDULER] Reserva " + r.getIdReserva() + " pasó a NO_INSPECCIONADO.");
            }
        }
    }

    // =========================================================================
    // TAREA 4: ENVIAR RECORDATORIOS 15 MINUTOS ANTES (ENTRADA Y SALIDA)
    // =========================================================================
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    @Transactional(readOnly = true)
    public void enviarRecordatoriosReservas() {
        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);
        LocalDateTime ahoraCompleto = LocalDateTime.of(hoy, ahora);

        // Buscamos todas las reservas de hoy que estén activas o confirmadas
        List<Reserva> reservasHoy = reservaRepository.findByFechaReserva(hoy);

        for (Reserva r : reservasHoy) {
            // Solo enviamos correos si la reserva está CONFIRMADA o EN_USO
            if (!"CONFIRMADA".equalsIgnoreCase(r.getEstado()) && !"EN_USO".equalsIgnoreCase(r.getEstado())) {
                continue;
            }

            if (r.getHoraInicio() == null || r.getHoraFin() == null) continue;

            LocalDateTime inicioReserva = LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio());
            LocalDateTime finReserva = LocalDateTime.of(r.getFechaReserva(), r.getHoraFin());

            long minutosParaInicio = ChronoUnit.MINUTES.between(ahoraCompleto, inicioReserva);
            long minutosParaFin = ChronoUnit.MINUTES.between(ahoraCompleto, finReserva);

            // Recordatorio de ENTRADA: Faltan entre 14 y 15 minutos para el inicio
            if (minutosParaInicio >= 14 && minutosParaInicio <= 15) {
                enviarCorreoRecordatorioEntrada(r);
            }

            // Recordatorio de SALIDA: Faltan entre 14 y 15 minutos para el fin
            if (minutosParaFin >= 14 && minutosParaFin <= 15) {
                enviarCorreoRecordatorioSalida(r);
            }
        }
    }

    private void enviarCorreoRecordatorioEntrada(Reserva r) {
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                String correo = ri.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe";
                String asunto = "⏰ Recordatorio: Tu reserva empieza en 15 minutos - UTP +Lab";
                String mensaje = "Hola " + ri.getAlumno().getNombre() + ",\n\n" +
                        "Te recordamos que tu reserva en " + r.getLaboratorio().getNombre() + 
                        " (" + r.getLaboratorio().getUbicacionCubiculo() + ") está por comenzar.\n\n" +
                        "Fecha: " + r.getFechaReserva() + "\n" +
                        "Horario: " + r.getHoraInicio() + " - " + r.getHoraFin() + "\n" +
                        "Código: " + r.getIdReserva() + "\n\n" +
                        "¡No llegues tarde!\n\n" +
                        "Atentamente,\nEl equipo de UTP +Lab";
                try {
                    emailService.enviarCorreo(correo, asunto, mensaje);
                    System.out.println("✅ Correo de ENTRADA enviado a: " + correo + " para la reserva " + r.getIdReserva());
                } catch (Exception e) {
                    System.out.println("❌ Error enviando correo de entrada: " + e.getMessage());
                }
            }
        }
    }

    private void enviarCorreoRecordatorioSalida(Reserva r) {
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                String correo = ri.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe";
                String asunto = "⏰ Recordatorio: Tu reserva termina en 15 minutos - UTP +Lab";
                String mensaje = "Hola " + ri.getAlumno().getNombre() + ",\n\n" +
                        "Te recordamos que tu reserva en " + r.getLaboratorio().getNombre() + 
                        " (" + r.getLaboratorio().getUbicacionCubiculo() + ") está por finalizar.\n\n" +
                        "Fecha: " + r.getFechaReserva() + "\n" +
                        "Horario: " + r.getHoraInicio() + " - " + r.getHoraFin() + "\n" +
                        "Código: " + r.getIdReserva() + "\n\n" +
                        "Por favor, prepara tus materiales para la devolución.\n\n" +
                        "Atentamente,\nEl equipo de UTP +Lab";
                try {
                    emailService.enviarCorreo(correo, asunto, mensaje);
                    System.out.println("✅ Correo de SALIDA enviado a: " + correo + " para la reserva " + r.getIdReserva());
                } catch (Exception e) {
                    System.out.println("❌ Error enviando correo de salida: " + e.getMessage());
                }
            }
        }
    }
}