# TROPICALISIMA ESTÉREO — ANDROID V1

**La que te pone a gozar** · Proyecto nativo Android, independiente de la web oficial.

> **Estado: fuente pre-beta, estructura y recursos verificados. No se ha compilado ni instalado en Android en este entorno. NO es una APK final firmada ni publicada.**

## Lo que incluye
- Reproductor Android **Media3 / MediaSessionService**, diseñado para seguir sonando al bloquear el teléfono y mostrar controles de reproducción del sistema.
- Inicio manual (sin autoplay), volumen interno inicial al **50 %**, botones grandes de subir/bajar y barra deslizable.
- Intro «El Despertar Tropical» con el **logotipo original**; se puede omitir. Animaciones ligeras, sin sonidos inesperados.
- Identidad «Neón Tropical Premium», colores luminosos, alto contraste, control de movimiento.
- Calendario de **12 ambientes y 25 ocasiones especiales** calculadas localmente (hora de Colombia), con cambios de paleta y nombre; los temas principales añaden pequeños motivos luminosos discretos. Incluye modo clásico.
- «Tu voz en la radio»: enviar mensajes, consultar los aprobados a través de la **API pública** de la web, sin incluir claves secretas.
- Pedidos de canciones, WhatsApp y compartir la radio.

## Compilar la primera APK (GitHub)
1. Subir el contenido de esta carpeta al repositorio `tropicalisima-android` conservando su estructura.
2. Ir a `Actions` → `Compilar APK Android (pruebas)` → `Run workflow` (también se inicia con cambios en main).
3. Al terminar, descargar el artifact `TROPICALISIMA-ESTEREO-V1-PRUEBAS`.
4. La APK debug es **solo para pruebas**. Se necesita **firma de distribución estable y pruebas en celulares reales** antes de poner un enlace público en la web.

## Requisitos
- Android 8.0+ (API 26) para esta versión.
- Conexión a internet para radio y mensajes; los ambientes decorativos se calculan sin internet.
- Se solicita permiso de servicio de reproducción en primer plano para mantener el audio activo. No se usan ubicación, contactos, cámara ni micrófono.

## Seguridad
- Nunca subir `SUPABASE_SERVICE_ROLE_KEY`, `MURO_ADMIN_CLAVE`, `MURO_HASH_SALT` ni llaves de firma.
- Las funciones de oyentes consultan `https://tropicalisimaonline.vercel.app/api/oyentes` exclusivamente; **no** llevan acceso de administrador.
- Para distribución fuera de Play Store, generar APK **release firmada** con clave protegida, verificar en un equipo Android y publicar en GitHub Releases, no usar debug como producto final.
- Evitar subir la clave de firma a un repositorio público. Guardarla privada y hacer copia de seguridad.
- Los medios de publicación en Vercel **no han sido modificados** en este proyecto.

## Fuentes del código
- Logotipo: archivo original compartido por el propietario.
- Stream oficial: `https://streampanel.online/8631/stream`.
- Datos públicos: `https://tropicalisimaonline.vercel.app/api/oyentes`.

## Próximas verificaciones en Android real
- Reproducción y reconexión por Wi-Fi/datos, bloqueo de pantalla, auriculares/Bluetooth, llamadas/audio focus.
- Envío real de mensajes y moderación desde la web, sin permisos extra.
- Calendario en cambios de mes y años bisiestos, accesibilidad, consumo de batería.
- Firma estable, instalación y actualización de APK.
