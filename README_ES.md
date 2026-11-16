# Motoroll Home 2.0 — launcher ligero para Motoroll OS

Rediseño de Motoroll Home con una identidad propia, en negro profundo, azul eléctrico y violeta. Está pensado para el Motorola Edge 70 Fusion (`marvel`), Android 16 / API 36, Hello UI stock, resolución 1272 × 2772 y densidad 450 dpi.

## Diseño

- Escritorio de launcher con reloj y fecha del sistema.
- Tarjeta de identidad Motoroll OS discreta.
- Fondo oscuro estático con dos halos de color dibujados por Canvas.
- Accesos a aplicaciones reales instaladas, usando sus iconos originales.
- Dock inferior con aplicaciones frecuentes y botón del cajón.
- Cajón de aplicaciones con búsqueda local y cuadrícula de cuatro columnas.
- Colores negros, azul eléctrico, cian y violeta; sin imitar One UI ni HyperOS.

## Rendimiento y privacidad

- Java nativo y vistas Android; sin Jetpack Compose, AppCompat ni bibliotecas externas.
- Sin conexión a Internet, anuncios, telemetría ni permisos especiales.
- Sin servicio en segundo plano, widgets de red, fondo animado ni bucles de animación.
- El reloj usa `TextClock`, un componente del sistema.
- Las aplicaciones se consultan localmente mediante PackageManager.
- El launcher Motorola original (`com.motorola.launcher3`) no se elimina ni modifica.

## Compilar con GitHub Actions

El proyecto conserva el flujo `.github/workflows/build-apk.yml` de la versión anterior. Sube el contenido de esta carpeta al mismo repositorio (incluida `.github`, si tu flujo anterior no está ya allí) y ejecuta **Actions → Build Motoroll Home APK → Run workflow**.

El flujo genera `app/build/outputs/apk/debug/app-debug.apk` como artefacto `MotorollHome-debug-apk`.

## Instalar

```bat
adb install -r app-debug.apk
```

Después, en Android, selecciona Ajustes → Aplicaciones → Aplicaciones predeterminadas → Aplicación de inicio → Motoroll Home.

Para volver a Hello UI, selecciona `com.motorola.launcher3` en ese mismo menú. No desinstales el launcher Motorola.

## Alcance de esta versión

Esta app sustituye solo la pantalla de inicio y el cajón de aplicaciones. No modifica SystemUI, ajustes del sistema, cámara, huella, gestos, Moto Secure ni servicios de Motorola. No requiere root ni Magisk.

## Nota de compilación

Este entorno de trabajo no incluye Android SDK/Gradle para ejecutar una compilación local. El proyecto está preparado para el flujo de GitHub Actions; la compilación real debe confirmarse en la pestaña Actions antes de instalar el APK.
