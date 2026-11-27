import Foundation

public struct CourtsideStrings: Sendable {
    public let language: AppLanguage
    public let navMyTeam: String
    public let navMatchday: String
    public let navStandings: String
    public let navAnalytics: String
    public let navSettings: String
    public let liveSyncButton: String
    public let searchPlaceholder: String
    public let roundPrefix: String
    public let filterFavorites: String
    public let filterAllGames: String
    public let filterLiveOnly: String
    public let statusLive: String
    public let statusFinal: String
    public let statusUpcoming: String
    public let crunchTimeBadge: String
    public let myTeamHubTitle: String
    public let nextTipOffBadge: String
    public let exportAppleCalendarButton: String
    public let exportSeasonIcsButton: String
    public let activeRosterHeader: String
    public let verifyRosterButton: String
    public let injuryFit: String
    public let injuryQuestionable: String
    public let injuryOut: String
    public let shotChartTab: String
    public let momentumTab: String
    public let playByPlayTab: String
    public let clutchFilterLabel: String
    public let playerBoxscoreHeader: String
    public let standingsHeader: String
    public let backToStandingsButton: String
    public let last3GamesHeader: String
    public let next3GamesHeader: String
    public let setAsMyTeamButton: String
    public let seasonLeadersHeader: String
    public let teamComparisonHeader: String
    public let playoffProjectionHeader: String
    public let gameRemindersTitle: String
    public let gameRemindersSubtitle: String
    public let reminderQ1Label: String
    public let reminderHalftimeLabel: String
    public let reminderQ3Label: String
    public let reminderCrunchTimeLabel: String
    public let reminderFinalScoreLabel: String
    public let enableAllRemindersLabel: String
    public let clearRemindersLabel: String
    public let languageSectionTitle: String
    public let themeSectionTitle: String
    public let lightModeLabel: String
    public let darkModeLabel: String
    public let menuBarTrackerTitle: String
    public let menuBarTrackerSubtitle: String
    public let notificationChannelsTitle: String
    public let tipOffChannelTitle: String
    public let scoresChannelTitle: String
    public let closeGameChannelTitle: String
    public let sendTestPushButton: String
    public let updateSectionTitle: String
    public let betaUpdateChannelTitle: String
    public let checkForUpdatesButton: String
    public let upToDateLabel: String
    public let onboardingWelcomeTitle: String
    public let onboardingWelcomeSubtitle: String
    public let onboardingPickClubTitle: String
    public let onboardingAlertsTitle: String
    public let onboardingFinishButton: String
    public let replayOnboardingButton: String
    public let playerProfileTitle: String
    public let seasonAveragesHeader: String
    public let shootingSplitsHeader: String
    public let gameLogHeader: String

    public func localizedReminderTitle(_ type: GameReminderType) -> String {
        switch type {
        case .q1Result: return reminderQ1Label
        case .halftimeResult: return reminderHalftimeLabel
        case .q3Result: return reminderQ3Label
        case .crunchTime: return reminderCrunchTimeLabel
        case .finalScore: return reminderFinalScoreLabel
        }
    }

    public func localizedInjuryLabel(_ status: InjuryStatus) -> String {
        switch status {
        case .fit: return injuryFit
        case .questionable: return injuryQuestionable
        case .out: return injuryOut
        }
    }
}

public enum CourtsideTranslations {
    public static func forLanguage(_ lang: AppLanguage) -> CourtsideStrings {
        switch lang {
        case .en: return en
        case .de: return de
        case .fr: return fr
        case .it: return it
        case .es: return es
        case .el: return el
        }
    }

    public static let en = CourtsideStrings(
        language: .en,
        navMyTeam: "My Team",
        navMatchday: "Match Center",
        navStandings: "Standings",
        navAnalytics: "Analytics",
        navSettings: "Settings",
        liveSyncButton: "⚡ Live Sync",
        searchPlaceholder: "Search clubs or arenas…",
        roundPrefix: "Round",
        filterFavorites: "★ Favorites",
        filterAllGames: "All Games",
        filterLiveOnly: "● Live",
        statusLive: "LIVE",
        statusFinal: "FINAL",
        statusUpcoming: "UPCOMING",
        crunchTimeBadge: "🔥 CRUNCH TIME",
        myTeamHubTitle: "My Team Hub",
        nextTipOffBadge: "NEXT TIP-OFF",
        exportAppleCalendarButton: "Add to Calendar",
        exportSeasonIcsButton: "Export Season .ics",
        activeRosterHeader: "Active Club Roster",
        verifyRosterButton: "✓ Verify Live Roster",
        injuryFit: "Available",
        injuryQuestionable: "Questionable (GTD)",
        injuryOut: "Out (Injured)",
        shotChartTab: "🎯 Shot Chart",
        momentumTab: "📈 Game Flow",
        playByPlayTab: "⚡ Play-by-Play",
        clutchFilterLabel: "🔥 Clutch",
        playerBoxscoreHeader: "Player Boxscore",
        standingsHeader: "EuroLeague Regular Season Standings",
        backToStandingsButton: "← Back to Standings",
        last3GamesHeader: "Last 3 Games",
        next3GamesHeader: "Next 3 Upcoming Games",
        setAsMyTeamButton: "★ Set as My Team",
        seasonLeadersHeader: "EuroLeague Season Leaders",
        teamComparisonHeader: "Head-to-Head Team Comparison",
        playoffProjectionHeader: "Projected Playoff Series (H2H)",
        gameRemindersTitle: "🔔 Game Reminders",
        gameRemindersSubtitle: "macOS notifications for key moments of this game",
        reminderQ1Label: "1st Quarter Result",
        reminderHalftimeLabel: "Halftime Result",
        reminderQ3Label: "3rd Quarter Result",
        reminderCrunchTimeLabel: "Crunch Time (Q4 ≤ 3:00)",
        reminderFinalScoreLabel: "Final Score",
        enableAllRemindersLabel: "Enable All",
        clearRemindersLabel: "Clear All",
        languageSectionTitle: "Language (6 EuroLeague Languages)",
        themeSectionTitle: "Liquid Glass Appearance (Light & Dark)",
        lightModeLabel: "☀️ Daylight Liquid Glass (Default)",
        darkModeLabel: "🌙 Obsidian Liquid Glass",
        menuBarTrackerTitle: "🏀 macOS Menu Bar Live Scoreboard",
        menuBarTrackerSubtitle: "Keep live EuroLeague scores & armed game reminders right in your macOS menu bar",
        notificationChannelsTitle: "macOS Game-Night Notifications",
        tipOffChannelTitle: "Tip-Off Alert (15 min before)",
        scoresChannelTitle: "Quarter, Halftime & Final Scores",
        closeGameChannelTitle: "Crunch-Time Alert (Q4 ≤ 3:00, ≤ 5 pts)",
        sendTestPushButton: "🔔 Send Instant macOS Test Notification",
        updateSectionTitle: "macOS App Updates (GitHub Releases)",
        betaUpdateChannelTitle: "Include Beta & Pre-Release Builds",
        checkForUpdatesButton: "🔄 Check for Updates",
        upToDateLabel: "✓ Up to date",
        onboardingWelcomeTitle: "Welcome to EuroLeague Courtside 2.0 for macOS",
        onboardingWelcomeSubtitle: "Liquid Glass edition • Choose your club, appearance, and live alerts.",
        onboardingPickClubTitle: "1. Choose Your Club (My Team)",
        onboardingAlertsTitle: "2. Menu Bar Live Ticker & Notifications",
        onboardingFinishButton: "Enter Courtside →",
        replayOnboardingButton: "✨ Open Welcome & Club Setup",
        playerProfileTitle: "Player Scouting Card",
        seasonAveragesHeader: "Season Averages",
        shootingSplitsHeader: "Shooting Splits",
        gameLogHeader: "EuroLeague Game Log"
    )

    public static let de = CourtsideStrings(
        language: .de,
        navMyTeam: "Mein Team",
        navMatchday: "Spieltag",
        navStandings: "Tabelle",
        navAnalytics: "Statistiken",
        navSettings: "Einstellungen",
        liveSyncButton: "⚡ Live-Sync",
        searchPlaceholder: "Verein oder Arena suchen…",
        roundPrefix: "Spieltag",
        filterFavorites: "★ Favoriten",
        filterAllGames: "Alle Spiele",
        filterLiveOnly: "● Live",
        statusLive: "LIVE",
        statusFinal: "ENDE",
        statusUpcoming: "GEPLANT",
        crunchTimeBadge: "🔥 CRUNCH-TIME",
        myTeamHubTitle: "Mein Team Zentrale",
        nextTipOffBadge: "NÄCHSTER TIP-OFF",
        exportAppleCalendarButton: "Zum Kalender hinzufügen",
        exportSeasonIcsButton: "Saison .ics Export",
        activeRosterHeader: "Aktiver Kader & Verletztenliste",
        verifyRosterButton: "✓ Kader live prüfen",
        injuryFit: "Einsatzbereit",
        injuryQuestionable: "Fraglich (GTD)",
        injuryOut: "Verletzt (Out)",
        shotChartTab: "🎯 Wurf-Karte",
        momentumTab: "📈 Spielverlauf",
        playByPlayTab: "⚡ Play-by-Play",
        clutchFilterLabel: "🔥 Clutch",
        playerBoxscoreHeader: "Spieler-Boxscore",
        standingsHeader: "EuroLeague Hauptrunden-Tabelle",
        backToStandingsButton: "← Zurück zur Tabelle",
        last3GamesHeader: "Letzte 3 Spiele",
        next3GamesHeader: "Nächste 3 Spiele",
        setAsMyTeamButton: "★ Als Mein Team wählen",
        seasonLeadersHeader: "EuroLeague Saison-Spitzenreiter",
        teamComparisonHeader: "Direkter Team-Vergleich",
        playoffProjectionHeader: "Playoff-Projektion (Direkter Vergleich)",
        gameRemindersTitle: "🔔 Spiel-Erinnerungen",
        gameRemindersSubtitle: "macOS-Mitteilungen für wichtige Momente dieses Spiels",
        reminderQ1Label: "1. Viertel Ergebnis",
        reminderHalftimeLabel: "Halbzeit-Ergebnis",
        reminderQ3Label: "3. Viertel Ergebnis",
        reminderCrunchTimeLabel: "Crunch-Time (Q4 ≤ 3:00)",
        reminderFinalScoreLabel: "Endergebnis",
        enableAllRemindersLabel: "Alle aktivieren",
        clearRemindersLabel: "Alle löschen",
        languageSectionTitle: "Sprache (6 EuroLeague-Sprachen)",
        themeSectionTitle: "Liquid Glass Darstellung (Hell & Dunkel)",
        lightModeLabel: "☀️ Daylight Liquid Glass (Standard)",
        darkModeLabel: "🌙 Obsidian Liquid Glass",
        menuBarTrackerTitle: "🏀 macOS Menüleisten Live-Scoreboard",
        menuBarTrackerSubtitle: "Zeigt laufende EuroLeague-Spielstände & aktive Alarme direkt in der macOS-Menüleiste",
        notificationChannelsTitle: "macOS Spieltags-Mitteilungen",
        tipOffChannelTitle: "Tip-Off-Alarm (15 Min. vorher)",
        scoresChannelTitle: "Viertel-, Halbzeit- & Endergebnisse",
        closeGameChannelTitle: "Crunch-Time-Alarm (Q4 ≤ 3:00, ≤ 5 Pkt.)",
        sendTestPushButton: "🔔 Sofortige macOS Test-Mitteilung senden",
        updateSectionTitle: "macOS App-Updates (GitHub Releases)",
        betaUpdateChannelTitle: "Beta- & Vorab-Versionen einbeziehen",
        checkForUpdatesButton: "🔄 Nach Updates suchen",
        upToDateLabel: "✓ Aktuellste Version installiert",
        onboardingWelcomeTitle: "Willkommen bei EuroLeague Courtside 2.0 für macOS",
        onboardingWelcomeSubtitle: "Liquid Glass Edition • Wähle deinen Club, das Design und Live-Alarme.",
        onboardingPickClubTitle: "1. Wähle deinen Verein (Mein Team)",
        onboardingAlertsTitle: "2. Menüleisten-Ticker & Mitteilungen",
        onboardingFinishButton: "Courtside starten →",
        replayOnboardingButton: "✨ Willkommens- & Club-Setup öffnen",
        playerProfileTitle: "Spieler-Scouting-Karte",
        seasonAveragesHeader: "Saisonschnitt",
        shootingSplitsHeader: "Wurfquoten",
        gameLogHeader: "EuroLeague Spiel-Logbuch"
    )

    public static let fr = CourtsideStrings(
        language: .fr,
        navMyTeam: "Mon Équipe",
        navMatchday: "Matchs",
        navStandings: "Classement",
        navAnalytics: "Stats",
        navSettings: "Réglages",
        liveSyncButton: "⚡ Sync Direct",
        searchPlaceholder: "Rechercher un club…",
        roundPrefix: "Journée",
        filterFavorites: "★ Favoris",
        filterAllGames: "Tous les matchs",
        filterLiveOnly: "● Direct",
        statusLive: "EN DIRECT",
        statusFinal: "TERMINÉ",
        statusUpcoming: "À VENIR",
        crunchTimeBadge: "🔥 CRUNCH TIME",
        myTeamHubTitle: "Espace Mon Club",
        nextTipOffBadge: "PROCHAIN ENTRE-DEUX",
        exportAppleCalendarButton: "Ajouter au Calendrier",
        exportSeasonIcsButton: "Exporter Saison .ics",
        activeRosterHeader: "Effectif Actif & Infirmerie",
        verifyRosterButton: "✓ Vérifier l'effectif",
        injuryFit: "Disponible",
        injuryQuestionable: "Incertain (GTD)",
        injuryOut: "Forfait (Blessé)",
        shotChartTab: "🎯 Carte des Tirs",
        momentumTab: "📈 Dynamique",
        playByPlayTab: "⚡ Action par Action",
        clutchFilterLabel: "🔥 Clutch",
        playerBoxscoreHeader: "Feuille de Stats",
        standingsHeader: "Classement Saison Régulière EuroLeague",
        backToStandingsButton: "← Retour au Classement",
        last3GamesHeader: "3 Derniers Matchs",
        next3GamesHeader: "3 Prochains Matchs",
        setAsMyTeamButton: "★ Définir comme Mon Équipe",
        seasonLeadersHeader: "Leaders de la Saison EuroLeague",
        teamComparisonHeader: "Comparaison Face-à-Face",
        playoffProjectionHeader: "Séries éliminatoires projetées (H2H)",
        gameRemindersTitle: "🔔 Rappels du Match",
        gameRemindersSubtitle: "Notifications macOS pour les moments clés de ce match",
        reminderQ1Label: "Score 1er Quart-Temps",
        reminderHalftimeLabel: "Score à la Mi-Temps",
        reminderQ3Label: "Score 3e Quart-Temps",
        reminderCrunchTimeLabel: "Crunch Time (Q4 ≤ 3:00)",
        reminderFinalScoreLabel: "Score Final",
        enableAllRemindersLabel: "Tout activer",
        clearRemindersLabel: "Tout effacer",
        languageSectionTitle: "Langue (6 langues EuroLeague)",
        themeSectionTitle: "Apparence Liquid Glass (Clair & Sombre)",
        lightModeLabel: "☀️ Liquid Glass Clair (Défaut)",
        darkModeLabel: "🌙 Liquid Glass Sombre",
        menuBarTrackerTitle: "🏀 Score en Direct dans la Barre des Menus",
        menuBarTrackerSubtitle: "Affiche les scores EuroLeague en direct dans la barre des menus macOS",
        notificationChannelsTitle: "Notifications macOS Soir de Match",
        tipOffChannelTitle: "Alerte Entre-deux (15 min avant)",
        scoresChannelTitle: "Scores Quart-Temps, Mi-Temps & Final",
        closeGameChannelTitle: "Alerte Crunch-Time (Q4 ≤ 3:00, ≤ 5 pts)",
        sendTestPushButton: "🔔 Envoyer une notification test macOS",
        updateSectionTitle: "Mises à jour macOS (GitHub Releases)",
        betaUpdateChannelTitle: "Inclure les versions Bêta",
        checkForUpdatesButton: "🔄 Vérifier les mises à jour",
        upToDateLabel: "✓ Version à jour",
        onboardingWelcomeTitle: "Bienvenue sur EuroLeague Courtside 2.0 pour macOS",
        onboardingWelcomeSubtitle: "Édition Liquid Glass • Choisissez votre club, votre thème et vos alertes.",
        onboardingPickClubTitle: "1. Choisissez votre club (Mon Équipe)",
        onboardingAlertsTitle: "2. Barre des menus & Notifications",
        onboardingFinishButton: "Entrer sur le parquet →",
        replayOnboardingButton: "✨ Ouvrir l'accueil & configuration",
        playerProfileTitle: "Fiche Scouting Joueur",
        seasonAveragesHeader: "Moyennes Saison",
        shootingSplitsHeader: "Pourcentages aux Tirs",
        gameLogHeader: "Journal des Matchs EuroLeague"
    )

    public static let it = CourtsideStrings(
        language: .it,
        navMyTeam: "La Mia Squadra",
        navMatchday: "Partite",
        navStandings: "Classifica",
        navAnalytics: "Statistiche",
        navSettings: "Impostazioni",
        liveSyncButton: "⚡ Sync Live",
        searchPlaceholder: "Cerca club o arena…",
        roundPrefix: "Giornata",
        filterFavorites: "★ Preferiti",
        filterAllGames: "Tutte le gare",
        filterLiveOnly: "● Live",
        statusLive: "LIVE",
        statusFinal: "FINALE",
        statusUpcoming: "IN PROGRAMMA",
        crunchTimeBadge: "🔥 CRUNCH TIME",
        myTeamHubTitle: "Hub La Mia Squadra",
        nextTipOffBadge: "PROSSIMA PALLA A DUE",
        exportAppleCalendarButton: "Aggiungi a Calendario",
        exportSeasonIcsButton: "Esporta Stagione .ics",
        activeRosterHeader: "Roster Attivo & Infortuni",
        verifyRosterButton: "✓ Verifica Roster Live",
        injuryFit: "Disponibile",
        injuryQuestionable: "In dubbio (GTD)",
        injuryOut: "Indisponibile",
        shotChartTab: "🎯 Mappa Tiri",
        momentumTab: "📈 Inerzia Gara",
        playByPlayTab: "⚡ Azione per Azione",
        clutchFilterLabel: "🔥 Clutch",
        playerBoxscoreHeader: "Tabellino Giocatori",
        standingsHeader: "Classifica Regular Season EuroLeague",
        backToStandingsButton: "← Torna alla Classifica",
        last3GamesHeader: "Ultime 3 Partite",
        next3GamesHeader: "Prossime 3 Partite",
        setAsMyTeamButton: "★ Imposta come La Mia Squadra",
        seasonLeadersHeader: "Leader Stagionali EuroLeague",
        teamComparisonHeader: "Confronto Diretto Squadre",
        playoffProjectionHeader: "Serie Playoff Proiettate (H2H)",
        gameRemindersTitle: "🔔 Promemoria Partita",
        gameRemindersSubtitle: "Notifiche macOS per i momenti chiave di questa gara",
        reminderQ1Label: "Risultato 1° Quarto",
        reminderHalftimeLabel: "Risultato Intervallo",
        reminderQ3Label: "Risultato 3° Quarto",
        reminderCrunchTimeLabel: "Crunch Time (Q4 ≤ 3:00)",
        reminderFinalScoreLabel: "Risultato Finale",
        enableAllRemindersLabel: "Attiva tutti",
        clearRemindersLabel: "Cancella tutti",
        languageSectionTitle: "Lingua (6 lingue EuroLeague)",
        themeSectionTitle: "Aspetto Liquid Glass (Chiaro & Scuro)",
        lightModeLabel: "☀️ Liquid Glass Chiaro (Predefinito)",
        darkModeLabel: "🌙 Liquid Glass Scuro",
        menuBarTrackerTitle: "🏀 Tabellino Live nella Barra dei Menu",
        menuBarTrackerSubtitle: "Mostra i punteggi EuroLeague in tempo reale nella barra dei menu macOS",
        notificationChannelsTitle: "Notifiche macOS Serata Gara",
        tipOffChannelTitle: "Avviso Palla a Due (15 min prima)",
        scoresChannelTitle: "Punteggi Quarti, Intervallo e Finale",
        closeGameChannelTitle: "Avviso Crunch-Time (Q4 ≤ 3:00, ≤ 5 pt)",
        sendTestPushButton: "🔔 Invia notifica di prova macOS",
        updateSectionTitle: "Aggiornamenti macOS (GitHub Releases)",
        betaUpdateChannelTitle: "Includi versioni Beta",
        checkForUpdatesButton: "🔄 Controlla aggiornamenti",
        upToDateLabel: "✓ Versione aggiornata",
        onboardingWelcomeTitle: "Benvenuto in EuroLeague Courtside 2.0 per macOS",
        onboardingWelcomeSubtitle: "Edizione Liquid Glass • Scegli il tuo club, il tema e gli avvisi live.",
        onboardingPickClubTitle: "1. Scegli il tuo Club (La Mia Squadra)",
        onboardingAlertsTitle: "2. Barra dei menu e Notifiche",
        onboardingFinishButton: "Entra in Courtside →",
        replayOnboardingButton: "✨ Apri configurazione iniziale",
        playerProfileTitle: "Scheda Scouting Giocatore",
        seasonAveragesHeader: "Medie Stagionali",
        shootingSplitsHeader: "Percentuali al Tiro",
        gameLogHeader: "Registro Partite EuroLeague"
    )

    public static let es = CourtsideStrings(
        language: .es,
        navMyTeam: "Mi Equipo",
        navMatchday: "Jornada",
        navStandings: "Clasificación",
        navAnalytics: "Estadísticas",
        navSettings: "Ajustes",
        liveSyncButton: "⚡ Sync en Vivo",
        searchPlaceholder: "Buscar club o pabellón…",
        roundPrefix: "Jornada",
        filterFavorites: "★ Favoritos",
        filterAllGames: "Todos",
        filterLiveOnly: "● En Vivo",
        statusLive: "EN VIVO",
        statusFinal: "FINAL",
        statusUpcoming: "PRÓXIMO",
        crunchTimeBadge: "🔥 CRUNCH TIME",
        myTeamHubTitle: "Centro Mi Equipo",
        nextTipOffBadge: "PRÓXIMO SALTO INICIAL",
        exportAppleCalendarButton: "Añadir al Calendario",
        exportSeasonIcsButton: "Exportar Temporada .ics",
        activeRosterHeader: "Plantilla Activa y Parte Médico",
        verifyRosterButton: "✓ Verificar Plantilla",
        injuryFit: "Disponible",
        injuryQuestionable: "Duda (GTD)",
        injuryOut: "Baja (Lesionado)",
        shotChartTab: "🎯 Mapa de Tiros",
        momentumTab: "📈 Ritmo de Partido",
        playByPlayTab: "⚡ Jugada a Jugada",
        clutchFilterLabel: "🔥 Clutch",
        playerBoxscoreHeader: "Estadísticas de Jugadores",
        standingsHeader: "Clasificación Liga Regular EuroLeague",
        backToStandingsButton: "← Volver a Clasificación",
        last3GamesHeader: "Últimos 3 Partidos",
        next3GamesHeader: "Próximos 3 Partidos",
        setAsMyTeamButton: "★ Elegir como Mi Equipo",
        seasonLeadersHeader: "Líderes de la Temporada EuroLeague",
        teamComparisonHeader: "Comparación Cara a Cara",
        playoffProjectionHeader: "Series de Playoffs Proyectadas (H2H)",
        gameRemindersTitle: "🔔 Recordatorios del Partido",
        gameRemindersSubtitle: "Notificaciones de macOS para momentos clave de este partido",
        reminderQ1Label: "Resultado 1.er Cuarto",
        reminderHalftimeLabel: "Resultado al Descanso",
        reminderQ3Label: "Resultado 3.er Cuarto",
        reminderCrunchTimeLabel: "Crunch Time (Q4 ≤ 3:00)",
        reminderFinalScoreLabel: "Marcador Final",
        enableAllRemindersLabel: "Activar todos",
        clearRemindersLabel: "Borrar todos",
        languageSectionTitle: "Idioma (6 idiomas EuroLeague)",
        themeSectionTitle: "Apariencia Liquid Glass (Claro y Oscuro)",
        lightModeLabel: "☀️ Liquid Glass Claro (Predeterminado)",
        darkModeLabel: "🌙 Liquid Glass Oscuro",
        menuBarTrackerTitle: "🏀 Marcador en Vivo en la Barra de Menús",
        menuBarTrackerSubtitle: "Muestra marcadores EuroLeague en tiempo real en la barra de menús de macOS",
        notificationChannelsTitle: "Notificaciones macOS Día de Partido",
        tipOffChannelTitle: "Alerta de Inicio (15 min antes)",
        scoresChannelTitle: "Marcadores por Cuarto, Descanso y Final",
        closeGameChannelTitle: "Alerta Crunch-Time (Q4 ≤ 3:00, ≤ 5 pts)",
        sendTestPushButton: "🔔 Enviar notificación de prueba macOS",
        updateSectionTitle: "Actualizaciones macOS (GitHub Releases)",
        betaUpdateChannelTitle: "Incluir versiones Beta",
        checkForUpdatesButton: "🔄 Buscar actualizaciones",
        upToDateLabel: "✓ Versión actualizada",
        onboardingWelcomeTitle: "Bienvenido a EuroLeague Courtside 2.0 para macOS",
        onboardingWelcomeSubtitle: "Edición Liquid Glass • Elige tu club, apariencia y alertas en vivo.",
        onboardingPickClubTitle: "1. Elige tu Club (Mi Equipo)",
        onboardingAlertsTitle: "2. Barra de menús y Notificaciones",
        onboardingFinishButton: "Entrar a Courtside →",
        replayOnboardingButton: "✨ Abrir configuración inicial",
        playerProfileTitle: "Ficha Scouting del Jugador",
        seasonAveragesHeader: "Promedios de Temporada",
        shootingSplitsHeader: "Porcentajes de Tiro",
        gameLogHeader: "Registro de Partidos EuroLeague"
    )

    public static let el = CourtsideStrings(
        language: .el,
        navMyTeam: "Η Ομάδα Μου",
        navMatchday: "Αγώνες",
        navStandings: "Βαθμολογία",
        navAnalytics: "Στατιστικά",
        navSettings: "Ρυθμίσεις",
        liveSyncButton: "⚡ Live Sync",
        searchPlaceholder: "Αναζήτηση ομάδας ή γηπέδου…",
        roundPrefix: "Αγωνιστική",
        filterFavorites: "★ Αγαπημένα",
        filterAllGames: "Όλοι οι αγώνες",
        filterLiveOnly: "● Live",
        statusLive: "LIVE",
        statusFinal: "ΤΕΛΙΚΟ",
        statusUpcoming: "ΠΡΟΣΕΧΩΣ",
        crunchTimeBadge: "🔥 CRUNCH TIME",
        myTeamHubTitle: "Κέντρο Ομάδας",
        nextTipOffBadge: "ΕΠΟΜΕΝΟ ΤΖΑΜΠΟΛ",
        exportAppleCalendarButton: "Προσθήκη στο Ημερολόγιο",
        exportSeasonIcsButton: "Εξαγωγή Σεζόν .ics",
        activeRosterHeader: "Ενεργό Ρόστερ & Ιατρικό Δελτίο",
        verifyRosterButton: "✓ Έλεγχος Ρόστερ Live",
        injuryFit: "Διαθέσιμος",
        injuryQuestionable: "Αμφίβολος (GTD)",
        injuryOut: "Εκτός (Τραυματίας)",
        shotChartTab: "🎯 Χάρτης Σουτ",
        momentumTab: "📈 Ροή Αγώνα",
        playByPlayTab: "⚡ Φάση προς Φάση",
        clutchFilterLabel: "🔥 Clutch",
        playerBoxscoreHeader: "Στατιστικά Παικτών",
        standingsHeader: "Βαθμολογία Κανονικής Περιόδου EuroLeague",
        backToStandingsButton: "← Πίσω στη Βαθμολογία",
        last3GamesHeader: "Τελευταίοι 3 Αγώνες",
        next3GamesHeader: "Επόμενοι 3 Αγώνες",
        setAsMyTeamButton: "★ Ορισμός ως Η Ομάδα Μου",
        seasonLeadersHeader: "Κορυφαίοι της Σεζόν EuroLeague",
        teamComparisonHeader: "Σύγκριση Ομάδων (H2H)",
        playoffProjectionHeader: "Προβλεπόμενα Ζευγάρια Playoffs",
        gameRemindersTitle: "🔔 Υπενθυμίσεις Αγώνα",
        gameRemindersSubtitle: "Ειδοποιήσεις macOS για τις κρίσιμες στιγμές του αγώνα",
        reminderQ1Label: "Αποτέλεσμα 1ης Περιόδου",
        reminderHalftimeLabel: "Αποτέλεσμα Ημιχρόνου",
        reminderQ3Label: "Αποτέλεσμα 3ης Περιόδου",
        reminderCrunchTimeLabel: "Crunch Time (Q4 ≤ 3:00)",
        reminderFinalScoreLabel: "Τελικό Σκορ",
        enableAllRemindersLabel: "Ενεργοποίηση όλων",
        clearRemindersLabel: "Καθαρισμός",
        languageSectionTitle: "Γλώσσα (6 Γλώσσες EuroLeague)",
        themeSectionTitle: "Εμφάνιση Liquid Glass (Φωτεινό & Σκοτεινό)",
        lightModeLabel: "☀️ Daylight Liquid Glass (Προεπιλογή)",
        darkModeLabel: "🌙 Obsidian Liquid Glass",
        menuBarTrackerTitle: "🏀 Live Σκορ στη Γραμμή Μενού macOS",
        menuBarTrackerSubtitle: "Ζωντανά σκορ EuroLeague & υπενθυμίσεις στη γραμμή μενού macOS",
        notificationChannelsTitle: "Ειδοποιήσεις macOS Αγωνιστικής",
        tipOffChannelTitle: "Ειδοποίηση Τζάμπολ (15 λεπτά πριν)",
        scoresChannelTitle: "Σκορ Περιόδων, Ημιχρόνου & Τελικό",
        closeGameChannelTitle: "Ειδοποίηση Crunch-Time (Q4 ≤ 3:00, ≤ 5 π.)",
        sendTestPushButton: "🔔 Αποστολή Δοκιμαστικής Ειδοποίησης macOS",
        updateSectionTitle: "Ενημερώσεις macOS (GitHub Releases)",
        betaUpdateChannelTitle: "Συμπερίληψη εκδόσεων Beta",
        checkForUpdatesButton: "🔄 Έλεγχος για Ενημερώσεις",
        upToDateLabel: "✓ Έχετε την τελευταία έκδοση",
        onboardingWelcomeTitle: "Καλωσορίσατε στο EuroLeague Courtside 2.0 για macOS",
        onboardingWelcomeSubtitle: "Έκδοση Liquid Glass • Επιλέξτε ομάδα, θέμα και ζωντανές ειδοποιήσεις.",
        onboardingPickClubTitle: "1. Επιλέξτε την Ομάδα σας",
        onboardingAlertsTitle: "2. Γραμμή Μενού & Ειδοποιήσεις",
        onboardingFinishButton: "Είσοδος στο Courtside →",
        replayOnboardingButton: "✨ Άνοιγμα οδηγού καλωσορίσματος",
        playerProfileTitle: "Κάρτα Scouting Παίκτη",
        seasonAveragesHeader: "Μέσοι Όροι Σεζόν",
        shootingSplitsHeader: "Ποσοστά Ευστοχίας",
        gameLogHeader: "Αρχείο Αγώνων EuroLeague"
    )
}
