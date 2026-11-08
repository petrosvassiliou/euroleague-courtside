# 🏀 EuroLeague Courtside

**EuroLeague Courtside** ist eine vollständig native Kotlin- & Jetpack-Compose-App (Material 3 Adaptive) im modernen **Courtside UI Broadcast-Design** für **Googlebook OS (Desktop/Laptop)**, **Android-Tablets**, **Foldables (Falt-Handys)** und **Smartphones**.

---

## ✨ Features & Architektur

### 1. Courtside UI Design & Offizielle Vereinswappen/-farben
- **Obsidian Broadcast-Design (`#0E0E11` / `#17171C`)** mit warmem EuroLeague-Orange-Glow (`#FF6600`) und Dual-Tone-Verlaufs-Bannern.
- **Alle 18 EuroLeague-Clubs** mit ihren offiziellen Primär-, Sekundär- und Akzentfarben sowie vektorbasierten Vereinswappen (`CourtsideClubCrest` in `CourtsideTheme.kt`).
- **Touch- & Desktop-Ergonomie:** Garantierte `48.dp` Touch-Targets kombiniert mit Maus-Hover-Effekten (`Modifier.hoverable()`), `PointerIcon.Hand`, Rechtsklick-Kontextmenü (`isSecondaryPressed`) und Hintergrund-Tastatur-Shortcuts (`Ctrl/Cmd + 1–5`, `F`, `R`, `←`/`→`).

### 2. Adaptives Multi-Device-Layout & Foldables
- **Desktop & Expanded Screens (Googlebook OS):** Linke Broadcast-Sidebar (`CourtsideLeftSidebar`) + mehrspaltiges Dual-Pane Split-View (`ListDetailPaneScaffold`).
- **Foldables (`BookPosture` & `TableTopPosture`):** Automatische Scharnier-Erkennung über Jetpack `WindowInfoTracker` (`FoldingFeature`).
- **Smartphones (`COMPACT`):** Abgedunkelte `NavigationBar` am unteren Bildschirmrand.

### 3. Live-Match-Center, PIR-Boxscore & Compose Canvas Charts
- **Split-View Match-Center (`MatchCenterScreen.kt`):**
  - Links: Live-Spielkarten mit `[LIVE | Q4 01:42]`-Badge, Vereinswappen und `Q1 Q2 Q3 Q4 PIR`-Matrix.
  - Rechts (gleichzeitig sichtbar): Dual-Tone-Scoreboard + leuchtendes **Half-Court Shot-Chart** (`Made` in `#00E676`, `Missed` in `#FF3B5C`) + sortierbare **`Player boxscore ⌄`**-Tabelle mit offizieller EuroLeague-**PIR**-Berechnung.
- **Team-Radar-Vergleich:** Nativ mit Compose `Canvas` gezeichnetes 5-Achsen-Radar-Chart (Offensive Rating, Defensive Rating, Pace, 3P%, Rebound-Rate).

### 4. Zwei Native Kalender-Export-Wege (`CalendarExportHelper.kt`)
1. **Google Kalender System-Intent:** `Intent(Intent.ACTION_INSERT)` mit `CalendarContract.Events.CONTENT_URI`, Arena als `EVENT_LOCATION` und 30-Minuten-Reminder.
2. **RFC 5545 `.ics` Saison-Generator:** Erzeugt eine standardkonforme `.ics`-Datei inkl. `VALARM` (`-PT30M`) für alle Saisonspiele des Lieblingsteams und teilt sie über `FileProvider`.

### 5. Natives Benachrichtigungs-System (`EuroLeagueNotificationManager.kt`)
- Dedizierte `NotificationChannel`s für **Tip-Off-Alarme** (15 Min. vor Spielbeginn via `WorkManager`), **Halbzeit-/Endergebnisse** und **Crunch-Time Close-Game-Alerts** (Q4 `< 3:00` Restzeit bei `<= 5` Punkten Differenz).
- Integrierter **Test-Push-Button** im Einstellungs-Tab mit `NotificationCompat.BigTextStyle`.

---

## 🛠️ Tech-Stack
- **UI:** 100 % Kotlin, Jetpack Compose, Material 3 Adaptive (`ListDetailPaneScaffold`), Jetpack WindowManager (`androidx.window:window`)
- **Architektur:** Clean Architecture, MVVM / MVI (`CourtsideViewModel`), Kotlin Coroutines & `StateFlow`, Hilt DI
- **Persistenz & Netzwerk:** Room Database (`CourtsideDatabase`), Jetpack DataStore (`CourtsidePreferencesDataStore`), Retrofit + Kotlinx Serialization (`EuroLeagueApiService`)
- **CI/CD:** GitHub Actions Workflow (`.github/workflows/android-build.yml`) führt bei jedem Push automatisch die Unit-Tests aus und baut das installierbare Debug-APK-Artefakt.
