package com.integrador.lab.controller.admin;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.integrador.lab.modelo.Encargado;
import com.integrador.lab.repository.EncargadoRepository;

@Controller
@RequestMapping("/admin/encargados")
public class EncargadosAdminController {

    @Autowired
    private EncargadoRepository encargadoRepositorio;

    // 1. LISTAR TODOS
    @GetMapping
    @Transactional(readOnly = true)
    public String verEncargados(Model modelo) {
        List<Encargado> encargados = encargadoRepositorio.findAll();
        modelo.addAttribute("encargados", encargados);
        return "admin/encargados"; // Asegúrate de tener este HTML
    }

    // 2. GUARDAR (CREAR O EDITAR)
    @PostMapping("/guardar")
    @Transactional
    public String guardarEncargado(
            @RequestParam("idEncargado") String idEncargado,
            @RequestParam("dni") String dni,
            @RequestParam("nombre") String nombre,
            @RequestParam("apellido") String apellido,
            @RequestParam(value = "password", required = false) String password,
            RedirectAttributes redirectAttributes) {

        // Si el ID ya existe, es una edición
        Optional<Encargado> existente = encargadoRepositorio.findById(idEncargado);
        
        Encargado enc;
        if (existente.isPresent()) {
            enc = existente.get();
            enc.setDni(dni);
            enc.setNombre(nombre);
            enc.setApellido(apellido);
            // Solo actualizamos la contraseña si se envió una nueva
            if (password != null && !password.trim().isEmpty()) {
                enc.setPassword(password);
            }
        } else {
            // Es uno nuevo
            if (password == null || password.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria para nuevos encargados.");
                return "redirect:/admin/encargados";
            }
            enc = new Encargado(idEncargado, dni, password, nombre, apellido);
        }

        try {
            encargadoRepositorio.save(enc);
            redirectAttributes.addFlashAttribute("mensaje", "Encargado guardado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar. Verifica que el DNI no esté duplicado.");
        }

        return "redirect:/admin/encargados";
    }

    // 3. ELIMINAR
    @GetMapping("/eliminar/{idEncargado}")
    @Transactional
    public String eliminarEncargado(@PathVariable("idEncargado") String idEncargado, RedirectAttributes redirectAttributes) {
        try {
            // Verificar si tiene reservas asociadas (opcional, pero recomendado para evitar errores de FK)
            // Si tienes una relación en Reserva, podrías verificar aquí.
            
            encargadoRepositorio.deleteById(idEncargado);
            redirectAttributes.addFlashAttribute("mensaje", "Encargado eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el encargado porque tiene registros asociados.");
        }
        return "redirect:/admin/encargados";
    }
}