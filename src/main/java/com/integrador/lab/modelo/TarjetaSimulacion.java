package com.integrador.lab.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tarjeta_simulacion")
public class TarjetaSimulacion {

    @Id
    @Column(name = "numero_tarjeta", length = 16)
    private String numeroTarjeta;

    @Column(nullable = false, length = 100)
    private String titular;

    @Column(name = "fecha_expiracion", nullable = false, length = 5)
    private String fechaExpiracion;

    @Column(nullable = false, length = 3)
    private String cvv;

    @Column(nullable = false, length = 20)
    private String estado;

    public TarjetaSimulacion() {}

    public String getNumeroTarjeta() { return numeroTarjeta; }
    public void setNumeroTarjeta(String numeroTarjeta) { this.numeroTarjeta = numeroTarjeta; }
    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }
    public String getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(String fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}