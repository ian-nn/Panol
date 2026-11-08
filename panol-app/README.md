# Pañol-App

App Android nativa (Kotlin + Jetpack Compose) para la gestión y trazabilidad de tablets corporativas
mediante códigos QR. TP N° 2 de Desarrollo de Sistemas — E.T. N° 1 "Otto Krause".

- **Documentación:** `docs/Carpeta_Tecnica.md` (casos de uso, reglas, diagramas, almacenamiento, pruebas).
- **QR de prueba:** `docs/qr_demo/`.

## Generar `panol_app.apk`

Hace falta JDK 17 y el Android SDK (viene con Android Studio).

```bash
./gradlew testDebugUnitTest     # pruebas de reglas de negocio y lector de QR
./gradlew assembleDebug         # genera app/build/outputs/apk/debug/panol_app-debug.apk
```

Sin instalar nada: subir este proyecto a GitHub; la pestaña *Actions* compila y publica el APK
(`.github/workflows/build-apk.yml`).

El APK de debug viene firmado con la clave de debug y se puede instalar directamente en cualquier
Android 8.0+ (habilitando "orígenes desconocidos"). El build *release* usa la misma clave solo para el TP;
para publicar de verdad hay que usar un keystore propio.

## Estructura

```
app/src/main/java/com/panolapp/
  domain/   reglas de negocio, lector/generador de QR, modelos   (Kotlin puro)
  data/     Room: entidades, DAO, base y repositorio transaccional
  ui/       Compose: tema, componentes, pantallas, ViewModels, cámara, navegación
app/src/test/                pruebas unitarias del dominio
```
