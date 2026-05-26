# Guía de trabajo en equipo — CampusFix

Cómo colaborar en el proyecto sin pisarnos. Léela antes de empezar a programar.

## 1. Configuración inicial (una sola vez)

Después de clonar el repositorio:

1. Pide a Edward el archivo **`google-services.json`** (se comparte por Drive/WhatsApp,
   NO está en el repo) y colócalo en la carpeta `app/`.
2. Saca tu **SHA-1** de debug: en Android Studio, pestaña Gradle (lateral derecha) →
   CampusFix → Tasks → android → ejecuta `signingReport` → copia el `SHA1` de la
   variante `debug`. Envíaselo a Edward para que lo registre en la consola de Firebase
   (sin esto, el login con Google no te funcionará).
3. Sincroniza Gradle y verifica que el proyecto compile.

## 2. Reparto del Sprint 1

| Integrante | Historia | Carpetas / Archivos a su cargo |
|-----------|----------|-------------------------------|
| Edward    | HU01 + HU02 | `feature/auth/`, `feature/profile/`, `AuthRepositoryImpl.kt`, `ProfileRepositoryImpl.kt` |
| Compañero A | HU03 | `feature/report/QrScanScreen.kt`, `QrScanViewModel.kt`, `AulaPickerScreen.kt`, `AulaPickerViewModel.kt`, `AulaRepositoryImpl.kt` |
| Compañero B | HU04 | `feature/report/ReportScreen.kt`, `ReportViewModel.kt`, `TicketRepositoryImpl.kt`, `TicketSyncWorker.kt`, `AudioRecorder.kt` |

### Archivos compartidos — NO modificar sin avisar al grupo

Estos archivos ya vienen completos para las 4 HU del Sprint 1. Tocarlos genera
conflictos de merge. Si necesitas cambiar alguno, coordínalo primero por el chat:

- `ui/navigation/CampusFixNavGraph.kt` y `Routes.kt`
- `di/RepositoryModule.kt` y `di/AppModule.kt`
- `domain/model/Models.kt` y `domain/repository/Repositories.kt`
- `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`

## 3. Regla de oro: nadie programa en `main`

Cada quien trabaja en su propia rama:

- Edward:      `feature/hu01-hu02-auth-perfil`
- Compañero A: `feature/hu03-aula-qr`
- Compañero B: `feature/hu04-ticket-reporte`

Crear tu rama (una vez):

    git checkout -b feature/hu03-aula-qr

## 4. Rutina diaria (ANTES de empezar a programar)

Trae lo último que subieron los demás para no acumular conflictos:

    git checkout main
    git pull origin main
    git checkout tu-rama
    git merge main

## 5. Subir tu trabajo

    git add .
    git commit -m "HU03: pantalla de escaneo QR funcionando"
    git push -u origin tu-rama

Cuando termines tu HU, abre un **Pull Request** en GitHub hacia `main`.
Otro integrante lo revisa y se hace el merge. Así `main` siempre funciona.

## 6. Mensajes de commit

Empieza el mensaje con la HU. Ejemplos:

- `HU01: validación de correo en el registro`
- `HU04: grabación de audio con MediaRecorder`
- `fix HU03: corregir lectura del campo qrCode`

## 7. Si aparece un conflicto de merge

No es grave. Git marca las zonas en conflicto con `<<<<<<<`, `=======`, `>>>>>>>`.
Usa la herramienta visual de Android Studio (botón `Merge`) para elegir qué código
queda. Ante la duda, avisa al grupo antes de resolver.

## Recordatorios

- El `google-services.json` NUNCA se sube al repo (ya está en `.gitignore`).
- No subas la carpeta `/build` ni `.idea` (ya están ignoradas).
- Si cambias algo en un archivo compartido, avisa al equipo en el chat.
