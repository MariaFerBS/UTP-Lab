
document.addEventListener("DOMContentLoaded", function () {
    // Cargar iconos
    if (typeof lucide !== "undefined") {
        lucide.createIcons();
    }
});


// ==========================================
// ABRIR Y CERRAR NOTIFICACIONES
// ==========================================

async function toggleNotificaciones(event) {

    if (event) {
        event.stopPropagation();
    }

    const burbuja = document.getElementById("burbujaNotificaciones");
    const boton = document.querySelector(".boton-campana-amarilla");

    if (!burbuja) return;

    // Si está abierto, cerrarlo
    if (!burbuja.hidden) {
        cerrarNotificaciones();
        return;
    }

    // Cerrar antes de abrir
    cerrarSidebar();

    // Mostrar panel
    burbuja.hidden = false;

    if (boton) {
        boton.setAttribute("aria-expanded", "true");
    }

    // Cargar notificaciones
    await cargarNotificaciones();
}


// ==========================================
// CARGAR NOTIFICACIONES DESDE EL BACKEND
// ==========================================

async function cargarNotificaciones() {

    const contenedor = document.getElementById("listaNotificaciones");

    if (!contenedor) return;

    // Mostrar estado de carga
    contenedor.innerHTML = `
        <div class="estado-notificaciones">
            <span class="spinner-notificaciones"></span>
            <p>Cargando notificaciones...</p>
        </div>
    `;

    try {

        const respuesta = await fetch("/alumno/api/notificaciones", {
            method: "GET",
            headers: {
                "Accept": "application/json"
            },
            credentials: "same-origin"
        });

        if (!respuesta.ok) {
            throw new Error("Error HTTP: " + respuesta.status);
        }

        const data = await respuesta.json();

        if (!Array.isArray(data)) {
            throw new Error("La respuesta del servidor no es una lista.");
        }

        // Si no existen notificaciones
        if (data.length === 0) {

            contenedor.innerHTML = `
                <div class="sin-notificaciones">
                    <i data-lucide="check-circle"></i>
                    <p>No tienes notificaciones pendientes</p>
                </div>
            `;

            actualizarIconos();
            return;
        }

        // Limpiar contenido
        contenedor.innerHTML = "";

        // Crear cada notificación
        data.forEach(function (notif) {

            const enlace = document.createElement("a");

            enlace.className = "item-notificacion";
            enlace.href = notif.url || "/alumno/incidencias";

            const icono = document.createElement("div");
            icono.className = "icono-notif icono-notif-rojo";

            const iconoLucide = document.createElement("i");
            iconoLucide.setAttribute("data-lucide", "alert-triangle");

            icono.appendChild(iconoLucide);

            const informacion = document.createElement("div");
            informacion.className = "info-notif";

            const titulo = document.createElement("strong");
            titulo.textContent = notif.titulo || "Nueva notificación";

            const mensaje = document.createElement("span");
            mensaje.textContent = notif.mensaje || "Tienes una notificación pendiente.";

            informacion.appendChild(titulo);
            informacion.appendChild(mensaje);

            enlace.appendChild(icono);
            enlace.appendChild(informacion);

            contenedor.appendChild(enlace);
        });

        actualizarIconos();

    } catch (error) {

        console.error("Error al cargar las notificaciones:", error);

        contenedor.innerHTML = `
            <div class="error-notificaciones">
                <i data-lucide="alert-circle"></i>
                <p>No se pudieron cargar las notificaciones.</p>
                <button type="button" onclick="cargarNotificaciones()">
                    Intentar nuevamente
                </button>
            </div>
        `;

        actualizarIconos();
    }
}


// ==========================================
// CERRAR NOTIFICACIONES
// ==========================================

function cerrarNotificaciones() {

    const burbuja = document.getElementById("burbujaNotificaciones");
    const boton = document.querySelector(".boton-campana-amarilla");

    if (burbuja) {
        burbuja.hidden = true;
    }

    if (boton) {
        boton.setAttribute("aria-expanded", "false");
    }
}


// ==========================================
// ACTUALIZAR ICONOS LUCIDE
// ==========================================

function actualizarIconos() {

    if (typeof lucide !== "undefined") {
        lucide.createIcons();
    }
}


// ==========================================
// MENÚ HAMBURGUESA
// ==========================================

function toggleSidebar() {

    document.body.classList.toggle("sidebar-abierto");

    let overlay = document.querySelector(".sidebar-overlay");

    if (!overlay) {

        overlay = document.createElement("div");
        overlay.className = "sidebar-overlay";

        overlay.addEventListener("click", function () {
            cerrarSidebar();
        });

        document.body.appendChild(overlay);
    }
}


function cerrarSidebar() {

    document.body.classList.remove("sidebar-abierto");
}


// ==========================================
// CERRAR AL HACER CLIC FUERA
// ==========================================

document.addEventListener("click", function (event) {

    const burbuja = document.getElementById("burbujaNotificaciones");
    const boton = document.querySelector(".boton-campana-amarilla");

    if (
        burbuja &&
        !burbuja.hidden &&
        !burbuja.contains(event.target) &&
        boton &&
        !boton.contains(event.target)
    ) {
        cerrarNotificaciones();
    }

});


// ==========================================
// CERRAR CON ESCAPE
// ==========================================

document.addEventListener("keydown", function (event) {

    if (event.key === "Escape") {
        cerrarNotificaciones();
        cerrarSidebar();
    }

});


// ==========================================
// AJUSTAR AL CAMBIAR TAMAÑO DE PANTALLA
// ==========================================

window.addEventListener("resize", function () {

    if (window.innerWidth > 1024) {
        cerrarSidebar();
    }

});
