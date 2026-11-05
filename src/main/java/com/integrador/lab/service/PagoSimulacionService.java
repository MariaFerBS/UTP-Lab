package com.integrador.lab.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.integrador.lab.modelo.TarjetaSimulacion;
import com.integrador.lab.repository.TarjetaSimulacionRepository;

@Service
public class PagoSimulacionService {

    @Autowired
    private TarjetaSimulacionRepository tarjetaRepositorio;

    public ResultadoPago procesarPago(
        String metodo, 
        String numeroTarjeta, 
        String titular, 
        String fechaExpiracion, 
        String cvv, 
        BigDecimal monto) {

    // YAPE y PLIN: siempre aprobados
    if ("YAPE".equalsIgnoreCase(metodo) || "PLIN".equalsIgnoreCase(metodo)) {
        String txn = "TXN-" + (1000000000L + (long)(Math.random() * 9000000000L));
        return new ResultadoPago(true, "Pago aprobado vía " + metodo.toUpperCase(), txn);
    }

    // TARJETA: validar contra la tabla de simulación
    if ("TARJETA".equalsIgnoreCase(metodo)) {
        if (numeroTarjeta == null || numeroTarjeta.trim().isEmpty()) {
            return new ResultadoPago(false, "Debes ingresar el número de tarjeta.", null);
        }

        String numero = numeroTarjeta.trim().replaceAll("\\s", "");
        Optional<TarjetaSimulacion> opt = tarjetaRepositorio.findById(numero);

        if (opt.isEmpty()) {
            return new ResultadoPago(false, 
                "❌ La tarjeta no existe. Verifica el número ingresado.", null);
        }

        TarjetaSimulacion t = opt.get();

        // ⭐ VALIDAR TITULAR
        if (titular == null || !titular.trim().equalsIgnoreCase(t.getTitular().trim())) {
            return new ResultadoPago(false, 
                "❌ El nombre del titular no coincide con el de la tarjeta.", null);
        }

        // ⭐ VALIDAR FECHA DE EXPIRACIÓN
        if (fechaExpiracion == null || 
            !fechaExpiracion.trim().equalsIgnoreCase(t.getFechaExpiracion().trim())) {
            return new ResultadoPago(false, 
                "❌ La fecha de expiración no coincide con la de la tarjeta.", null);
        }

        // ⭐ VALIDAR CVV
        if (cvv == null || !cvv.trim().equals(t.getCvv().trim())) {
            return new ResultadoPago(false, 
                "❌ El CVV no coincide con el de la tarjeta.", null);
        }

        // Validar estado de la tarjeta
        if ("ACEPTADA".equalsIgnoreCase(t.getEstado())) {
            String txn = "TXN-" + (1000000000L + (long)(Math.random() * 9000000000L));
            return new ResultadoPago(true, 
                "Pago aprobado con tarjeta " + enmascarar(numero), txn);
        }

        if ("RECHAZADA_FONDOS".equalsIgnoreCase(t.getEstado())) {
            return new ResultadoPago(false, 
                "❌ Fondos insuficientes. La tarjeta no tiene saldo disponible.", null);
        }

        if ("RECHAZADA_NO_EXISTE".equalsIgnoreCase(t.getEstado())) {
            return new ResultadoPago(false, 
                "❌ Tarjeta rechazada por el banco emisor.", null);
        }

        return new ResultadoPago(false, "❌ Estado de tarjeta desconocido.", null);
    }

    // Si es GRATUITO
    if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
        return new ResultadoPago(true, "Reserva gratuita aprobada", null);
    }

    return new ResultadoPago(false, "Método de pago no reconocido.", null);
}

    private String enmascarar(String numero) {
        if (numero.length() < 4) return "****";
        return "****" + numero.substring(numero.length() - 4);
    }

    public static class ResultadoPago {
        private boolean exitoso;
        private String mensaje;
        private String transaccion;

        public ResultadoPago(boolean exitoso, String mensaje, String transaccion) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.transaccion = transaccion;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public String getTransaccion() { return transaccion; }
    }
}