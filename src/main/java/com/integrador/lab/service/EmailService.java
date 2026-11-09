package com.integrador.lab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();

    // Método base para enviar correos con o sin adjunto
    private void enviarCorreoBrevo(String destinatario, String nombreAlumno, String asunto, String cuerpo, byte[] archivoAdjunto, String nombreArchivo) {
        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.set("accept", "application/json");

        Map<String, Object> sender = new HashMap<>();
        sender.put("name", fromName);
        sender.put("email", fromEmail);

        Map<String, Object> to = new HashMap<>();
        to.put("email", destinatario);
        to.put("name", nombreAlumno);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", List.of(to));
        body.put("subject", asunto);
        body.put("textContent", cuerpo);

        // 👇 SI HAY ARCHIVO ADJUNTO, LO CONVERTIMOS A BASE64 Y LO AGREGAMOS 👇
        if (archivoAdjunto != null && archivoAdjunto.length > 0) {
            String pdfBase64 = Base64.getEncoder().encodeToString(archivoAdjunto);
            
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("content", pdfBase64);
            attachment.put("name", nombreArchivo);
            
            body.put("attachment", List.of(attachment));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ Correo enviado a: " + destinatario + " (status: " + response.getStatusCode() + ")");
        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo con Brevo: " + e.getMessage());
            throw new RuntimeException("Error al enviar correo", e);
        }
    }

    // 1. Método público para enviar correo sin adjunto (compatibilidad)
    public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        enviarCorreoBrevo(destinatario, "Estudiante UTP", asunto, cuerpo, null, null);
    }

    // 2. ⭐ NUEVO MÉTODO: Enviar correo CON adjunto (para la boleta PDF)
    public void enviarCorreoConAdjunto(String destinatario, String asunto, String cuerpo, byte[] archivoPdf, String nombreArchivo) {
        enviarCorreoBrevo(destinatario, "Estudiante UTP", asunto, cuerpo, archivoPdf, nombreArchivo);
    }

    // 3. Método existente para Restablecimiento de Contraseña
    public void enviarCodigoRestablecimiento(String destinatario, String nombreAlumno, String codigo) {
        String cuerpo = "Hola " + nombreAlumno + ",\n\n"
                + "ES UNA PRUEBA Recibimos una solicitud para restablecer tu contraseña en UTP +Lab.\n\n"
                + "Tu código de verificación es: " + codigo + "\n\n"
                + "Este código expira en 15 minutos. Si no solicitaste este cambio, ignora este mensaje.\n\n"
                + "Atentamente,\n"
                + "Equipo UTP +Lab";

        enviarCorreoBrevo(destinatario, nombreAlumno, "UTP +Lab - Código para restablecer contraseña", cuerpo, null, null);
    }

    // 4. Método existente para Notificación de Incidencia
    public void enviarCorreoIncidencia(String destinatario, String nombreAlumno, String motivo, String monto, String idReserva) {
        String cuerpo = "Hola " + nombreAlumno + ",\n\n"
                + " ESTO ES UNA PRUEBA Se ha registrado una incidencia a tu nombre durante la reserva " + idReserva + ".\n\n"
                + "Detalle del daño: " + motivo + "\n"
                + "Costo a pagar: S/ " + monto + "\n\n"
                + "Tu cuenta ha sido bloqueada temporalmente hasta que realices el pago correspondiente desde tu portal de estudiante (sección Incidencias).\n\n"
                + "Atentamente,\n"
                + "El equipo de UTP +Lab";

        enviarCorreoBrevo(destinatario, nombreAlumno, "⚠️ Notificación de Incidencia - UTP +Lab", cuerpo, null, null);
    }
}