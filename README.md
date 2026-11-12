# Actualización V1.2 BETA 3 — PARTICIPA (12 noviembre 2026)

Esta es la V1.1 BETA 2 con un parche enfocado en portada compacta y accesible.

- **Elimina** las tarjetas principales «TU VOZ EN LA RADIO», «PIDE TU CANCIÓN», «WHATSAPP» y «COMPARTIR».
- Añade **un solo botón flotante «✦ PARTICIPA»**, siempre visible sobre la portada, que abre un panel inferior de Android con 3 acciones grandes: **DEJA TU SALUDO**, **PIDE TU CANCIÓN**, **WHATSAPP**. «COMPARTIR LA EMISORA» queda como enlace secundario dentro del panel.
- Conserva **intactos** los tres destinos anteriores, y deja la radio reproduciéndose mientras se usan.
- El área desplazable reserva espacio bajo el contenido para que el botón flotante no tape los controles. En celulares pequeños y con texto grande puede seguir existiendo desplazamiento para accesibilidad.
- Mantiene **sin cambios** `RadioPlaybackService.java`, intro V1.1, logo original, Media3, formulario y calendario.
- Bump de aplicación `versionCode 3`, `versionName 1.2.0-beta3`.
- **Pendiente:** compilar con GitHub Actions e instalar en un Android real antes de anunciarla pública. No subir un APK debug a descargas de oyentes.

## Reemplazar en el repositorio existente

Desde GitHub, actualiza solamente:

1. `app/src/main/java/com/tropicalisima/estereo/MainActivity.java`
2. `app/build.gradle`

El resto permanece como en V1.1 BETA 2. Este archivo ZIP también trae un proyecto completo, por si aún no habías subido la V1.1.

---

# TROPICALISIMA ESTÉREO — ANDROID V1

**La que te pone a gozar** · Proyecto nativo Android, independiente de la web oficial.

> **Estado: fuente pre-beta, estructura y recursos verificados. No se ha compilado ni instalado en Android en este entorno. NO es una APK final firmada ni publicada.**

## Lo que incluye
- Reproductor Android **Media3 / MediaSessionService**, diseñado para seguir sonando al bloquear el teléfono y mostrar controles de reproducción del sistema.
- Inicio manual (sin autoplay), volumen interno inicial al **50 %**, botones grandes de subir/bajar y barra deslizable.
- Intro «El Despertar Tropical» con el **logotipo original**; se puede omitir. Animaciones ligeras, sin sonidos inesperados.
- Identidad «Neón Tropical Premium», colores luminosos, alto contraste, control de movimiento.
- Calendario de **12 ambientes y 25 ocasiones especiales** calculadas localmente (hora de Colombia), con cambios de paleta y nombre; los temas principales añaden pequeños motivos luminosos discretos. Sin interruptor de temas: el calendario es parte de la identidad editorial de la emisora.
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

## V1.1 beta2 — Portada compacta e intro mejorada

- Los ambientes son automáticos y no negociables para el oyente; se eliminó el interruptor.
- Se retiró la tarjeta de explicación «TROPICALISIMA VIVA» de la portada.
- Portada compacta con logo, reproductor, volumen y cuatro acciones principales.
- Las pantallas cortas o con letra accesible todavía permiten desplazamiento para no ocultar controles.
- Intro con luces de cabina, halo tropical y revelación del logotipo; sin reproducción automática de audio.
- El servicio de audio Media3 no cambió.
- El flujo de GitHub Actions incluye la corrección de Android SDK v4.
- Esta entrega es **código para compilar y probar**, no una APK firmada ni verificada en un dispositivo.
