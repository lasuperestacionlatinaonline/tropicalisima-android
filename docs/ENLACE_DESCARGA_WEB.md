# Botón de descarga directa: futuro lanzamiento estable

No publicar la URL hasta tener una APK release firmada, instalada y verificada.

1. Publicar la APK oficial en **GitHub Releases**, dentro de un repositorio público independiente.
2. Adjuntar un archivo con nombre estable `TROPICALISIMA-ESTEREO.apk` al lanzamiento de cada versión.
3. Botón web apuntará a:

   `https://github.com/lasuperestacionlatinaonline/tropicalisima-android/releases/latest/download/TROPICALISIMA-ESTEREO.apk`

   Ese enlace usa un alias de la última versión y descarga el archivo sin que el oyente tenga que navegar por GitHub. Confirmar su funcionamiento antes de publicar.
4. Incluir guía de instalación en la web: el navegador puede solicitar permiso para descargar el APK; Android puede exigir confirmar **Instalar apps desconocidas** desde el navegador elegido. El usuario debe poder negar la instalación y debe mantener Play Protect activado. No podemos suprimir los avisos del sistema.
5. No instalar APK de prueba como versión oficial: **debug no es release firmada para distribución**.
6. El código fuente web oficial V6 permanece intacto hasta la fase de lanzamiento de Android.
