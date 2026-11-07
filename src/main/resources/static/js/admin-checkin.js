// =====================================================================
// JS DE SOLO LECTURA PARA EL ADMINISTRADOR (CHECK-IN)
// =====================================================================

function abrirResumenCheckin(idReserva) {
    fetch(`/admin/checkin/api/${idReserva}`)
        .then(res => res.json())
        .then(data => {
            let resumenHtml = `<strong>Laboratorio:</strong> ${data.laboratorio}<br>`;
            resumenHtml += `<strong>Horario:</strong> ${data.horario}<br>`;

            // 👇 NUEVO: Mostramos al encargado si existe
            if (data.nombreEncargado) {
                resumenHtml += `<div style="margin-top: 8px; padding: 8px; background: #ecfdf5; border-radius: 6px; color: #065f46; border: 1px solid #10b981;">`;
                resumenHtml += `<strong>✅ Check-in realizado por:</strong> ${data.nombreEncargado} <br>`;
                resumenHtml += `<span style="font-size: 0.85rem;">A las ${data.horaAccion}</span>`;
                resumenHtml += `</div><br>`;
            }
            
            resumenHtml += `<strong>Integrantes:</strong><br>`;
            data.integrantes.forEach(i => { resumenHtml += `- ${i.nombre} (${i.codigo})<br>`; });
            
            resumenHtml += `<br><strong>Insumos y EPPs:</strong><br>`;
            
            if (data.insumos && data.insumos.length > 0) {
                data.insumos.forEach(i => { resumenHtml += `- 📦 ${i.nombre}<br>`; });
            } else {
                resumenHtml += `- Sin insumos registrados<br>`;
            }

            if (data.epps && data.epps.length > 0) {
                data.epps.forEach(e => { resumenHtml += `- ${e.emoji} ${e.nombre}<br>`; });
            }

            let html = `
                <div id="modalResumenCheckin" style="position: fixed; inset: 0; background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 99999;">
                    <div style="background: #fff; border-radius: 16px; width: 90%; max-width: 440px; padding: 2rem; max-height: 80vh; overflow-y: auto;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 1rem;">
                            <h3 style="margin: 0; font-size: 1.2rem; font-weight: 800;">Resumen de Check-in</h3>
                            <button onclick="document.getElementById('modalResumenCheckin').remove()" style="background: none; border: none; font-size: 1.2rem; cursor: pointer;">✕</button>
                        </div>
                        <div style="background: #f8fafc; border-radius: 10px; padding: 1.25rem; font-size: 0.88rem; color: #334155; line-height: 1.6;">
                            ${resumenHtml}
                        </div>
                        <button onclick="document.getElementById('modalResumenCheckin').remove()" style="width: 100%; background: #0f172a; color: #fff; border: none; padding: 0.75rem; border-radius: 8px; font-weight: 600; cursor: pointer; margin-top: 1.5rem;">Cerrar</button>
                    </div>
                </div>
            `;
            document.body.insertAdjacentHTML('beforeend', html);
        })
        .catch(() => {
            alert("No se pudieron cargar los detalles de esta reserva.");
        });
}