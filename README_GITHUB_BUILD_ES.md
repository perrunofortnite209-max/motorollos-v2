# Compilar Motoroll Home 2.0 en GitHub

1. Sube el contenido de esta carpeta al repositorio donde compilaste Motoroll Home 1.0.
2. Confirma que `.github/workflows/build-apk.yml` siga presente. Si el repositorio ya tiene el flujo que compiló 1.0, puedes conservarlo sin cambios.
3. Abre la pestaña **Actions**.
4. Selecciona **Build Motoroll Home APK** y pulsa **Run workflow**.
5. Cuando termine con una marca verde, abre la ejecución y descarga el artefacto **MotorollHome-debug-apk**.
6. Extrae `app-debug.apk` e instálalo con `adb install -r app-debug.apk`.

Si GitHub no permite subir `.github` desde el selector de archivos, no la renombres. Conserva el workflow que ya funciona en tu repositorio.

El identificador de paquete sigue siendo `com.motoroll.home`, así que `adb install -r` actualiza la app existente sin borrar sus datos privados de aplicación. El launcher Motorola continúa instalado como respaldo.
