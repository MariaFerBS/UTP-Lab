package com.integrador.lab.controller.admin;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.integrador.lab.modelo.Laboratorio;
import com.integrador.lab.modelo.Producto;
import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.modelo.ReservaIntegrante;
import com.integrador.lab.modelo.Sancion;
import com.integrador.lab.repository.LaboratorioRepository;
import com.integrador.lab.repository.ProductoRepository;
import com.integrador.lab.repository.ReservaIntegranteRepository;
import com.integrador.lab.repository.ReservaRepository;
import com.integrador.lab.repository.SancionRepository;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private LaboratorioRepository laboratorioRepositorio;

    @Autowired
    private ReservaRepository reservaRepositorio;

    @Autowired
    private ReservaIntegranteRepository reservaIntegranteRepositorio;

    @Autowired
    private ProductoRepository productoRepositorio;

    @Autowired
    private SancionRepository sancionRepositorio;

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String verDashboard(
            @RequestParam(required = false) String fecha,
            Model modelo) {

        // ============ FECHA DE CONSULTA ============
        LocalDate fechaConsulta;
        try {
            fechaConsulta = (fecha != null && !fecha.isEmpty()) 
                    ? LocalDate.parse(fecha) 
                    : LocalDate.now();
        } catch (Exception e) {
            fechaConsulta = LocalDate.now();
        }

        LocalDate hoy = LocalDate.now();
        boolean esHoy = fechaConsulta.equals(hoy);

        long inspeccionesPendientes = reservaRepositorio.findByEstado("DEVUELTA").size();
        modelo.addAttribute("inspeccionesPendientes", inspeccionesPendientes);

        Locale espLocale = new Locale("es", "PE");
        String diaSemana = fechaConsulta.getDayOfWeek().getDisplayName(TextStyle.FULL, espLocale);
        String mes = fechaConsulta.getMonth().getDisplayName(TextStyle.FULL, espLocale);
        String fechaFormateada = diaSemana + ", " + fechaConsulta.getDayOfMonth() + " de " + mes + " de " + fechaConsulta.getYear();

        List<Reserva> reservasDeFecha = reservaRepositorio.findByFechaReserva(fechaConsulta);
        if (reservasDeFecha == null) reservasDeFecha = new ArrayList<>();

        long cantLabsEnUso = reservasDeFecha.stream()
                .filter(r -> "EN_USO".equals(r.getEstado()))
                .count();

        long cantPendientesCheckin = reservasDeFecha.stream()
                .filter(r -> "CONFIRMADA".equals(r.getEstado()) || "PENDIENTE".equals(r.getEstado()))
                .count();

        long cantPrestamosActivos = cantLabsEnUso;

        List<Sancion> sancionesPendientes = sancionRepositorio.findAll().stream()
                .filter(s -> "PENDIENTE".equalsIgnoreCase(s.getEstado()) || "NOTIFICADA".equalsIgnoreCase(s.getEstado()))
                .toList();
        
        long cantIncidentesAbiertos = sancionRepositorio.findAll().stream()
                .filter(s -> !"RESUELTA".equalsIgnoreCase(s.getEstado()) 
                          && !"PAGADA".equalsIgnoreCase(s.getEstado()))
                .count();

        List<Producto> todosProductos = productoRepositorio.findAll();
        List<AlertaStockDto> alertasStock = new ArrayList<>();
        for (Producto p : todosProductos) {
            int stock = (p.getStockActual() != null) ? p.getStockActual() : 0;
            if (stock <= 10) {
                int minReq = (stock < 5) ? 8 : (stock < 50 ? 50 : 100);
                String unidad = "und";
                String cat = (p.getCategoria() != null) ? p.getCategoria().toUpperCase() : "";
                if (cat.contains("REACT")) unidad = "g";
                else if (cat.contains("SOLV")) unidad = "mL";
                alertasStock.add(new AlertaStockDto(p.getNombre(), stock + " " + unidad, String.valueOf(minReq)));
            }
        }
        long cantAlertasStock = alertasStock.size();

        List<Laboratorio> labsBD = laboratorioRepositorio.findAll();
        List<EstadoLabDto> listaEstadosLabs = new ArrayList<>();

        for (Laboratorio lab : labsBD) {
            String cubiculo = (lab.getUbicacionCubiculo() != null && !lab.getUbicacionCubiculo().isBlank())
                    ? lab.getUbicacionCubiculo().trim() : lab.getIdLab();
            String estado = "Libre";

            for (Reserva r : reservasDeFecha) {
                if (r.getLaboratorio() != null && r.getLaboratorio().getIdLab().equals(lab.getIdLab())) {
                    if ("EN_USO".equals(r.getEstado())) estado = "En uso";
                    else if ("MANTENIMIENTO".equals(r.getEstado())) estado = "Mantenimiento";
                    break;
                }
            }

            listaEstadosLabs.add(new EstadoLabDto(
                    cubiculo,
                    lab.getNombre(),
                    "Bloque " + (cubiculo.length() > 0 ? cubiculo.charAt(0) : "A") + " · 12 puestos",
                    estado
            ));
        }

        if (listaEstadosLabs.size() < 10) {
            String[] defs = {"A-101", "A-102", "A-103", "A-104", "B-101", "B-102", "B-103", "B-104", "C-101", "C-102", "C-103", "C-104"};
            String[] nombres = {"Lab. Química", "Lab. Química", "Lab. Química", "Lab. Química", "Lab. Mecatrónica", "Lab. Mecatrónica", "Lab. Mecatrónica", "Lab. Mecatrónica", "Lab. Física", "Lab. Física", "Lab. Física", "Lab. Física"};

            listaEstadosLabs.clear();
            for (int i = 0; i < defs.length; i++) {
                String est = "Libre";
                for (Reserva r : reservasDeFecha) {
                    if (r.getLaboratorio() != null && r.getLaboratorio().getUbicacionCubiculo() != null 
                        && r.getLaboratorio().getUbicacionCubiculo().equals(defs[i]) && "EN_USO".equals(r.getEstado())) {
                        est = "En uso";
                        break;
                    }
                }
                listaEstadosLabs.add(new EstadoLabDto(defs[i], nombres[i], "Bloque " + defs[i].charAt(0) + " · 12 puestos", est));
            }
        }

        long totalLabsDisponibles = listaEstadosLabs.size();

        List<AgendaHoyDto> agenda = new ArrayList<>();
        for (Reserva r : reservasDeFecha) {
            String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                    ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";
            String labNom = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";

            String solicitante = "Estudiante";
            List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
            if (!integrantes.isEmpty() && integrantes.get(0).getAlumno() != null) {
                solicitante = integrantes.get(0).getAlumno().getNombre() + " " + integrantes.get(0).getAlumno().getApellido();
                if (integrantes.size() > 1) solicitante += " +" + (integrantes.size() - 1);
            }
            agenda.add(new AgendaHoyDto(horario, labNom, solicitante, r.getEstado()));
        }

        List<IncidenciaRecienteDto> incidenciasRecientes = new ArrayList<>();
        for (Sancion s : sancionesPendientes) {
            String est = (s.getAlumno() != null) ? s.getAlumno().getNombre() + " " + s.getAlumno().getApellido() : "Estudiante";
            String monto = (s.getMontoDeuda() != null) ? "S/ " + s.getMontoDeuda().setScale(2).toString() : "S/ 350.00";
            String desc = (s.getMotivo() != null && !s.getMotivo().isBlank()) ? s.getMotivo() : (s.getProducto() != null ? s.getProducto().getNombre() : "Material dañado");
            incidenciasRecientes.add(new IncidenciaRecienteDto(desc, est, monto));
        }

        // ============ 7. MÉTRICAS DEL MES ACTUAL (NUEVAS) ============
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate finMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        long totalReservasMes = reservaRepositorio.findAll().stream()
                .filter(r -> r.getFechaReserva() != null 
                        && !r.getFechaReserva().isBefore(inicioMes) 
                        && !r.getFechaReserva().isAfter(finMes)
                        && !"CANCELADA".equalsIgnoreCase(r.getEstado()))
                .count();

        long totalIncidenciasMes = sancionRepositorio.findAll().stream()
                .filter(s -> s.getFechaSancion() != null 
                        && !s.getFechaSancion().toLocalDate().isBefore(inicioMes) 
                        && !s.getFechaSancion().toLocalDate().isAfter(finMes))
                .count();

        // LÍMITE A 5 ELEMENTOS PARA EL DASHBOARD
        List<IncidenciaRecienteDto> top5Incidencias = incidenciasRecientes.stream().limit(5).toList();
        List<AlertaStockDto> top5Alertas = alertasStock.stream().limit(5).toList();

        // ============ 8. Pasar todo al modelo ============
        modelo.addAttribute("fechaHoy", fechaFormateada);
        modelo.addAttribute("fechaConsulta", fechaConsulta.toString());
        modelo.addAttribute("fechaAnterior", fechaConsulta.minusDays(1).toString());
        modelo.addAttribute("fechaSiguiente", fechaConsulta.plusDays(1).toString());
        modelo.addAttribute("esHoy", esHoy);

        modelo.addAttribute("labsEnUso", cantLabsEnUso);
        modelo.addAttribute("totalLabsDisp", totalLabsDisponibles);
        modelo.addAttribute("pendientesCheckin", cantPendientesCheckin);
        modelo.addAttribute("prestamosActivos", cantPrestamosActivos);
        modelo.addAttribute("incidentesAbiertos", cantIncidentesAbiertos);
        modelo.addAttribute("cantAlertasStock", cantAlertasStock);

        modelo.addAttribute("estadosLabs", listaEstadosLabs);
        modelo.addAttribute("agendaHoy", agenda);
        modelo.addAttribute("incidenciasRecientes", top5Incidencias);
        modelo.addAttribute("alertasStock", top5Alertas);

        modelo.addAttribute("totalReservasMes", totalReservasMes);
        modelo.addAttribute("totalIncidenciasMes", totalIncidenciasMes);

        return "admin/dashboard";
    }

    // DTOs
    public static class EstadoLabDto {
        private String codigo, nombre, ubicacion, estado;
        public EstadoLabDto(String codigo, String nombre, String ubicacion, String estado) {
            this.codigo = codigo; this.nombre = nombre; this.ubicacion = ubicacion; this.estado = estado;
        }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getUbicacion() { return ubicacion; }
        public String getEstado() { return estado; }
    }

    public static class AgendaHoyDto {
        private String horario, laboratorio, solicitante, estado;
        public AgendaHoyDto(String horario, String laboratorio, String solicitante, String estado) {
            this.horario = horario; this.laboratorio = laboratorio; this.solicitante = solicitante; this.estado = estado;
        }
        public String getHorario() { return horario; }
        public String getLaboratorio() { return laboratorio; }
        public String getSolicitante() { return solicitante; }
        public String getEstado() { return estado; }
    }

    public static class IncidenciaRecienteDto {
        private String item, estudiante, monto;
        public IncidenciaRecienteDto(String item, String estudiante, String monto) {
            this.item = item; this.estudiante = estudiante; this.monto = monto;
        }
        public String getItem() { return item; }
        public String getEstudiante() { return estudiante; }
        public String getMonto() { return monto; }
    }

    public static class AlertaStockDto {
        private String producto, stockActual, minimo;
        public AlertaStockDto(String producto, String stockActual, String minimo) {
            this.producto = producto; this.stockActual = stockActual; this.minimo = minimo;
        }
        public String getProducto() { return producto; }
        public String getStockActual() { return stockActual; }
        public String getMinimo() { return minimo; }
    }
}