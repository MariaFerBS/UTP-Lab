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

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    // TAREAS 1, 2 y 3 (Se mantienen igual)
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void cancelarReservasNoAsistidas() {
        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);
        List<Reserva> reservasConfirmadas = reservaRepository.findByEstado("CONFIRMADA");

        for (Reserva r : reservasConfirmadas) {
            if (r.getHoraInicio() == null) continue;
            boolean noAsistio = false;

            if (r.getFechaReserva().equals(hoy)) {
                if (ahora.isAfter(r.getHoraInicio().plusMinutes(15))) noAsistio = true;
            } else if (r.getFechaReserva().isBefore(hoy)) {
                noAsistio = true;
            }

            if (noAsistio) {
                r.setEstado("NO_ASISTIO");
                reservaRepository.save(r);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirarReservasAntiguas() {
        LocalDate hoy = LocalDate.now();
        LocalTime horaActual = LocalTime.now();
        List<Reserva> candidatas = reservaRepository.findByEstadoAndFechaReservaLessThanEqual("EN_USO", hoy);

        for (Reserva reserva : candidatas) {
            boolean esDeDiasPasados = reserva.getFechaReserva().isBefore(hoy);
            boolean esDeHoyPeroYaTermino = reserva.getFechaReserva().isEqual(hoy) && reserva.getHoraFin().isBefore(horaActual);
            if (esDeDiasPasados || esDeHoyPeroYaTermino) {
                reserva.setEstado("DEVUELTA");
                reservaRepository.save(reserva);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void vencerInspecciones() {
        LocalDate hoy = LocalDate.now();
        List<Reserva> reservasPendientes = reservaRepository.findByEstadoAndFechaReservaLessThanEqual("DEVUELTA", hoy);
        for (Reserva r : reservasPendientes) {
            LocalDateTime horaFin = LocalDateTime.of(r.getFechaReserva(), r.getHoraFin());
            if (LocalDateTime.now().isAfter(horaFin.plusMinutes(30))) {
                r.setEstado("NO_INSPECCIONADO");
                reservaRepository.save(r);
            }
        }
    }

    // =========================================================================
    // TAREA 4: ENVIAR RECORDATORIOS A ALUMNOS Y ENCARGADOS (CON CORREOS REALES)
    // =========================================================================
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    @Transactional(readOnly = true)
    public void enviarRecordatoriosReservas() {
        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);
        LocalDateTime ahoraCompleto = LocalDateTime.of(hoy, ahora);

        List<Reserva> reservasHoy = reservaRepository.findByFechaReserva(hoy);

        for (Reserva r : reservasHoy) {
            if (!"CONFIRMADA".equalsIgnoreCase(r.getEstado()) && !"EN_USO".equalsIgnoreCase(r.getEstado())) continue;
            if (r.getHoraInicio() == null || r.getHoraFin() == null) continue;

            LocalDateTime inicioReserva = LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio());
            LocalDateTime finReserva = LocalDateTime.of(r.getFechaReserva(), r.getHoraFin());

            long minutosParaInicio = ChronoUnit.MINUTES.between(ahoraCompleto, inicioReserva);
            long minutosParaFin = ChronoUnit.MINUTES.between(ahoraCompleto, finReserva);

            // 👇 15 MINUTOS ANTES DE ENTRAR
            if (minutosParaInicio == 15) {
                enviarCorreoEntrada(r, r.getLaboratorio().getEncargado());
            }

            // 👇 10 MINUTOS ANTES DE SALIR
            if (minutosParaFin == 10) {
                enviarCorreoSalida(r, r.getLaboratorio().getEncargado());
            }
        }
    }

    // 👇 MÉTODO PARA OBTENER EL CORREO DEL ENCARGADO (Mapeo manual para pruebas) 👇
    private String obtenerCorreoEncargado(com.integrador.lab.modelo.Encargado encargado) {
        if (encargado == null) return null;
        String id = encargado.getIdEncargado();
        
        // Si es Carlos Mendoza (E45985625), le llega a tu correo U24225302
        if ("E45985625".equals(id)) {
            return "U24225302@utp.edu.pe"; 
        }
        // Si es Lucía Fernandez (E45985626), le llega a tu compañera U24204158
        if ("E45985626".equals(id)) {
            return "U24204158@utp.edu.pe";
        }
        
        // Por defecto (si no es ninguno de los de prueba), usamos el suyo
        return id.toLowerCase() + "@utp.edu.pe";
    }

    private void enviarCorreoEntrada(Reserva r, com.integrador.lab.modelo.Encargado encargado) {
        // 1. Enviar a los Alumnos
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                String correo = ri.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe";
                String asunto = "ES UNA PRUEBA ⏰ Recordatorio: Tu reserva empieza en 15 minutos - UTP +Lab";
                String mensaje = "Hola " + ri.getAlumno().getNombre() + ",\n\n" +
                        "Tu reserva en " + r.getLaboratorio().getNombre() + " está por comenzar.\n" +
                        "Horario: " + r.getHoraInicio() + " - " + r.getHoraFin() + "\n\n" +
                        "¡No llegues tarde!\n\nAtentamente,\nEl equipo de UTP +Lab";
                try { emailService.enviarCorreo(correo, asunto, mensaje); } catch (Exception e) {}
            }
        }

        // 2. Enviar al Encargado
        if (encargado != null) {
            String correoEnc = obtenerCorreoEncargado(encargado);
            String asuntoEnc = "ES UNA PRUEBA ⏰ Aviso: Reserva próxima a iniciar (" + r.getIdReserva() + ")";
            String mensajeEnc = "Hola " + encargado.getNombre() + ",\n\n" +
                    "Te informamos que en 15 minutos iniciará una reserva en tu laboratorio.\n\n" +
                    "Laboratorio: " + r.getLaboratorio().getNombre() + " (" + r.getLaboratorio().getUbicacionCubiculo() + ")\n" +
                    "Código de Reserva: " + r.getIdReserva() + "\n" +
                    "Horario: " + r.getHoraInicio() + " - " + r.getHoraFin() + "\n\n" +
                    "Por favor, prepárate para recibir a los estudiantes y realizar el Check-in.\n\n" +
                    "Atentamente,\nEl sistema de UTP +Lab";
            try { 
                emailService.enviarCorreo(correoEnc, asuntoEnc, mensajeEnc); 
                System.out.println("✅ Correo de ENTRADA enviado al Encargado: " + correoEnc);
            } catch (Exception e) { 
                System.out.println("❌ Error correo Encargado: " + e.getMessage()); 
            }
        }
    }

    private void enviarCorreoSalida(Reserva r, com.integrador.lab.modelo.Encargado encargado) {
        // 1. Enviar a los Alumnos
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                String correo = ri.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe";
                String asunto = "ES UNA PRUEBA ⏰ Recordatorio: Tu reserva termina en 10 minutos - UTP +Lab";
                String mensaje = "Hola " + ri.getAlumno().getNombre() + ",\n\n" +
                        "Tu reserva en " + r.getLaboratorio().getNombre() + " está por finalizar.\n" +
                        "Por favor, prepara tus materiales para la devolución.\n\n" +
                        "Atentamente,\nEl equipo de UTP +Lab";
                try { emailService.enviarCorreo(correo, asunto, mensaje); } catch (Exception e) {}
            }
        }

        // 2. Enviar al Encargado
        if (encargado != null) {
            String correoEnc = obtenerCorreoEncargado(encargado);
            String asuntoEnc = "ES UNA PRUEBA⌛ Aviso: Reserva por terminar (" + r.getIdReserva() + ")";
            String mensajeEnc = "Hola " + encargado.getNombre() + ",\n\n" +
                    "Te informamos que la siguiente reserva finalizará en 10 minutos.\n\n" +
                    "Laboratorio: " + r.getLaboratorio().getNombre() + " (" + r.getLaboratorio().getUbicacionCubiculo() + ")\n" +
                    "Código de Reserva: " + r.getIdReserva() + "\n" +
                    "Horario: " + r.getHoraInicio() + " - " + r.getHoraFin() + "\n\n" +
                    "Por favor, prepárate para recibir los materiales y realizar la inspección correspondiente.\n\n" +
                    "Atentamente,\nEl sistema de UTP +Lab";
            try { 
                emailService.enviarCorreo(correoEnc, asuntoEnc, mensajeEnc); 
                System.out.println("✅ Correo de SALIDA enviado al Encargado: " + correoEnc);
            } catch (Exception e) { 
                System.out.println("❌ Error correo Encargado: " + e.getMessage()); 
            }
        }
    }
}