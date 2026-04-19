# Changelog

Todas las versiones notables de **Zone Royale** se documentan en este archivo.

## [1.3.0] — 2026-04-19

### ✨ Nuevas funciones

- **Archivo de configuración**: ahora puedes ajustar el comportamiento de la zona sin recompilar el mod. Edita `config/zoneroyale-common.toml` en tu servidor para cambiar:
  - El intervalo entre reducciones de la zona (por defecto: 30s).
  - El factor de reducción del radio (por defecto: 20% por ciclo).
  - La duración de la transición visual del borde del mundo.
  - Las fórmulas de radio inicial y final según la cantidad de jugadores.
- **Aviso dinámico en el chat**: el mensaje "tienes X segundos para moverte" ahora refleja el tiempo real de transición configurado, en lugar de un valor fijo.

### 🔧 Mejoras

- Reorganización interna del código en módulos con responsabilidades claras (zona, jugadores, mensajes). Sienta las bases para agregar más funciones sin convertir el mod en un desastre.

### 📦 Técnico

- Versión del mod: `1.2.0` → `1.3.0`.
- Requisitos sin cambios: Minecraft 1.20.1, Forge 47.4.0+.

---

## [1.2.0]

### ✨ Nuevas funciones

- **Teletransporte al ganador**: cuando termina la partida, todos los jugadores son teletransportados alrededor del ganador para presenciar la victoria.
- **Muerte sin pantalla de "You Died"**: los jugadores eliminados pasan directamente a modo espectador sin la pantalla de muerte.
- **Ocultar etiquetas de nombre**: durante la partida, los nombres de los jugadores se ocultan para aumentar la tensión.
- **Títulos en pantalla**: los eventos importantes (inicio de partida, victoria) ahora muestran un título animado.
- **Notificaciones mejoradas al reducirse la zona**: mensajes más claros en chat cuando el borde empieza a cerrarse.

### 🐛 Correcciones

- Validador del permiso de operador ahora se aplica correctamente en los comandos.
- Condición de victoria verificada correctamente al final de cada tick.
- Ajuste del radio final de la zona.

---

## [1.0.0]

### ✨ Versión inicial

- Zona dinámica estilo Battle Royale que se reduce con el tiempo.
- Escalado del radio inicial y final según el número de jugadores.
- Teletransporte aleatorio de jugadores en posiciones seguras al iniciar.
- Modo espectador automático al ser eliminado.
- Sistema de comandos `/zoneroyale`:
  - `/zoneroyale start` — inicia la partida.
  - `/zoneroyale stop` — la detiene.
  - `/zoneroyale setcenter <x> <z>` — define el centro de la zona.
  - `/zoneroyale status` — muestra el estado actual.
  - `/zoneroyale help` — lista los comandos.
- Requiere permisos de operador (nivel 2).
