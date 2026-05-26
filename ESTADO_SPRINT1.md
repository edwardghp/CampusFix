# Estado del proyecto — Esqueleto Sprint 1

Este repositorio es el **punto de partida** del Sprint 1 de CampusFix.
El proyecto COMPILA y se puede ejecutar. Las historias se desarrollan por equipo.

## Estado de cada Historia de Usuario

| HU | Descripcion | Estado | Encargado |
|----|-------------|--------|-----------|
| HU01 | Registro e inicio de sesion por roles | Desarrollada (referencia) | Edward |
| HU02 | Perfil de usuario y seleccion de rol | Desarrollada (referencia) | Edward |
| HU03 | Identificacion del aula por QR | **Pendiente** | Companero A |
| HU04 | Captura de evidencia y envio del ticket | **Pendiente** | Companero B |

## Que esta listo

- Estructura completa del proyecto (Clean Architecture + MVVM).
- Configuracion de Gradle, Hilt, Room, Firebase.
- HU01 y HU02 funcionando: login, registro, perfil.
- Navegacion entre todas las pantallas.
- Capa de datos completa: los 4 repositorios y sus implementaciones ya existen.

## Que falta (lo desarrolla el equipo)

- **HU03** — La pantalla `feature/report/QrScanScreen.kt` esta como
  "En construccion". El encargado debe implementar el escaneo de QR.
  Ya existe `QrScanViewModel.kt` y `AulaRepositoryImpl.kt` como apoyo.

- **HU04** — La pantalla `feature/report/ReportScreen.kt` esta como
  "En construccion". El encargado debe implementar el formulario de reporte.
  Ya existe `ReportViewModel.kt`, `TicketRepositoryImpl.kt`, `TicketSyncWorker.kt`
  y `AudioRecorder.kt` como apoyo.

## Como desarrollar tu HU

1. Abre el archivo de tu pantalla (`QrScanScreen.kt` o `ReportScreen.kt`).
2. Dentro tiene un comentario con los pasos sugeridos para implementarla.
3. Reemplaza la llamada a `EnConstruccionScreen(...)` por tu interfaz real.
4. Reutiliza el ViewModel que ya esta en la misma carpeta.
5. Verifica que el proyecto siga compilando antes de subir tus cambios.

Lee `CONTRIBUTING.md` para el flujo de trabajo con Git (ramas, commits, merge).
