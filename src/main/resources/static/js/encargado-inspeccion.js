let idReservaInspActual = null;
let listaItemsParaInspeccionar = [];

// ============================================================================
// 1. CHECKOUT (Registrar Salida) — solo para reservas EN_USO
// ============================================================================
function abrirModalCheckout(idReserva) {
    fetch(`/encargado/checkin/api/${idReserva}`)
        .then(res => res.json())
        .then(data => {
            let nombreResponsable = "Estudiante";
            if (data.integrantes && data.integrantes.length > 0) {
                nombreResponsable = data.integrantes[0].nombre;
            }

            let totalItems = 0;
            if (data.insumos) totalItems += data.insumos.length;
            if (data.epps) totalItems += data.epps.length;

            let modalCheckoutHTML = `
                <div id="modalCheckoutFlotante" style="position: fixed; inset: 0; background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 99999;">
                    <div style="background: #ffffff; border-radius: 16px; width: 90%; max-width: 440px; padding: 2rem; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.2);">
                        
                        <h3 style="margin: 0 0 0.2rem 0; font-size: 1.2rem; font-weight: 800; color: #0f172a;">Confirmar devolución</h3>
                        <p style="margin: 0 0 1.5rem 0; font-size: 0.85rem; color: #64748b;">${data.laboratorio} · ${idReserva}</p>

                        <div style="background: #f8fafc; border-radius: 10px; padding: 1.25rem; display: flex; flex-direction: column; gap: 0.8rem; margin-bottom: 1.5rem;">
                            <div style="display: flex; justify-content: space-between; font-size: 0.9rem;">
                                <span style="color: #64748b;">Responsable</span>
                                <strong style="color: #0f172a;">${nombreResponsable}</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; font-size: 0.9rem;">
                                <span style="color: #64748b;">Horario</span>
                                <strong style="color: #0f172a;">${data.horario}</strong>
                            </div>
                            <div style="display: flex; justify-content: space-between; font-size: 0.9rem;">
                                <span style="color: #64748b;">Ítems a recibir</span>
                                <strong style="color: #0f172a;">${totalItems}</strong>
                            </div>
                        </div>

                        <div style="background: #f5f3ff; border-radius: 8px; padding: 0.75rem 1rem; display: flex; align-items: flex-start; gap: 0.6rem; color: #7c3aed; font-size: 0.82rem; font-weight: 500; margin-bottom: 1.5rem;">
                            <i data-lucide="search" style="width: 16px; height: 16px; flex-shrink: 0; margin-top: 2px;"></i>
                            <span>Tras confirmar, los materiales pasan a inspección.</span>
                        </div>

                        <div style="display: flex; gap: 0.75rem;">
                            <button type="button" onclick="cerrarModalCheckoutFlotante()" style="flex: 1; background: #f1f5f9; color: #475569; border: none; padding: 0.75rem; border-radius: 8px; font-weight: 600; cursor: pointer;">Cancelar</button>
                            <button type="button" id="btnConfirmarCheckout" onclick="procesarCheckout('${idReserva}')" style="flex: 1; background: #7c3aed; color: #ffffff; border: none; padding: 0.75rem; border-radius: 8px; font-weight: 600; cursor: pointer;">Confirmar</button>
                        </div>
                    </div>
                </div>
            `;
            
            document.body.insertAdjacentHTML('beforeend', modalCheckoutHTML);
            if (typeof lucide !== 'undefined') lucide.createIcons();
        })
        .catch(() => {
            alert("No se pudieron cargar los datos de la reserva para el checkout.");
        });
}

function cerrarModalCheckoutFlotante() {
    const modal = document.getElementById('modalCheckoutFlotante');
    if (modal) modal.remove();
}

function procesarCheckout(idReserva) {
    const btn = document.getElementById('btnConfirmarCheckout');
    btn.innerText = "Procesando...";
    btn.disabled = true;

    fetch(`/encargado/inspeccion/checkout/${idReserva}`, { method: 'POST' })
        .then(res => {
            if (!res.ok) {
                return res.json().then(err => { throw new Error(err.error || "Error al procesar"); });
            }
            return res.json();
        })
        .then(() => {
            cerrarModalCheckoutFlotante();
            // ⚠️ IMPORTANTE: Recargamos la página para que la tarjeta cambie a DEVUELTA
            // O alternativamente, abrimos el modal de inspección directamente
            window.location.reload();
        })
        .catch(err => {
            alert("Error: " + err.message);
            btn.innerText = "Confirmar";
            btn.disabled = false;
        });
}

// ============================================================================
// 2. INSPECCIÓN (Abrir modal) — para reservas DEVUELTA
// ============================================================================
function abrirModalInspeccion(idReserva) {
    idReservaInspActual = idReserva;
    listaItemsParaInspeccionar = [];
    
    document.getElementById('modalInspeccion').style.display = 'flex';
    document.getElementById('modalInspSubtitulo').textContent = idReserva;
    
    fetch(`/encargado/inspeccion/api/${idReserva}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('modalInspLabTitulo').textContent = data.laboratorio || 'Inspección';
            document.getElementById('modalInspSubtitulo').textContent = 
                `${data.laboratorio || ''} · ${data.horario || ''} · ${idReserva}`;

            listaItemsParaInspeccionar = [];
            
            const contInsumos = document.getElementById('listaInspInsumos');
            const contEpps = document.getElementById('listaInspEpps');
            contInsumos.innerHTML = '';
            contEpps.innerHTML = '';

            // Insumos
            if (data.insumos && data.insumos.length > 0) {
                data.insumos.forEach(item => {
                    listaItemsParaInspeccionar.push({...item, estado: 'conforme', motivo: ''});
                    contInsumos.insertAdjacentHTML('beforeend', generarHtmlItem(item));
                });
            } else {
                contInsumos.innerHTML = '<p style="color:#94a3b8; font-size:0.85rem;">Sin insumos prestados</p>';
            }

            // EPPs
            if (data.epps && data.epps.length > 0) {
                data.epps.forEach(item => {
                    listaItemsParaInspeccionar.push({...item, estado: 'conforme', motivo: ''});
                    contEpps.insertAdjacentHTML('beforeend', generarHtmlItem(item));
                });
            } else {
                contEpps.innerHTML = '<p style="color:#94a3b8; font-size:0.85rem;">Sin EPPs prestados</p>';
            }

            evaluarEstadoGeneral();
            if (typeof lucide !== 'undefined') lucide.createIcons();
        })
        .catch(() => {
            alert("Error al cargar los datos para inspección.");
            cerrarModalInspeccion();
        });
}

function generarHtmlItem(item) {
    return `
        <div class="tarjeta-item-inspeccionar" id="row-item-${item.id}">
            <div class="fila-principal-item-insp">
                <div style="display:flex; align-items:center; gap:0.6rem;">
                    <span style="font-size: 1.25rem;">${item.emoji}</span>
                    <div>
                        <strong style="font-size:0.88rem; color:#0f172a; display:block;">${item.nombre}</strong>
                        <span style="font-size:0.75rem; color:#64748b;">Reposición: S/ ${Number(item.costoReposicion).toFixed(2)}</span>
                    </div>
                </div>
                <div class="acciones-estado-item">
                    <button type="button" 
                            class="btn-estado-check activo-conforme" 
                            id="btn-conf-${item.id}" 
                            onclick="marcarEstado('${item.id}', 'conforme')">✓ Conforme</button>
                    <button type="button" 
                            class="btn-estado-check" 
                            id="btn-dan-${item.id}" 
                            onclick="marcarEstado('${item.id}', 'danado')">✕ Dañado / Pérdida</button>
                </div>
            </div>
            <input type="text" 
                   id="motivo-${item.id}" 
                   class="input-motivo-dano" 
                   placeholder="Describa el daño o pérdida..."
                   oninput="actualizarMotivoTexto('${item.id}', this.value)" />
        </div>
    `;
}

function marcarEstado(id, nuevoEstado) {
    const it = listaItemsParaInspeccionar.find(x => String(x.id) === String(id));
    if (!it) return;
    it.estado = nuevoEstado;
    
    const fila = document.getElementById(`row-item-${id}`);
    const btnConf = document.getElementById(`btn-conf-${id}`);
    const btnDan = document.getElementById(`btn-dan-${id}`);
    const inputMotivo = document.getElementById(`motivo-${id}`);

    if (nuevoEstado === 'danado') {
        fila.classList.add('marcado-danado');
        btnConf.classList.remove('activo-conforme');
        btnDan.classList.add('activo-danado');
        inputMotivo.style.display = 'block';
        inputMotivo.focus();
    } else {
        fila.classList.remove('marcado-danado');
        btnConf.classList.add('activo-conforme');
        btnDan.classList.remove('activo-danado');
        inputMotivo.style.display = 'none';
        it.motivo = '';
    }
    evaluarEstadoGeneral();
}

function actualizarMotivoTexto(id, val) {
    const it = listaItemsParaInspeccionar.find(x => String(x.id) === String(id));
    if (it) it.motivo = val;
}

function evaluarEstadoGeneral() {
    const danados = listaItemsParaInspeccionar.filter(x => x.estado === 'danado');
    const txt = document.getElementById('txtAlertaDanio');
    const btn = document.getElementById('btnFinalizarInsp');

    if (danados.length > 0) {
        txt.style.color = '#dc2626';
        txt.innerText = `⚠️ ${danados.length} daño(s) detectado(s) · Pasará a Incidencias`;
        btn.style.backgroundColor = '#dc2626';
    } else {
        txt.style.color = '#059669';
        txt.innerText = '✓ Todos los materiales conformes';
        btn.style.backgroundColor = '#0d9488';
    }
}

function cerrarModalInspeccion() {
    document.getElementById('modalInspeccion').style.display = 'none';
    idReservaInspActual = null;
}

function enviarFinalizarInspeccion() {
    if (!idReservaInspActual) return;

    const reservaIdAEliminar = idReservaInspActual;

    const danados = listaItemsParaInspeccionar
        .filter(x => x.estado === 'danado')
        .map(x => ({
            idProducto: x.id,
            nombre: x.nombre,
            costoReposicion: x.costoReposicion,
            motivo: x.motivo && x.motivo.trim() !== '' ? x.motivo.trim() : `Daño en ${x.nombre}`
        }));

    const btn = document.getElementById('btnFinalizarInsp');
    btn.innerText = "Finalizando...";
    btn.disabled = true;

    fetch(`/encargado/inspeccion/finalizar/${reservaIdAEliminar}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ danados: danados })
    })
    .then(res => {
        if (!res.ok) throw new Error("Error en servidor");
        return res.json();
    })
    .then(data => {
        cerrarModalInspeccion();

        const tarjeta = document.getElementById(`tarjeta-inspeccion-${reservaIdAEliminar}`);
        if (tarjeta) {
            tarjeta.style.transition = 'all 0.3s ease';
            tarjeta.style.opacity = '0';
            tarjeta.style.transform = 'translateY(-10px)';
            setTimeout(() => {
                tarjeta.remove();
                const contenedor = document.getElementById('contenedorInspeccion');
                const restantes = contenedor ? contenedor.querySelectorAll('.tarjeta-reserva-checkin-fila').length : 0;
                if (restantes === 0) {
                    if (contenedor) contenedor.style.display = 'none';
                    const mensajeVacio = document.getElementById('tarjetaSinInspecciones');
                    if (mensajeVacio) mensajeVacio.style.display = 'flex';
                }
            }, 300);
        }

        if (data.huboDanios) {
            mostrarModalResultado(true, "Daño Registrado",
                "La inspección finalizó reportando daños. Se generó la sanción y la reserva fue trasladada al Módulo de Incidencias.");
        } else {
            mostrarModalResultado(false, "Inspección Conforme",
                "El material fue devuelto en perfecto estado. El laboratorio quedó completamente liberado.");
        }
    })
    .catch(err => {
        alert("Error al registrar la inspección: " + err.message);
    })
    .finally(() => {
        btn.innerText = "Finalizar Inspección →";
        btn.disabled = false;
    });
}

function mostrarModalResultado(huboDanio, titulo, mensaje) {
    const modal = document.getElementById('modalResultado');
    const icono = document.getElementById('iconoResultado');
    const tit = document.getElementById('tituloResultado');
    const desc = document.getElementById('mensajeResultado');

    if (huboDanio) {
        icono.className = 'icono-resultado-circulo res-danio';
        icono.innerText = '⚠️';
    } else {
        icono.className = 'icono-resultado-circulo res-exito';
        icono.innerText = '✓';
    }
    tit.innerText = titulo;
    desc.innerText = mensaje;
    modal.style.display = 'flex';
}

function cerrarModalResultado() {
    document.getElementById('modalResultado').style.display = 'none';
}

// ============================================================================
// 3. VER RESUMEN (para reservas INSPECCIONADA)
// ============================================================================
function abrirResumenInspeccion(idReserva) {
    fetch(`/encargado/checkin/api/${idReserva}`)
        .then(res => res.json())
        .then(data => {
            let resumenHtml = `<strong>Laboratorio:</strong> ${data.laboratorio}<br>`;
            resumenHtml += `<strong>Horario:</strong> ${data.horario}<br><br>`;
            
            resumenHtml += `<strong>Integrantes:</strong><br>`;
            data.integrantes.forEach(i => { resumenHtml += `- ${i.nombre}<br>`; });
            
            resumenHtml += `<br><strong>Insumos y EPPs Devueltos:</strong><br>`;
            data.insumos.forEach(i => { resumenHtml += `- ${i.nombre}<br>`; });
            data.epps.forEach(e => { resumenHtml += `- ${e.nombre}<br>`; });

            let html = `
                <div id="modalResumenInspeccion" style="position: fixed; inset: 0; background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 99999;">
                    <div style="background: #fff; border-radius: 16px; width: 90%; max-width: 440px; padding: 2rem; max-height: 80vh; overflow-y: auto;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 1rem;">
                            <h3 style="margin: 0; font-size: 1.2rem; font-weight: 800;">Resumen de Inspección</h3>
                            <button onclick="document.getElementById('modalResumenInspeccion').remove()" style="background: none; border: none; font-size: 1.2rem; cursor: pointer;">✕</button>
                        </div>
                        <div style="background: #f8fafc; border-radius: 10px; padding: 1.25rem; font-size: 0.88rem; color: #334155; line-height: 1.6;">
                            ${resumenHtml}
                        </div>
                        <button onclick="document.getElementById('modalResumenInspeccion').remove()" style="width: 100%; background: #0f172a; color: #fff; border: none; padding: 0.75rem; border-radius: 8px; font-weight: 600; cursor: pointer; margin-top: 1.5rem;">Cerrar</button>
                    </div>
                </div>
            `;
            document.body.insertAdjacentHTML('beforeend', html);
        });
}