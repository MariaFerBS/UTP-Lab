/* ==========================================================
   TOPBAR ADMINISTRADOR
   NOTIFICACIONES + MENÚ HAMBURGUESA
   ========================================================== */

document.addEventListener("DOMContentLoaded", function () {

    // Inicializar iconos Lucide
    if (typeof lucide !== "undefined") {
        lucide.createIcons();
    }

    // Preparar overlay del menú
    crearOverlaySidebar();

});


/* ==========================================================
   NOTIFICACIONES
   ========================================================== */

function toggleNotificaciones(event) {

    if (event) event.stopPropagation();

    const burbuja = document.getElementById("burbujaNotificaciones");
    const boton = document.querySelector(".boton-campana-amarilla");

    if (!burbuja) return;

    // Si está abierto, cerrarlo
    if (!burbuja.hidden) {
        cerrarNotificaciones();
        return;
    }

    // Cerrar sidebar antes de abrir
    cerrarSidebar();

    // Mostrar panel
    burbuja.hidden = false;

    if (boton) {
        boton.setAttribute("aria-expanded", "true");
    }

    if (typeof lucide !== "undefined") {
        lucide.createIcons();
    }
}


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


/* Cerrar notificaciones al hacer clic fuera */

document.addEventListener("click", function (event) {

    const burbuja = document.getElementById("burbujaNotificaciones");
    const boton = document.querySelector(".boton-campana-amarilla");

    if (
        burbuja &&
        !burbuja.hidden &&
        !burbuja.contains(event.target) &&
        (!boton || !boton.contains(event.target))
    ) {
        cerrarNotificaciones();
    }

});


/* ==========================================================
   MENÚ HAMBURGUESA
   ========================================================== */

function crearOverlaySidebar() {

    let overlay = document.querySelector(".sidebar-overlay");

    if (!overlay) {
        overlay = document.createElement("div");
        overlay.className = "sidebar-overlay";
        document.body.appendChild(overlay);
    }

    overlay.onclick = function () {
        cerrarSidebar();
    };
}


function toggleSidebar() {

    cerrarNotificaciones();

    crearOverlaySidebar();

    document.body.classList.toggle("sidebar-abierto");
}


function cerrarSidebar() {

    document.body.classList.remove("sidebar-abierto");
}


/* ==========================================================
   CERRAR CON ESCAPE
   ========================================================== */

document.addEventListener("keydown", function (event) {

    if (event.key === "Escape") {
        cerrarNotificaciones();
        cerrarSidebar();
    }

});


/* ==========================================================
   AL VOLVER A ESCRITORIO
   ========================================================== */

window.addEventListener("resize", function () {

    if (window.innerWidth > 1024) {
        cerrarSidebar();
        cerrarNotificaciones();
    }

});