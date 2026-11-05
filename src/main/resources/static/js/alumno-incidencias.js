let idSancionActual = '';
let montoActual = 'S/ 0.00';
let metodoPagoActual = 'YAPE';

document.addEventListener("DOMContentLoaded", function () {
  if (typeof lucide !== 'undefined') lucide.createIcons();
});

function abrirModalPago(btn) {
  idSancionActual = btn.getAttribute('data-id');
  const item = btn.getAttribute('data-item') || 'Material de Lab';
  montoActual = btn.getAttribute('data-monto') || 'S/ 0.00';

  document.getElementById('modalTituloInc').innerText = `Pago de Incidencia · ${item}`;
  document.getElementById('modalMontoPagar').innerText = montoActual;
  document.getElementById('labelComercioQr').innerText = `UTP Lab · ${montoActual}`;
  document.getElementById('btnConfirmarPago').innerText = `Confirmar pago · ${montoActual}`;

  // Reset a Yape
  metodoPagoActual = 'YAPE';
  document.querySelectorAll('.opcion-metodo').forEach(o => o.classList.remove('seleccionado'));
  document.querySelectorAll('.opcion-metodo')[0].classList.add('seleccionado');
  document.getElementById('cajaDetalleMetodo').style.display = 'block';
  document.getElementById('seccionTarjeta').style.display = 'none';

  document.getElementById('pasoPagoSeleccion').style.display = 'block';
  document.getElementById('pasoPagoExitoso').style.display = 'none';
  document.getElementById('modalPagoIncidencia').style.display = 'flex';

  if (typeof lucide !== 'undefined') lucide.createIcons();
}

function cerrarModalPago() {
  document.getElementById('modalPagoIncidencia').style.display = 'none';
}

function seleccionarMetodo(elem, metodo) {
  document.querySelectorAll('.opcion-metodo').forEach(el => el.classList.remove('seleccionado'));
  elem.classList.add('seleccionado');
  metodoPagoActual = metodo;

  const cajaQr = document.getElementById('cajaDetalleMetodo');
  const seccionTarjeta = document.getElementById('seccionTarjeta');
  const tituloQr = document.getElementById('tituloQrMetodo');
  const imagenQr = document.getElementById('imagenQrPago');

  if (metodo === 'TARJETA') {
    cajaQr.style.display = 'none';
    seccionTarjeta.style.display = 'block';
  } else {
    cajaQr.style.display = 'block';
    seccionTarjeta.style.display = 'none';

    if (metodo === 'YAPE') {
      tituloQr.innerText = 'Pagar con Yape';
      tituloQr.style.color = '#7c3aed';
      imagenQr.src = 'https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=UTP-LAB-YAPE&color=7c3aed';
    } else {
      tituloQr.innerText = 'Pagar con Plin';
      tituloQr.style.color = '#059669';
      imagenQr.src = 'https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=UTP-LAB-PLIN&color=059669';
    }
  }
}

function ejecutarPagoBackend() {
  const btn = document.getElementById('btnConfirmarPago');
  btn.innerText = "Procesando pago...";
  btn.disabled = true;

  const params = new URLSearchParams();
  params.append('metodoPago', metodoPagoActual);

  if (metodoPagoActual === 'TARJETA') {
    const num = document.getElementById('inputNumeroTarjeta').value.replace(/\s/g, '');
    const titular = document.getElementById('inputTitularTarjeta').value.trim();
    const fecha = document.getElementById('inputFechaExpTarjeta').value.trim();
    const cvv = document.getElementById('inputCvvTarjeta').value.trim();

    // Validaciones locales (rápidas)
    if (!num || num.length < 16) {
      alert('❌ Ingresa un número de tarjeta válido (16 dígitos).');
      btn.innerText = `Confirmar pago · ${montoActual}`;
      btn.disabled = false;
      return;
    }
    if (!titular) {
      alert('❌ Ingresa el nombre del titular.');
      btn.innerText = `Confirmar pago · ${montoActual}`;
      btn.disabled = false;
      return;
    }
    if (!fecha || !cvv) {
      alert('❌ Completa la fecha de expiración y el CVV.');
      btn.innerText = `Confirmar pago · ${montoActual}`;
      btn.disabled = false;
      return;
    }

    // ⭐ Enviar TODOS los datos al backend
    params.append('numeroTarjeta', num);
    params.append('titularTarjeta', titular);
    params.append('fechaExpTarjeta', fecha);
    params.append('cvvTarjeta', cvv);
  }

  fetch(`/alumno/incidencias/pagar/${idSancionActual}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params
  })
  .then(res => res.json().then(data => ({ ok: res.ok, data })))
  .then(({ ok, data }) => {
    if (!ok) {
      alert('❌ ' + (data.error || 'Error al procesar el pago'));
      btn.innerText = `Confirmar pago · ${montoActual}`;
      btn.disabled = false;
      return;
    }

    // Pago exitoso
    document.getElementById('pasoPagoSeleccion').style.display = 'none';
    document.getElementById('subtextoExitoMonto').innerText = `${montoActual} procesados correctamente`;
    document.getElementById('textoTxnCode').innerText = data.transaccion || 'TXN-000';
    document.getElementById('pasoPagoExitoso').style.display = 'flex';

    if (typeof lucide !== 'undefined') lucide.createIcons();
  })
  .catch(() => {
    alert("Error al procesar el pago. Por favor intenta de nuevo.");
    btn.innerText = `Confirmar pago · ${montoActual}`;
    btn.disabled = false;
  });
}

function finalizarPagoYRecargar() {
  cerrarModalPago();
  window.location.reload(); // Recargar para reflejar el pago grupal
}