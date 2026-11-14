package com.euroleague.courtside.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.euroleague.courtside.data.repository.InjuryStatus
import java.util.Locale

/**
 * Die 6 offiziell unterstützten Sprachen in EuroLeague Courtside:
 * Englisch (EN), Deutsch (DE), Französisch (FR), Italienisch (IT), Spanisch (ES) und Griechisch (EL).
 */
enum class AppLanguage(
    val code: String,
    val shortLabel: String,
    val nativeName: String,
    val flagEmoji: String
) {
    EN("en", "EN", "English", "🇬🇧"),
    DE("de", "DE", "Deutsch", "🇩🇪"),
    FR("fr", "FR", "Français", "🇫🇷"),
    IT("it", "IT", "Italiano", "🇮🇹"),
    ES("es", "ES", "Español", "🇪🇸"),
    EL("el", "EL", "Ελληνικά", "🇬🇷");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            val normalized = code?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return when {
                normalized.startsWith("en") -> EN
                normalized.startsWith("de") -> DE
                normalized.startsWith("fr") -> FR
                normalized.startsWith("it") -> IT
                normalized.startsWith("es") -> ES
                normalized.startsWith("el") || normalized.startsWith("gr") -> EL
                else -> {
                    val sys = Locale.getDefault().language.lowercase(Locale.ROOT)
                    entries.firstOrNull { sys.startsWith(it.code) } ?: DE
                }
            }
        }
    }
}

/**
 * Vollständiges UI-Wörterbuch für alle Screens, Tabellen, Buttons, Status-Pills,
 * Kontextmenüs und Benachrichtigungen in allen 6 Sprachen.
 */
data class CourtsideStrings(
    val language: AppLanguage,
    // Navigation
    val navMyTeam: String,
    val navMatchday: String,
    val navStandings: String,
    val navAnalytics: String,
    val navSettings: String,
    val liveSimButton: String,
    // Match Center & Live Scores
    val matchCenterTitle: String,
    val searchPlaceholder: String,
    val roundPrefix: String,
    val filterFavorites: String,
    val filterAllGames: String,
    val filterLiveOnly: String = "● Live",
    val liveScoresHeader: String = "LIVE SCORES",
    val liveTickButton: String = "⚡ Live Tick",
    val crunchTimeBadge: String = "🔥 CRUNCH TIME",
    val statusLive: String,
    val statusFinal: String,
    val statusUpcoming: String,
    val shotMade: String,
    val shotMissed: String,
    val playerBoxscoreHeader: String,
    val playerColumn: String,
    val contextExportCalendar: String,
    val contextScheduleTipOff: String,
    val contextOpenTeamStatsSuffix: String,
    // My Team Hub
    val myTeamHubTitle: String,
    val liveSyncBadge: String,
    val clubsSuffix: String,
    val nextTipOffBadge: String,
    val googleCalendarButton: String,
    val seasonIcsButton: String,
    val activeRosterHeader: String,
    val playersWord: String,
    val rosterVerifiedBadge: String,
    val winLetter: String,
    val lossLetter: String,
    val injuryFit: String,
    val injuryQuestionable: String,
    val injuryOut: String,
    // Standings & Playoffs
    val playoffBracketTitle: String,
    val regularSeasonLabel: String,
    val standingsHeader: String,
    val clubColumn: String,
    val winsColumn: String,
    val lossesColumn: String,
    val offRatingColumn: String,
    // Analytics & Radar
    val teamAnalyticsTitle: String,
    val activeBadge: String,
    val compareBadge: String,
    val radarBadge: String,
    val seasonLeadersHeader: String,
    // Settings & Notifications & Language
    val languageSectionTitle: String,
    val languageSectionSubtitle: String,
    val notificationChannelsTitle: String,
    val tipOffChannelTitle: String,
    val tipOffChannelSubtitle: String,
    val scoresChannelTitle: String,
    val scoresChannelSubtitle: String,
    val closeGameChannelTitle: String,
    val closeGameChannelSubtitle: String,
    val livePushPreviewBadge: String,
    val sendTestPushButton: String,
    val testPushSentPrefix: String,
    // App Updates & Google Play Protect
    val updateSectionTitle: String = "App Updates & Play Protect",
    val playProtectBadge: String = "🛡️ Google Play Protect Compliant",
    val autoUpdateToggleTitle: String = "Auto-Check for Updates on Startup",
    val autoUpdateToggleSubtitle: String = "Play Protect-compliant HTTPS check without invasive install permissions",
    val checkForUpdatesButton: String = "🔄 Check for Updates",
    val checkingUpdatesLabel: String = "Checking GitHub Releases for updates…",
    val upToDateLabel: String = "✓ You are on the latest version",
    val updateAvailablePrefix: String = "🚀 Update Available:",
    val downloadInstallUpdateButton: String = "⬇ Download & Install Update",
    val viewReleaseNotesButton: String = "Release Notes",
    val laterButton: String = "Later"
)

object CourtsideTranslations {

    val EN = CourtsideStrings(
        language = AppLanguage.EN,
        navMyTeam = "My Team",
        navMatchday = "EuroLeague",
        navStandings = "Standings",
        navAnalytics = "Analytics",
        navSettings = "Settings",
        liveSimButton = "⚡ Live Sync",
        matchCenterTitle = "EuroLeague",
        searchPlaceholder = "Search",
        roundPrefix = "Round",
        filterFavorites = "★ Favorites",
        filterAllGames = "All Games",
        filterLiveOnly = "● Live",
        liveScoresHeader = "LIVE SCORES",
        liveTickButton = "⚡ Live Tick",
        crunchTimeBadge = "🔥 CRUNCH TIME",
        statusLive = "LIVE",
        statusFinal = "FINAL",
        statusUpcoming = "UPCOMING",
        shotMade = "Made",
        shotMissed = "Missed",
        playerBoxscoreHeader = "Player boxscore",
        playerColumn = "Player",
        contextExportCalendar = "Export to Google Calendar",
        contextScheduleTipOff = "Schedule 15-min Tip-Off Alert",
        contextOpenTeamStatsSuffix = "Open Team Analytics",
        myTeamHubTitle = "My Team Hub",
        liveSyncBadge = "LIVE SYNC",
        clubsSuffix = "Clubs",
        nextTipOffBadge = "NEXT TIP-OFF",
        googleCalendarButton = "Google Calendar",
        seasonIcsButton = "Season .ics",
        activeRosterHeader = "Active Club Roster",
        playersWord = "Players",
        rosterVerifiedBadge = "✓ Roster & Injury Report verified",
        winLetter = "W",
        lossLetter = "L",
        injuryFit = "Available",
        injuryQuestionable = "Questionable (GTD)",
        injuryOut = "Out (Injured)",
        playoffBracketTitle = "Playoff & Play-In Bracket",
        regularSeasonLabel = "REGULAR SEASON",
        standingsHeader = "EuroLeague Standings (1–6 Playoffs • 7–10 Play-In)",
        clubColumn = "Club",
        winsColumn = "W",
        lossesColumn = "L",
        offRatingColumn = "OFF",
        teamAnalyticsTitle = "Team Analytics Comparison",
        activeBadge = "ACTIVE",
        compareBadge = "COMPARE",
        radarBadge = "RADAR",
        seasonLeadersHeader = "Season MVP & PIR Leaders",
        languageSectionTitle = "App Language (6 Languages)",
        languageSectionSubtitle = "Instant switch between English, German, French, Italian, Spanish & Greek",
        notificationChannelsTitle = "Notification Channels",
        tipOffChannelTitle = "Tip-Off Alert",
        tipOffChannelSubtitle = "15 minutes before tip-off including arena & H2H record",
        scoresChannelTitle = "Halftime & Final Score",
        scoresChannelSubtitle = "Push update with quarter scores & top PIR performer",
        closeGameChannelTitle = "Close-Game Alert (Q4 < 3:00)",
        closeGameChannelSubtitle = "Instant crunch-time alert when point difference is <= 5",
        livePushPreviewBadge = "LIVE PUSH PREVIEW",
        sendTestPushButton = "Send Test Push Notification",
        testPushSentPrefix = "System notification sent:",
        updateSectionTitle = "App Updates & Play Protect",
        playProtectBadge = "🛡️ Google Play Protect Compliant",
        autoUpdateToggleTitle = "Auto-Check for Updates on Startup",
        autoUpdateToggleSubtitle = "Play Protect-compliant HTTPS release check with explicit user consent",
        checkForUpdatesButton = "🔄 Check for Updates",
        checkingUpdatesLabel = "Checking GitHub Releases for updates…",
        upToDateLabel = "✓ You are on the latest version",
        updateAvailablePrefix = "🚀 Update Available:",
        downloadInstallUpdateButton = "⬇ Download & Install Update",
        viewReleaseNotesButton = "Release Notes",
        laterButton = "Later"
    )

    val DE = CourtsideStrings(
        language = AppLanguage.DE,
        navMyTeam = "Mein Team",
        navMatchday = "Spieltag",
        navStandings = "Tabelle",
        navAnalytics = "Statistiken",
        navSettings = "Einstellungen",
        liveSimButton = "⚡ Live-Sync",
        matchCenterTitle = "EuroLeague",
        searchPlaceholder = "Suche",
        roundPrefix = "Spieltag",
        filterFavorites = "★ Favoriten",
        filterAllGames = "Alle Spiele",
        filterLiveOnly = "● Live",
        liveScoresHeader = "LIVE-SPIELSTÄNDE",
        liveTickButton = "⚡ Live-Ticker",
        crunchTimeBadge = "🔥 CRUNCH TIME",
        statusLive = "LIVE",
        statusFinal = "ENDE",
        statusUpcoming = "GEPLANT",
        shotMade = "Treffer",
        shotMissed = "Fehlwurf",
        playerBoxscoreHeader = "Spieler-Boxscore",
        playerColumn = "Spieler",
        contextExportCalendar = "In Google Kalender exportieren",
        contextScheduleTipOff = "15-Min. Tip-Off-Alarm planen",
        contextOpenTeamStatsSuffix = "Team-Statistiken öffnen",
        myTeamHubTitle = "Mein Team Hub",
        liveSyncBadge = "LIVE SYNC",
        clubsSuffix = "Clubs",
        nextTipOffBadge = "NÄCHSTER TIP-OFF",
        googleCalendarButton = "Google Kalender",
        seasonIcsButton = "Saison .ics",
        activeRosterHeader = "Aktueller Kader",
        playersWord = "Spieler",
        rosterVerifiedBadge = "✓ Kader & Injury-Report geprüft",
        winLetter = "S",
        lossLetter = "N",
        injuryFit = "Einsatzbereit",
        injuryQuestionable = "Fraglich (GTD)",
        injuryOut = "Verletzt",
        playoffBracketTitle = "Playoff & Play-In Turnierbaum",
        regularSeasonLabel = "HAUPTRUNDE",
        standingsHeader = "EuroLeague Tabelle (1–6 Playoffs • 7–10 Play-In)",
        clubColumn = "Verein",
        winsColumn = "S",
        lossesColumn = "N",
        offRatingColumn = "OFF",
        teamAnalyticsTitle = "Team-Vergleich & Radar",
        activeBadge = "AKTIV",
        compareBadge = "VERGLEICH",
        radarBadge = "RADAR",
        seasonLeadersHeader = "Saison MVP & PIR-Leader",
        languageSectionTitle = "App-Sprache (6 Sprachen)",
        languageSectionSubtitle = "Sofortiger Wechsel zwischen Englisch, Deutsch, Französisch, Italienisch, Spanisch & Griechisch",
        notificationChannelsTitle = "Benachrichtigungs-Kanäle",
        tipOffChannelTitle = "Tip-Off-Alarm",
        tipOffChannelSubtitle = "15 Minuten vor Spielbeginn inkl. Arena & H2H",
        scoresChannelTitle = "Halbzeit- & Endergebnis",
        scoresChannelSubtitle = "Push-Update mit Viertelständen & Top-PIR-Performer",
        closeGameChannelTitle = "Close-Game-Alert (Q4 < 3:00)",
        closeGameChannelSubtitle = "Sofort-Alarm bei <= 5 Punkten Differenz in der Schlussphase",
        livePushPreviewBadge = "LIVE PUSH VORSCHAU",
        sendTestPushButton = "Test-Push-Benachrichtigung senden",
        testPushSentPrefix = "System-Benachrichtigung gesendet:",
        updateSectionTitle = "App-Updates & Play Protect",
        playProtectBadge = "🛡️ Google Play Protect Konform",
        autoUpdateToggleTitle = "Automatisch beim Start nach Updates suchen",
        autoUpdateToggleSubtitle = "Play-Protect-konforme HTTPS-Prüfung ohne invasive Installationsrechte",
        checkForUpdatesButton = "🔄 Nach Updates suchen",
        checkingUpdatesLabel = "Prüfe GitHub Releases auf neue Version…",
        upToDateLabel = "✓ App ist auf dem neuesten Stand",
        updateAvailablePrefix = "🚀 Neues Update verfügbar:",
        downloadInstallUpdateButton = "⬇ Update herunterladen & installieren",
        viewReleaseNotesButton = "Release Notes",
        laterButton = "Später"
    )

    val FR = CourtsideStrings(
        language = AppLanguage.FR,
        navMyTeam = "Mon Équipe",
        navMatchday = "Journée",
        navStandings = "Classement",
        navAnalytics = "Statistiques",
        navSettings = "Paramètres",
        liveSimButton = "⚡ Sync Direct",
        matchCenterTitle = "EuroLeague",
        searchPlaceholder = "Rechercher",
        roundPrefix = "Journée",
        filterFavorites = "★ Favoris",
        filterAllGames = "Tous les matchs",
        filterLiveOnly = "● Direct",
        liveScoresHeader = "SCORES EN DIRECT",
        liveTickButton = "⚡ Direct",
        crunchTimeBadge = "🔥 MONEY TIME",
        statusLive = "DIRECT",
        statusFinal = "TERMINÉ",
        statusUpcoming = "À VENIR",
        shotMade = "Réussi",
        shotMissed = "Manqué",
        playerBoxscoreHeader = "Stats des joueurs",
        playerColumn = "Joueur",
        contextExportCalendar = "Exporter vers Google Agenda",
        contextScheduleTipOff = "Alerte coup d'envoi (15 min)",
        contextOpenTeamStatsSuffix = "Ouvrir les stats d'équipe",
        myTeamHubTitle = "Mon Équipe Hub",
        liveSyncBadge = "SYNC DIRECT",
        clubsSuffix = "Clubs",
        nextTipOffBadge = "PROCHAIN MATCH",
        googleCalendarButton = "Google Agenda",
        seasonIcsButton = "Saison .ics",
        activeRosterHeader = "Effectif Officiel",
        playersWord = "Joueurs",
        rosterVerifiedBadge = "✓ Effectif & Blessures vérifiés",
        winLetter = "V",
        lossLetter = "D",
        injuryFit = "Disponible",
        injuryQuestionable = "Incertain (GTD)",
        injuryOut = "Blessé (Out)",
        playoffBracketTitle = "Tableau Playoffs & Play-In",
        regularSeasonLabel = "SAISON RÉGULIÈRE",
        standingsHeader = "Classement EuroLeague (1–6 Playoffs • 7–10 Play-In)",
        clubColumn = "Club",
        winsColumn = "V",
        lossesColumn = "D",
        offRatingColumn = "OFF",
        teamAnalyticsTitle = "Comparaison Radar des Équipes",
        activeBadge = "ACTIF",
        compareBadge = "COMPARER",
        radarBadge = "RADAR",
        seasonLeadersHeader = "Leaders MVP & PIR de la Saison",
        languageSectionTitle = "Langue de l'application (6 langues)",
        languageSectionSubtitle = "Changement instantané : Anglais, Allemand, Français, Italien, Espagnol & Grec",
        notificationChannelsTitle = "Canaux de Notification",
        tipOffChannelTitle = "Alerte Coup d'envoi",
        tipOffChannelSubtitle = "15 minutes avant le match avec salle et face-à-face",
        scoresChannelTitle = "Score Mi-temps & Final",
        scoresChannelSubtitle = "Score par quart-temps et meilleur joueur à l'évaluation PIR",
        closeGameChannelTitle = "Alerte Fin de Match Serrée (Q4 < 3:00)",
        closeGameChannelSubtitle = "Notification immédiate si l'écart est <= 5 points dans le money-time",
        livePushPreviewBadge = "APERÇU NOTIFICATION",
        sendTestPushButton = "Envoyer une notification test",
        testPushSentPrefix = "Notification système envoyée :",
        updateSectionTitle = "Mises à jour & Play Protect",
        playProtectBadge = "🛡️ Conforme Google Play Protect",
        autoUpdateToggleTitle = "Vérification auto des mises à jour au démarrage",
        autoUpdateToggleSubtitle = "Vérification HTTPS conforme Play Protect avec consentement explicite",
        checkForUpdatesButton = "🔄 Vérifier les mises à jour",
        checkingUpdatesLabel = "Recherche de mises à jour sur GitHub Releases…",
        upToDateLabel = "✓ Vous utilisez la dernière version",
        updateAvailablePrefix = "🚀 Mise à jour disponible :",
        downloadInstallUpdateButton = "⬇ Télécharger et installer",
        viewReleaseNotesButton = "Notes de version",
        laterButton = "Plus tard"
    )

    val IT = CourtsideStrings(
        language = AppLanguage.IT,
        navMyTeam = "La Mia Squadra",
        navMatchday = "Giornata",
        navStandings = "Classifica",
        navAnalytics = "Statistiche",
        navSettings = "Impostazioni",
        liveSimButton = "⚡ Sync Live",
        matchCenterTitle = "EuroLeague",
        searchPlaceholder = "Cerca",
        roundPrefix = "Giornata",
        filterFavorites = "★ Preferiti",
        filterAllGames = "Tutte le partite",
        filterLiveOnly = "● Live",
        liveScoresHeader = "RISULTATI LIVE",
        liveTickButton = "⚡ Live Tick",
        crunchTimeBadge = "🔥 CRUNCH TIME",
        statusLive = "LIVE",
        statusFinal = "FINALE",
        statusUpcoming = "IN PROGRAMMA",
        shotMade = "Segnato",
        shotMissed = "Sbagliato",
        playerBoxscoreHeader = "Tabellino giocatori",
        playerColumn = "Giocatore",
        contextExportCalendar = "Esporta in Google Calendar",
        contextScheduleTipOff = "Promemoria palla a due (15 min)",
        contextOpenTeamStatsSuffix = "Apri statistiche squadra",
        myTeamHubTitle = "Hub Squadra",
        liveSyncBadge = "LIVE SYNC",
        clubsSuffix = "Club",
        nextTipOffBadge = "PROSSIMA PALLA A DUE",
        googleCalendarButton = "Google Calendar",
        seasonIcsButton = "Stagione .ics",
        activeRosterHeader = "Roster Ufficiale",
        playersWord = "Giocatori",
        rosterVerifiedBadge = "✓ Roster & Infortuni verificati",
        winLetter = "V",
        lossLetter = "P",
        injuryFit = "Disponibile",
        injuryQuestionable = "In dubbio (GTD)",
        injuryOut = "Infortunato",
        playoffBracketTitle = "Tabellone Playoff & Play-In",
        regularSeasonLabel = "STAGIONE REGOLARE",
        standingsHeader = "Classifica EuroLeague (1–6 Playoff • 7–10 Play-In)",
        clubColumn = "Club",
        winsColumn = "V",
        lossesColumn = "P",
        offRatingColumn = "OFF",
        teamAnalyticsTitle = "Confronto Radar Squadre",
        activeBadge = "ATTIVO",
        compareBadge = "CONFRONTA",
        radarBadge = "RADAR",
        seasonLeadersHeader = "Leader Stagionali MVP & PIR",
        languageSectionTitle = "Lingua dell'app (6 lingue)",
        languageSectionSubtitle = "Cambio istantaneo tra Inglese, Tedesco, Francese, Italiano, Spagnolo e Greco",
        notificationChannelsTitle = "Canali di Notifica",
        tipOffChannelTitle = "Avviso Palla a Due",
        tipOffChannelSubtitle = "15 minuti prima dell'inizio con arena e precedenti H2H",
        scoresChannelTitle = "Risultato Intervallo & Finale",
        scoresChannelSubtitle = "Parziali dei quarti e miglior giocatore per PIR",
        closeGameChannelTitle = "Avviso Partita Punto a Punto (Q4 < 3:00)",
        closeGameChannelSubtitle = "Notifica immediata con scarto <= 5 punti negli ultimi 3 minuti",
        livePushPreviewBadge = "ANTEPRIMA NOTIFICA",
        sendTestPushButton = "Invia notifica push di prova",
        testPushSentPrefix = "Notifica di sistema inviata:",
        updateSectionTitle = "Aggiornamenti App & Play Protect",
        playProtectBadge = "🛡️ Conforme a Google Play Protect",
        autoUpdateToggleTitle = "Controllo automatico aggiornamenti all'avvio",
        autoUpdateToggleSubtitle = "Controllo HTTPS conforme a Play Protect senza permessi invasivi",
        checkForUpdatesButton = "🔄 Controlla aggiornamenti",
        checkingUpdatesLabel = "Controllo aggiornamenti su GitHub Releases…",
        upToDateLabel = "✓ L'app è aggiornata all'ultima versione",
        updateAvailablePrefix = "🚀 Nuovo aggiornamento disponibile:",
        downloadInstallUpdateButton = "⬇ Scarica e installa aggiornamento",
        viewReleaseNotesButton = "Note di rilascio",
        laterButton = "Più tardi"
    )

    val ES = CourtsideStrings(
        language = AppLanguage.ES,
        navMyTeam = "Mi Equipo",
        navMatchday = "Jornada",
        navStandings = "Clasificación",
        navAnalytics = "Estadísticas",
        navSettings = "Ajustes",
        liveSimButton = "⚡ Sync Vivo",
        matchCenterTitle = "EuroLeague",
        searchPlaceholder = "Buscar",
        roundPrefix = "Jornada",
        filterFavorites = "★ Favoritos",
        filterAllGames = "Todos los partidos",
        filterLiveOnly = "● En Vivo",
        liveScoresHeader = "RESULTADOS EN VIVO",
        liveTickButton = "⚡ Ticker",
        crunchTimeBadge = "🔥 MINUTOS FINALES",
        statusLive = "EN VIVO",
        statusFinal = "FINAL",
        statusUpcoming = "PRÓXIMO",
        shotMade = "Anotado",
        shotMissed = "Fallado",
        playerBoxscoreHeader = "Estadísticas de jugadores",
        playerColumn = "Jugador",
        contextExportCalendar = "Exportar a Google Calendar",
        contextScheduleTipOff = "Programar alerta 15 min antes",
        contextOpenTeamStatsSuffix = "Abrir estadísticas del equipo",
        myTeamHubTitle = "Mi Equipo Hub",
        liveSyncBadge = "SYNC EN VIVO",
        clubsSuffix = "Clubes",
        nextTipOffBadge = "PRÓXIMO SALTO INICIAL",
        googleCalendarButton = "Google Calendar",
        seasonIcsButton = "Temporada .ics",
        activeRosterHeader = "Plantilla Oficial",
        playersWord = "Jugadores",
        rosterVerifiedBadge = "✓ Plantilla y Parte Médico verificados",
        winLetter = "V",
        lossLetter = "D",
        injuryFit = "Disponible",
        injuryQuestionable = "Duda (GTD)",
        injuryOut = "Baja por lesión",
        playoffBracketTitle = "Cuadro de Playoffs y Play-In",
        regularSeasonLabel = "LIGA REGULAR",
        standingsHeader = "Clasificación EuroLeague (1–6 Playoffs • 7–10 Play-In)",
        clubColumn = "Club",
        winsColumn = "V",
        lossesColumn = "D",
        offRatingColumn = "OF",
        teamAnalyticsTitle = "Comparativa Radar de Equipos",
        activeBadge = "ACTIVO",
        compareBadge = "COMPARAR",
        radarBadge = "RADAR",
        seasonLeadersHeader = "Líderes MVP y Valoración PIR",
        languageSectionTitle = "Idioma de la aplicación (6 idiomas)",
        languageSectionSubtitle = "Cambio instantáneo entre Inglés, Alemán, Francés, Italiano, Español y Griego",
        notificationChannelsTitle = "Canales de Notificación",
        tipOffChannelTitle = "Alerta de Salto Inicial",
        tipOffChannelSubtitle = "15 minutos antes del partido con pabellón y cara a cara",
        scoresChannelTitle = "Descanso y Resultado Final",
        scoresChannelSubtitle = "Parciales por cuarto y líder en valoración PIR",
        closeGameChannelTitle = "Alerta de Final Igualado (Q4 < 3:00)",
        closeGameChannelSubtitle = "Aviso inmediato cuando la diferencia es <= 5 puntos en el último cuarto",
        livePushPreviewBadge = "VISTA PREVIA PUSH",
        sendTestPushButton = "Enviar notificación push de prueba",
        testPushSentPrefix = "Notificación del sistema enviada:",
        updateSectionTitle = "Actualizaciones y Play Protect",
        playProtectBadge = "🛡️ Compatible con Google Play Protect",
        autoUpdateToggleTitle = "Buscar actualizaciones automáticamente al iniciar",
        autoUpdateToggleSubtitle = "Verificación HTTPS compatible con Play Protect sin permisos invasivos",
        checkForUpdatesButton = "🔄 Buscar actualizaciones",
        checkingUpdatesLabel = "Buscando actualizaciones en GitHub Releases…",
        upToDateLabel = "✓ Tienes la última versión instalada",
        updateAvailablePrefix = "🚀 Nueva actualización disponible:",
        downloadInstallUpdateButton = "⬇ Descargar e instalar actualización",
        viewReleaseNotesButton = "Notas de la versión",
        laterButton = "Más tarde"
    )

    val EL = CourtsideStrings(
        language = AppLanguage.EL,
        navMyTeam = "Η Ομάδα Μου",
        navMatchday = "Αγωνιστική",
        navStandings = "Βαθμολογία",
        navAnalytics = "Στατιστικά",
        navSettings = "Ρυθμίσεις",
        liveSimButton = "⚡ Live Ανανέωση",
        matchCenterTitle = "EuroLeague",
        searchPlaceholder = "Αναζήτηση",
        roundPrefix = "Αγωνιστική",
        filterFavorites = "★ Αγαπημένα",
        filterAllGames = "Όλοι οι αγώνες",
        filterLiveOnly = "● Ζωντανά",
        liveScoresHeader = "ΖΩΝΤΑΝΑ ΣΚΟΡ",
        liveTickButton = "⚡ Live Tick",
        crunchTimeBadge = "🔥 ΦΙΝΑΛΕ ΘΡΙΛΕΡ",
        statusLive = "ΖΩΝΤΑΝΑ",
        statusFinal = "ΤΕΛΙΚΟ",
        statusUpcoming = "ΠΡΟΓΡΑΜΜΑ",
        shotMade = "Εύστοχο",
        shotMissed = "Άστοχο",
        playerBoxscoreHeader = "Στατιστικά παικτών",
        playerColumn = "Παίκτης",
        contextExportCalendar = "Εξαγωγή στο Ημερολόγιο Google",
        contextScheduleTipOff = "Ειδοποίηση τζάμπολ (15 λεπτά πριν)",
        contextOpenTeamStatsSuffix = "Άνοιγμα στατιστικών ομάδας",
        myTeamHubTitle = "Κέντρο Ομάδας",
        liveSyncBadge = "LIVE ΣΥΓΧΡΟΝΙΣΜΟΣ",
        clubsSuffix = "Ομάδες",
        nextTipOffBadge = "ΕΠΟΜΕΝΟ ΤΖΑΜΠΟΛ",
        googleCalendarButton = "Google Calendar",
        seasonIcsButton = "Σεζόν .ics",
        activeRosterHeader = "Επίσημο Ρόστερ",
        playersWord = "Παίκτες",
        rosterVerifiedBadge = "✓ Επαληθευμένο Ρόστερ & Ιατρικό Δελτίο",
        winLetter = "Ν",
        lossLetter = "Η",
        injuryFit = "Διαθέσιμος",
        injuryQuestionable = "Αμφίβολος (GTD)",
        injuryOut = "Τραυματίας (Εκτός)",
        playoffBracketTitle = "Πλέι-οφ & Play-In Πίνακας",
        regularSeasonLabel = "ΚΑΝΟΝΙΚΗ ΠΕΡΙΟΔΟΣ",
        standingsHeader = "Βαθμολογία EuroLeague (1–6 Playoffs • 7–10 Play-In)",
        clubColumn = "Ομάδα",
        winsColumn = "Ν",
        lossesColumn = "Η",
        offRatingColumn = "ΕΠΙΘ",
        teamAnalyticsTitle = "Σύγκριση Ομάδων (Radar)",
        activeBadge = "ΕΝΕΡΓΟ",
        compareBadge = "ΣΥΓΚΡΙΣΗ",
        radarBadge = "RADAR",
        seasonLeadersHeader = "Κορυφαίοι MVP & PIR Σεζόν",
        languageSectionTitle = "Γλώσσα Εφαρμογής (6 Γλώσσες)",
        languageSectionSubtitle = "Άμεση εναλλαγή: Αγγλικά, Γερμανικά, Γαλλικά, Ιταλικά, Ισπανικά & Ελληνικά",
        notificationChannelsTitle = "Κανάλια Ειδοποιήσεων",
        tipOffChannelTitle = "Ειδοποίηση Τζάμπολ",
        tipOffChannelSubtitle = "15 λεπτά πριν την έναρξη με γήπεδο & προϊστορία",
        scoresChannelTitle = "Ημίχρονο & Τελικό Σκορ",
        scoresChannelSubtitle = "Ενημέρωση ανά δεκάλεπτο & κορυφαίος παίκτης PIR",
        closeGameChannelTitle = "Ειδοποίηση Ντέρμπι (Q4 < 3:00)",
        closeGameChannelSubtitle = "Άμεση ειδοποίηση όταν η διαφορά είναι <= 5 πόντοι στο φινάλε",
        livePushPreviewBadge = "ΠΡΟΕΠΙΣΚΟΠΗΣΗ ΕΙΔΟΠΟΙΗΣΗΣ",
        sendTestPushButton = "Αποστολή Δοκιμαστικής Ειδοποίησης Push",
        testPushSentPrefix = "Εστάλη ειδοποίηση συστήματος:",
        updateSectionTitle = "Ενημερώσεις Εφαρμογής & Play Protect",
        playProtectBadge = "🛡️ Συμβατό με Google Play Protect",
        autoUpdateToggleTitle = "Αυτόματος έλεγχος ενημερώσεων κατά την εκκίνηση",
        autoUpdateToggleSubtitle = "Έλεγχος HTTPS συμβατός με το Play Protect χωρίς επεμβατικές άδειες",
        checkForUpdatesButton = "🔄 Έλεγχος για ενημερώσεις",
        checkingUpdatesLabel = "Έλεγχος στο GitHub Releases για νέα έκδοση…",
        upToDateLabel = "✓ Έχετε την πιο πρόσφατη έκδοση",
        updateAvailablePrefix = "🚀 Νέα ενημέρωση διαθέσιμη:",
        downloadInstallUpdateButton = "⬇ Λήψη & Εγκατάσταση ενημέρωσης",
        viewReleaseNotesButton = "Σημειώσεις έκδοσης",
        laterButton = "Αργότερα"
    )

    fun forLanguage(language: AppLanguage): CourtsideStrings = when (language) {
        AppLanguage.EN -> EN
        AppLanguage.DE -> DE
        AppLanguage.FR -> FR
        AppLanguage.IT -> IT
        AppLanguage.ES -> ES
        AppLanguage.EL -> EL
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.DE }
val LocalCourtsideStrings = staticCompositionLocalOf { CourtsideTranslations.DE }

fun InjuryStatus.localizedLabel(strings: CourtsideStrings): String = when (this) {
    InjuryStatus.FIT -> strings.injuryFit
    InjuryStatus.GAME_TIME_DECISION, InjuryStatus.QUESTIONABLE -> strings.injuryQuestionable
    InjuryStatus.OUT -> strings.injuryOut
}

/**
 * Formatiert die verbleibende Zeit bis zum Tip-Off lesbar in Tagen & Stunden
 * (z. B. "3 Tage 15 Stunden" statt "87:12:04") in allen 6 unterstützten Sprachen.
 */
fun CourtsideStrings.formatTipOffCountdown(remainingMillis: Long): String {
    val safeMillis = remainingMillis.coerceAtLeast(0L)
    val totalMinutes = safeMillis / 60_000L
    val totalHours = totalMinutes / 60L
    val days = totalHours / 24L
    val hours = totalHours % 24L
    val minutes = totalMinutes % 60L

    return when (language) {
        AppLanguage.DE -> when {
            days > 0L -> {
                val dLabel = if (days == 1L) "1 Tag" else "$days Tage"
                val hLabel = if (hours == 1L) "1 Stunde" else "$hours Stunden"
                "$dLabel $hLabel"
            }
            hours > 0L -> {
                val hLabel = if (hours == 1L) "1 Stunde" else "$hours Stunden"
                "$hLabel $minutes Min."
            }
            else -> "$minutes Min."
        }
        AppLanguage.EN -> when {
            days > 0L -> {
                val dLabel = if (days == 1L) "1 Day" else "$days Days"
                val hLabel = if (hours == 1L) "1 Hour" else "$hours Hours"
                "$dLabel $hLabel"
            }
            hours > 0L -> {
                val hLabel = if (hours == 1L) "1 Hour" else "$hours Hours"
                "$hLabel $minutes Min"
            }
            else -> "$minutes Min"
        }
        AppLanguage.FR -> when {
            days > 0L -> {
                val dLabel = if (days == 1L) "1 Jour" else "$days Jours"
                val hLabel = if (hours == 1L) "1 Heure" else "$hours Heures"
                "$dLabel $hLabel"
            }
            hours > 0L -> {
                val hLabel = if (hours == 1L) "1 Heure" else "$hours Heures"
                "$hLabel $minutes Min"
            }
            else -> "$minutes Min"
        }
        AppLanguage.IT -> when {
            days > 0L -> {
                val dLabel = if (days == 1L) "1 Giorno" else "$days Giorni"
                val hLabel = if (hours == 1L) "1 Ora" else "$hours Ore"
                "$dLabel $hLabel"
            }
            hours > 0L -> {
                val hLabel = if (hours == 1L) "1 Ora" else "$hours Ore"
                "$hLabel $minutes Min"
            }
            else -> "$minutes Min"
        }
        AppLanguage.ES -> when {
            days > 0L -> {
                val dLabel = if (days == 1L) "1 Día" else "$days Días"
                val hLabel = if (hours == 1L) "1 Hora" else "$hours Horas"
                "$dLabel $hLabel"
            }
            hours > 0L -> {
                val hLabel = if (hours == 1L) "1 Hora" else "$hours Horas"
                "$hLabel $minutes Min"
            }
            else -> "$minutes Min"
        }
        AppLanguage.EL -> when {
            days > 0L -> {
                val dLabel = if (days == 1L) "1 Ημέρα" else "$days Ημέρες"
                val hLabel = if (hours == 1L) "1 Ώρα" else "$hours Ώρες"
                "$dLabel $hLabel"
            }
            hours > 0L -> {
                val hLabel = if (hours == 1L) "1 Ώρα" else "$hours Ώρες"
                "$hLabel $minutes Λεπτά"
            }
            else -> "$minutes Λεπτά"
        }
    }
}

