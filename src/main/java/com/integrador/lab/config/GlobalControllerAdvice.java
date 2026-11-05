package com.integrador.lab.config;

import com.integrador.lab.modelo.Sancion;
import com.integrador.lab.repository.SancionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private SancionRepository sancionRepositorio;

    @ModelAttribute("totalIncidencias")
    public long getTotalIncidencias() {
        try {
            // 1. Traemos todas las sanciones activas
            List<Sancion> activas = sancionRepositorio.findAll().stream()
                    .filter(s -> !"RESUELTA".equalsIgnoreCase(s.getEstado()) 
                              && !"PAGADA".equalsIgnoreCase(s.getEstado()))
                    .toList();

            // 2. Las agrupamos por reserva y contamos cuántos grupos únicos hay
            return activas.stream()
                    .collect(Collectors.groupingBy(s -> 
                        s.getReserva() != null ? s.getReserva().getIdReserva() : "SIN_RESERVA"))
                    .size();

        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}