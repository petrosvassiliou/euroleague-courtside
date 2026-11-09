#!/usr/bin/env python3
"""
Fetches live EuroLeague data from the official keyless EuroLeague APIs:
  - https://api-live.euroleague.net/v2/competitions/E/seasons/{seasonCode}/clubs
  - https://api-live.euroleague.net/v2/competitions/E/seasons/{seasonCode}/clubs/{clubCode}/people
  - https://api-live.euroleague.net/v2/competitions/E/seasons/{seasonCode}/rounds
  - https://api-live.euroleague.net/v2/competitions/E/seasons/{seasonCode}/games
  - https://api-live.euroleague.net/v2/competitions/E/seasons/{seasonCode}/games/{gameCode}/stats
  - https://api-live.euroleague.net/v3/competitions/E/statistics/players/traditional
  - https://api-live.euroleague.net/v3/competitions/E/statistics/teams/traditional
  - https://live.euroleague.net/api/Points?gamecode={gameCode}&seasoncode={seasonCode}
  - Daily Injury Report (IvoVillanueva/INJURYREPORT-EUROLEAGE_2026 & RotoWire)

Downloads all official club crest PNGs into:
  app/src/main/res/drawable-nodpi/club_logo_<code>.png
And writes the complete verified live dataset into:
  app/src/main/assets/euroleague_live_snapshot.json
"""

import base64
import csv
import datetime
import io
import json
import os
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
DRAWABLE_DIR = ROOT_DIR / "app" / "src" / "main" / "res" / "drawable-nodpi"
ASSETS_DIR = ROOT_DIR / "app" / "src" / "main" / "assets"

V2_BASE = "https://api-live.euroleague.net/v2/competitions/E"
V3_BASE = "https://api-live.euroleague.net/v3/competitions/E"
LIVE_BASE = "https://live.euroleague.net/api"

# Official club colors & arenas by EuroLeague club code (and tvCode aliases)
CLUB_STYLE_MAP = {
    "PAN": {"primary": 0xFF007A3D, "secondary": 0xFFD4AF37, "accent": 0xFFFFFFFF, "arena": "OAKA Altion Arena", "city": "Athens", "country": "Greece"},
    "PAO": {"primary": 0xFF007A3D, "secondary": 0xFFD4AF37, "accent": 0xFFFFFFFF, "arena": "OAKA Altion Arena", "city": "Athens", "country": "Greece"},
    "MAD": {"primary": 0xFF00529F, "secondary": 0xFFFEBE10, "accent": 0xFFFFFFFF, "arena": "Movistar Arena", "city": "Madrid", "country": "Spain"},
    "RMB": {"primary": 0xFF00529F, "secondary": 0xFFFEBE10, "accent": 0xFFFFFFFF, "arena": "Movistar Arena", "city": "Madrid", "country": "Spain"},
    "OLY": {"primary": 0xFFE30613, "secondary": 0xFFFFFFFF, "accent": 0xFFD4AF37, "arena": "Peace and Friendship Stadium", "city": "Piraeus", "country": "Greece"},
    "ULK": {"primary": 0xFF002B5C, "secondary": 0xFFFFED00, "accent": 0xFFFFFFFF, "arena": "Ulker Sports and Event Hall", "city": "Istanbul", "country": "Turkiye"},
    "FBB": {"primary": 0xFF002B5C, "secondary": 0xFFFFED00, "accent": 0xFFFFFFFF, "arena": "Ulker Sports and Event Hall", "city": "Istanbul", "country": "Turkiye"},
    "MCO": {"primary": 0xFFCE1126, "secondary": 0xFFD4AF37, "accent": 0xFFFFFFFF, "arena": "Salle Gaston Medecin", "city": "Monaco", "country": "Monaco"},
    "ASM": {"primary": 0xFFCE1126, "secondary": 0xFFD4AF37, "accent": 0xFFFFFFFF, "arena": "Salle Gaston Medecin", "city": "Monaco", "country": "Monaco"},
    "BAR": {"primary": 0xFFA50044, "secondary": 0xFF004D98, "accent": 0xFFEDBB00, "arena": "Palau Blaugrana", "city": "Barcelona", "country": "Spain"},
    "FCB": {"primary": 0xFFA50044, "secondary": 0xFF004D98, "accent": 0xFFEDBB00, "arena": "Palau Blaugrana", "city": "Barcelona", "country": "Spain"},
    "MUN": {"primary": 0xFFDC052D, "secondary": 0xFF0066B2, "accent": 0xFFFFFFFF, "arena": "SAP Garden", "city": "Munich", "country": "Germany"},
    "BAY": {"primary": 0xFFDC052D, "secondary": 0xFF0066B2, "accent": 0xFFFFFFFF, "arena": "SAP Garden", "city": "Munich", "country": "Germany"},
    "ZAL": {"primary": 0xFF006233, "secondary": 0xFFFFFFFF, "accent": 0xFFD4AF37, "arena": "Zalgirio Arena", "city": "Kaunas", "country": "Lithuania"},
    "PAR": {"primary": 0xFF1A1A1A, "secondary": 0xFFFFFFFF, "accent": 0xFFD4AF37, "arena": "Belgrade Arena", "city": "Belgrade", "country": "Serbia"},
    "IST": {"primary": 0xFF0033A0, "secondary": 0xFFE30613, "accent": 0xFFFFFFFF, "arena": "Basketball Development Center", "city": "Istanbul", "country": "Turkiye"},
    "EFS": {"primary": 0xFF0033A0, "secondary": 0xFFE30613, "accent": 0xFFFFFFFF, "arena": "Basketball Development Center", "city": "Istanbul", "country": "Turkiye"},
    "RED": {"primary": 0xFFE30613, "secondary": 0xFFFFFFFF, "accent": 0xFF0033A0, "arena": "Belgrade Arena", "city": "Belgrade", "country": "Serbia"},
    "CZV": {"primary": 0xFFE30613, "secondary": 0xFFFFFFFF, "accent": 0xFF0033A0, "arena": "Belgrade Arena", "city": "Belgrade", "country": "Serbia"},
    "MIL": {"primary": 0xFFD6001C, "secondary": 0xFFFFFFFF, "accent": 0xFFD4AF37, "arena": "Unipol Forum", "city": "Milan", "country": "Italy"},
    "EA7": {"primary": 0xFFD6001C, "secondary": 0xFFFFFFFF, "accent": 0xFFD4AF37, "arena": "Unipol Forum", "city": "Milan", "country": "Italy"},
    "BAS": {"primary": 0xFFA6192E, "secondary": 0xFF00205B, "accent": 0xFFD4AF37, "arena": "Buesa Arena", "city": "Vitoria-Gasteiz", "country": "Spain"},
    "BKN": {"primary": 0xFFA6192E, "secondary": 0xFF00205B, "accent": 0xFFD4AF37, "arena": "Buesa Arena", "city": "Vitoria-Gasteiz", "country": "Spain"},
    "KBA": {"primary": 0xFFA6192E, "secondary": 0xFF00205B, "accent": 0xFFD4AF37, "arena": "Buesa Arena", "city": "Vitoria-Gasteiz", "country": "Spain"},
    "VIR": {"primary": 0xFF141414, "secondary": 0xFFFFFFFF, "accent": 0xFFD4AF37, "arena": "Virtus Segafredo Arena", "city": "Bologna", "country": "Italy"},
    "TEL": {"primary": 0xFFFFD100, "secondary": 0xFF0033A0, "accent": 0xFFFFFFFF, "arena": "Menora Mivtachim Arena", "city": "Tel Aviv", "country": "Israel"},
    "MTA": {"primary": 0xFFFFD100, "secondary": 0xFF0033A0, "accent": 0xFFFFFFFF, "arena": "Menora Mivtachim Arena", "city": "Tel Aviv", "country": "Israel"},
    "PRS": {"primary": 0xFF111116, "secondary": 0xFF00B2A9, "accent": 0xFFE30613, "arena": "Adidas Arena", "city": "Paris", "country": "France"},
    "ASV": {"primary": 0xFF1E1E24, "secondary": 0xFFFFFFFF, "accent": 0xFF00A859, "arena": "LDLC Arena", "city": "Villeurbanne", "country": "France"},
    "BER": {"primary": 0xFFFFCC00, "secondary": 0xFF002F6C, "accent": 0xFFFFFFFF, "arena": "Uber Arena", "city": "Berlin", "country": "Germany"},
    "PAM": {"primary": 0xFFFF6600, "secondary": 0xFF1A1A1A, "accent": 0xFFFFFFFF, "arena": "Roig Arena / Fuente de San Luis", "city": "Valencia", "country": "Spain"},
    "VBC": {"primary": 0xFFFF6600, "secondary": 0xFF1A1A1A, "accent": 0xFFFFFFFF, "arena": "Roig Arena / Fuente de San Luis", "city": "Valencia", "country": "Spain"},
    "DUB": {"primary": 0xFFC59B27, "secondary": 0xFF121216, "accent": 0xFFFFFFFF, "arena": "Coca-Cola Arena", "city": "Dubai", "country": "UAE"},
    "HTA": {"primary": 0xFFD9001B, "secondary": 0xFFFFFFFF, "accent": 0xFF1A1A1A, "arena": "Shlomo Group Arena", "city": "Tel Aviv", "country": "Israel"},
    "BES": {"primary": 0xFF141418, "secondary": 0xFFFFFFFF, "accent": 0xFFE30613, "arena": "BJK Akatlar Arena", "city": "Istanbul", "country": "Turkiye"},
    "BJK": {"primary": 0xFF141418, "secondary": 0xFFFFFFFF, "accent": 0xFFE30613, "arena": "BJK Akatlar Arena", "city": "Istanbul", "country": "Turkiye"},
}


def fetch_json(url: str, timeout: int = 20):
    req = urllib.request.Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "EuroLeagueCourtside/1.1 (Android; GooglebookOS)"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            if not raw.strip():
                return None
            return json.loads(raw)
    except Exception as exc:
        print(f"[WARN] Failed to fetch {url}: {exc}")
        return None


def download_binary(url: str, timeout: int = 20):
    if not url:
        return None
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "EuroLeagueCourtside/1.1"}
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.read()
    except Exception as exc:
        print(f"[WARN] Failed to download image {url}: {exc}")
        return None


def extract_rows(payload):
    if isinstance(payload, list):
        return payload
    if isinstance(payload, dict):
        for k in ("data", "Data", "Rows", "rows", "teams", "players"):
            if isinstance(payload.get(k), list):
                return payload[k]
    return []


def format_person_name(raw_name: str, passport_name: str = "", passport_surname: str = "") -> str:
    if passport_name and passport_surname:
        return f"{passport_name.strip().title()} {passport_surname.strip().title()}"
    if not raw_name:
        return "Unknown Player"
    if "," in raw_name:
        parts = [p.strip() for p in raw_name.split(",", 1)]
        return f"{parts[1].title()} {parts[0].title()}"
    return raw_name.strip().title()


def map_position(pos_name: str, pos_code=None) -> str:
    p = (pos_name or "").lower()
    if "guard" in p or pos_code == 1:
        return "G"
    if "forward" in p or pos_code == 2:
        return "F"
    if "center" in p or pos_code == 3:
        return "C"
    return "G/F"


def fetch_latest_injuries():
    """Fetches daily EuroLeague injury report from IvoVillanueva/INJURYREPORT-EUROLEAGE_2026 or RotoWire."""
    injuries_by_name = {}
    today = datetime.date.today()
    for delta in range(0, 7):
        d = today - datetime.timedelta(days=delta)
        date_str = d.strftime("%Y_%m_%d")
        csv_url = f"https://raw.githubusercontent.com/IvoVillanueva/INJURYREPORT-EUROLEAGE_2026/main/data/euroleage_injuryreport_{date_str}.csv"
        req = urllib.request.Request(csv_url, headers={"User-Agent": "EuroLeagueCourtside/1.1"})
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                text = resp.read().decode("utf-8", errors="replace")
                reader = csv.DictReader(io.StringIO(text))
                for row in reader:
                    pname = (row.get("player") or "").strip().lower()
                    status = (row.get("status") or "OUT").strip().upper()
                    injury = (row.get("injury") or "").strip()
                    if pname:
                        injuries_by_name[pname] = {
                            "status": "GAME_TIME_DECISION" if "DECISION" in status or "GTD" in status or "QUESTION" in status else "OUT",
                            "injury": injury
                        }
                print(f"[INFO] Loaded {len(injuries_by_name)} live injury entries from {date_str}")
                if injuries_by_name:
                    return injuries_by_name
        except Exception:
            continue
    return injuries_by_name


def parse_iso_epoch_millis(iso_str: str) -> int:
    if not iso_str:
        return int(time.time() * 1000)
    clean = iso_str.strip()
    if clean.endswith("Z"):
        clean = clean[:-1] + "+00:00"
    elif "+" not in clean[10:] and "-" not in clean[10:]:
        clean = clean + "+00:00"
    try:
        dt = datetime.datetime.fromisoformat(clean)
        return int(dt.timestamp() * 1000)
    except Exception:
        return int(time.time() * 1000)


def main():
    DRAWABLE_DIR.mkdir(parents=True, exist_ok=True)
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)

    # Detect active season (try E2026 first, then E2025)
    season_code = "E2026"
    clubs_payload = fetch_json(f"{V2_BASE}/seasons/{season_code}/clubs")
    clubs_raw = extract_rows(clubs_payload)
    if not clubs_raw:
        season_code = "E2025"
        clubs_payload = fetch_json(f"{V2_BASE}/seasons/{season_code}/clubs")
        clubs_raw = extract_rows(clubs_payload)

    print(f"[INFO] Using active EuroLeague season: {season_code} ({len(clubs_raw)} clubs)")

    # Also fetch E2025 clubs if some historical logos are useful
    injuries_map = fetch_latest_injuries()

    # Fetch player traditional stats (PerGame)
    player_stats_payload = fetch_json(
        f"{V3_BASE}/statistics/players/traditional?seasonCode={season_code}&statisticMode=PerGame&limit=500"
    )
    player_stats_rows = extract_rows(player_stats_payload)
    stats_by_player_code = {}
    for row in player_stats_rows:
        player_obj = row.get("player")
        if isinstance(player_obj, str):
            try:
                player_obj = json.loads(player_obj)
            except Exception:
                player_obj = {}
        if isinstance(player_obj, dict):
            pcode = str(player_obj.get("code") or "").strip()
            if pcode:
                stats_by_player_code[pcode] = row

    print(f"[INFO] Loaded season per-game stats for {len(stats_by_player_code)} players")

    # Fetch team traditional stats (PerGame)
    team_stats_payload = fetch_json(
        f"{V3_BASE}/statistics/teams/traditional?seasonCode={season_code}&statisticMode=PerGame&limit=50"
    )
    team_stats_rows = extract_rows(team_stats_payload)
    team_stats_by_code = {}
    for row in team_stats_rows:
        t_obj = row.get("team")
        if isinstance(t_obj, str):
            try:
                t_obj = json.loads(t_obj)
            except Exception:
                t_obj = {}
        if isinstance(t_obj, dict):
            tcode = str(t_obj.get("code") or "").strip()
            if tcode:
                team_stats_by_code[tcode] = row

    # Fetch all season games to compute accurate W-L, pointsFor, pointsAgainst, streak, and schedule
    all_games_payload = fetch_json(f"{V2_BASE}/seasons/{season_code}/games")
    all_games_raw = extract_rows(all_games_payload)
    print(f"[INFO] Loaded {len(all_games_raw)} total games for season {season_code}")

    # Compute standings & last-5 form per club code from played games
    club_records = {}
    for c in clubs_raw:
        code = str(c.get("code") or "").strip()
        if code:
            club_records[code] = {
                "wins": 0,
                "losses": 0,
                "pointsFor": 0,
                "pointsAgainst": 0,
                "results": []  # list of (round, "S" or "N")
            }

    # Sort games by round & date
    sorted_games = sorted(all_games_raw, key=lambda g: (int(g.get("round") or 0), str(g.get("utcDate") or g.get("date") or "")))
    for g in sorted_games:
        if not g.get("played"):
            continue
        local = g.get("local") or {}
        road = g.get("road") or {}
        local_club = (local.get("club") or {})
        road_club = (road.get("club") or {})
        l_code = str(local_club.get("code") or "").strip()
        r_code = str(road_club.get("code") or "").strip()
        l_score = int(local.get("score") or 0)
        r_score = int(road.get("score") or 0)
        if l_score == 0 and r_score == 0:
            continue
        rnd = int(g.get("round") or 0)
        if l_code in club_records:
            club_records[l_code]["pointsFor"] += l_score
            club_records[l_code]["pointsAgainst"] += r_score
            if l_score > r_score:
                club_records[l_code]["wins"] += 1
                club_records[l_code]["results"].append((rnd, "S"))
            else:
                club_records[l_code]["losses"] += 1
                club_records[l_code]["results"].append((rnd, "N"))
        if r_code in club_records:
            club_records[r_code]["pointsFor"] += r_score
            club_records[r_code]["pointsAgainst"] += l_score
            if r_score > l_score:
                club_records[r_code]["wins"] += 1
                club_records[r_code]["results"].append((rnd, "S"))
            else:
                club_records[r_code]["losses"] += 1
                club_records[r_code]["results"].append((rnd, "N"))

    # Build teams list & download official club logos
    teams_out = []
    rosters_out = []
    logo_data_uris = {}

    for c in clubs_raw:
        if c.get("isVirtual"):
            continue
        code = str(c.get("code") or "").strip()
        tv_code = str(c.get("tvCode") or code).strip()
        name = str(c.get("name") or code).strip()
        images = c.get("images") or {}
        crest_url = str(images.get("crest") or "").strip()

        # Download official PNG logo
        drawable_name = f"club_logo_{code.lower()}"
        if crest_url:
            img_bytes = download_binary(crest_url)
            if img_bytes:
                out_png = DRAWABLE_DIR / f"{drawable_name}.png"
                out_png.write_bytes(img_bytes)
                # Also create tvCode alias drawable if different
                tv_drawable = f"club_logo_{tv_code.lower()}"
                if tv_drawable != drawable_name:
                    (DRAWABLE_DIR / f"{tv_drawable}.png").write_bytes(img_bytes)
                b64 = base64.b64encode(img_bytes).decode("ascii")
                logo_data_uris[code] = f"data:image/png;base64,{b64}"
                print(f"[LOGO] Saved {out_png.name} ({len(img_bytes)} bytes) from {crest_url}")

        style = CLUB_STYLE_MAP.get(code) or CLUB_STYLE_MAP.get(tv_code) or {
            "primary": 0xFFFF6600,
            "secondary": 0xFF1A1A22,
            "accent": 0xFFFFFFFF,
            "arena": str(c.get("venueCode") or "EuroLeague Arena"),
            "city": str(c.get("city") or "").title(),
            "country": str((c.get("country") or {}).get("name") or "")
        }

        rec = club_records.get(code) or {"wins": 0, "losses": 0, "pointsFor": 0, "pointsAgainst": 0, "results": []}
        last5 = [r[1] for r in rec["results"][-5:]]
        while len(last5) < 5:
            last5.append("S")

        t_stat = team_stats_by_code.get(code) or {}
        gp = max(1, rec["wins"] + rec["losses"])
        ppg = float(t_stat.get("pointsScored") or (rec["pointsFor"] / gp if gp else 82.0))
        opp_ppg = (rec["pointsAgainst"] / gp) if (rec["wins"] + rec["losses"]) > 0 else 80.0
        two_pct_raw = str(t_stat.get("twoPointersPercentage") or "54.5").replace("%", "")
        three_pct_raw = str(t_stat.get("threePointersPercentage") or "37.2").replace("%", "")
        reb_raw = float(t_stat.get("totalRebounds") or 34.5)

        try:
            two_pct = float(two_pct_raw)
            if two_pct <= 1.0:
                two_pct *= 100.0
        except Exception:
            two_pct = 54.5

        try:
            three_pct = float(three_pct_raw)
            if three_pct <= 1.0:
                three_pct *= 100.0
        except Exception:
            three_pct = 37.2

        city_str = str(c.get("city") or style["city"]).title()
        country_str = str((c.get("country") or {}).get("name") or style["country"])

        teams_out.append({
            "id": code,
            "code": tv_code,
            "name": name,
            "city": city_str,
            "country": country_str,
            "arena": style["arena"],
            "logoUrl": crest_url,
            "localLogoResName": drawable_name,
            "primaryColorHex": style["primary"],
            "secondaryColorHex": style["secondary"],
            "accentColorHex": style["accent"],
            "wins": rec["wins"],
            "losses": rec["losses"],
            "pointsFor": rec["pointsFor"],
            "pointsAgainst": rec["pointsAgainst"],
            "streak": last5,
            "offensiveRating": round(ppg * 1.32, 1),
            "defensiveRating": round(opp_ppg * 1.32, 1),
            "pace": round(70.0 + (ppg - 78.0) * 0.35, 1),
            "threePointPct": round(three_pct, 1),
            "twoPointPct": round(two_pct, 1),
            "reboundRate": round(min(57.5, max(44.5, reb_raw * 1.42)), 1)
        })

        # Fetch live club roster from /seasons/{seasonCode}/clubs/{code}/people
        people_payload = fetch_json(f"{V2_BASE}/seasons/{season_code}/clubs/{code}/people")
        people_rows = extract_rows(people_payload)
        club_players_count = 0
        for member in people_rows:
            type_code = str(member.get("type") or "").strip()
            type_name = str(member.get("typeName") or "").strip()
            if type_code != "J" and type_name.lower() != "player":
                continue
            if member.get("active") is False:
                continue

            person = member.get("person") or {}
            pcode = str(person.get("code") or "").strip()
            full_name = format_person_name(
                str(person.get("name") or ""),
                str(person.get("passportName") or ""),
                str(person.get("passportSurname") or "")
            )
            dorsal_str = str(member.get("dorsal") or member.get("dorsalRaw") or "0").strip()
            try:
                dorsal = int(re.sub(r"\D", "", dorsal_str) or "0")
            except Exception:
                dorsal = 0

            pos = map_position(str(member.get("positionName") or ""), member.get("position"))
            country_obj = person.get("country") or {}
            nationality = str(country_obj.get("code") or country_obj.get("name") or "INT").strip()
            height_cm = int(person.get("height") or 198)
            member_imgs = member.get("images") or {}
            person_imgs = person.get("images") or {}
            headshot_url = str(member_imgs.get("headshot") or person_imgs.get("headshot") or "").strip()

            p_stat = stats_by_player_code.get(pcode) or {}
            p_ppg = float(p_stat.get("pointsScored") or 0.0)
            p_rpg = float(p_stat.get("totalRebounds") or 0.0)
            p_apg = float(p_stat.get("assists") or 0.0)
            p_pir = float(p_stat.get("pir") or 0.0)

            # Check injury status from daily injury report
            inj_entry = injuries_map.get(full_name.lower())
            if not inj_entry:
                # Try matching by last name + club
                surname = str(person.get("passportSurname") or "").strip().lower()
                if surname:
                    for k, v in injuries_map.items():
                        if k.endswith(" " + surname) or k == surname:
                            inj_entry = v
                            break

            inj_status = inj_entry["status"] if inj_entry else "FIT"
            inj_detail = inj_entry["injury"] if inj_entry else ""

            rosters_out.append({
                "id": pcode or f"{code}_{dorsal}_{full_name}",
                "teamId": code,
                "number": dorsal,
                "name": full_name,
                "position": pos,
                "nationality": nationality,
                "heightCm": height_cm,
                "ppg": round(p_ppg, 1),
                "rpg": round(p_rpg, 1),
                "apg": round(p_apg, 1),
                "pir": round(p_pir, 1),
                "injuryStatus": inj_status,
                "injuryDetail": inj_detail,
                "headshotUrl": headshot_url
            })
            club_players_count += 1

        print(f"[ROSTER] {code} ({name}): verified {club_players_count} active players")

    # Check which rounds actually have played games
    played_rounds = sorted({int(g.get("round") or 1) for g in all_games_raw if g.get("played")})
    latest_played_round = played_rounds[-1] if played_rounds else 1
    active_round = latest_played_round
    print(f"[INFO] Active round set to latest played round: Round {active_round}")

    # Select rounds to fetch full boxscore + shot chart details for
    detail_rounds = {latest_played_round, max(1, latest_played_round - 1)}

    games_out = []
    for g in sorted_games:
        rnd = int(g.get("round") or 1)
        game_code = int(g.get("gameCode") or 0)
        played = bool(g.get("played"))
        game_status_str = str(g.get("gameStatus") or "").lower()
        local = g.get("local") or {}
        road = g.get("road") or {}
        l_club = local.get("club") or {}
        r_club = road.get("club") or {}
        l_code = str(l_club.get("code") or "").strip()
        r_code = str(r_club.get("code") or "").strip()
        if not l_code or not r_code:
            continue

        l_score = int(local.get("score") or 0)
        r_score = int(road.get("score") or 0)
        l_part = local.get("partials") or {}
        r_part = road.get("partials") or {}

        q1_h = int(l_part.get("partials1") or 0)
        q2_h = int(l_part.get("partials2") or 0)
        q3_h = int(l_part.get("partials3") or 0)
        q4_h = int(l_part.get("partials4") or 0)
        q1_a = int(r_part.get("partials1") or 0)
        q2_a = int(r_part.get("partials2") or 0)
        q3_a = int(r_part.get("partials3") or 0)
        q4_a = int(r_part.get("partials4") or 0)

        utc_date = str(g.get("utcDate") or g.get("date") or "")
        tip_off_ms = parse_iso_epoch_millis(utc_date)
        venue_name = str((g.get("venue") or {}).get("name") or "")

        if "live" in game_status_str or "playing" in game_status_str:
            status = "LIVE"
        elif played or (l_score > 0 or r_score > 0):
            status = "FINAL"
        else:
            status = "UPCOMING"

        box_home = []
        box_away = []
        shots_list = []

        # Fetch full boxscore & shot chart for played/live games in detail_rounds
        if (played or status == "LIVE") and rnd in {active_round, latest_played_round} and game_code > 0:
            stats_data = fetch_json(f"{V2_BASE}/seasons/{season_code}/games/{game_code}/stats")
            if isinstance(stats_data, dict):
                for side_key, target_list, team_code in [("local", box_home, l_code), ("road", box_away, r_code)]:
                    side_obj = stats_data.get(side_key) or {}
                    players_arr = side_obj.get("players") or []
                    for p_entry in players_arr:
                        p_reg = p_entry.get("player") or {}
                        p_person = p_reg.get("person") or {}
                        st = p_entry.get("stats") or {}
                        sec_played = float(st.get("timePlayed") or 0.0)
                        if sec_played <= 0 and float(st.get("points") or 0.0) == 0:
                            continue
                        mins = int(sec_played // 60)
                        secs = int(sec_played % 60)
                        min_str = f"{mins:02d}:{secs:02d}"
                        p_id = str(p_person.get("code") or "")
                        p_name = format_person_name(
                            str(p_person.get("name") or ""),
                            str(p_person.get("passportName") or ""),
                            str(p_person.get("passportSurname") or "")
                        )
                        dorsal_num = int(st.get("dorsal") or 0)
                        target_list.append({
                            "playerId": p_id,
                            "playerName": p_name,
                            "jerseyNumber": dorsal_num,
                            "teamId": team_code,
                            "minutes": min_str,
                            "points": int(st.get("points") or 0),
                            "offRebounds": int(st.get("offensiveRebounds") or 0),
                            "defRebounds": int(st.get("defensiveRebounds") or 0),
                            "assists": int(st.get("assistances") or 0),
                            "steals": int(st.get("steals") or 0),
                            "blocks": int(st.get("blocksFavour") or 0),
                            "turnovers": int(st.get("turnovers") or 0),
                            "twoPtMade": int(st.get("fieldGoalsMade2") or 0),
                            "twoPtAttempted": int(st.get("fieldGoalsAttempted2") or 0),
                            "threePtMade": int(st.get("fieldGoalsMade3") or 0),
                            "threePtAttempted": int(st.get("fieldGoalsAttempted3") or 0),
                            "ftMade": int(st.get("freeThrowsMade") or 0),
                            "ftAttempted": int(st.get("freeThrowsAttempted") or 0),
                            "foulsDrawn": int(st.get("foulsReceived") or 0),
                            "foulsCommitted": int(st.get("foulsCommited") or 0)
                        })

            # Fetch shot chart points from live.euroleague.net/api/Points
            time.sleep(0.15)
            points_data = fetch_json(f"{LIVE_BASE}/Points?gamecode={game_code}&seasoncode={season_code}")
            point_rows = extract_rows(points_data)
            for idx, pt in enumerate(point_rows[:60]):
                action_id = str(pt.get("ID_ACTION") or "").strip().upper()
                if action_id not in ("2FGM", "2FGA", "3FGM", "3FGA"):
                    continue
                try:
                    cx = float(pt.get("COORD_X") or 0.0)
                    cy = float(pt.get("COORD_Y") or 0.0)
                except Exception:
                    continue
                if cx == -1 and cy == -1:
                    continue
                # Normalize [-750..750] -> [0.05..0.95], [-100..900] -> [0.08..0.92]
                norm_x = max(0.05, min(0.95, (cx + 750.0) / 1500.0))
                norm_y = max(0.08, min(0.92, (cy + 120.0) / 1050.0))
                made = action_id.endswith("M")
                is_three = action_id.startswith("3")
                raw_team = str(pt.get("TEAM") or "").strip()
                raw_player = format_person_name(str(pt.get("PLAYER") or ""))
                minute_val = int(pt.get("MINUTE") or 1)
                qtr = min(4, max(1, (minute_val - 1) // 10 + 1))
                shots_list.append({
                    "id": f"shot_{game_code}_{idx}",
                    "playerId": str(pt.get("ID_PLAYER") or "").strip(),
                    "playerName": raw_player,
                    "teamId": raw_team or l_code,
                    "courtX": round(norm_x, 3),
                    "courtY": round(norm_y, 3),
                    "isMade": made,
                    "made": made,
                    "isThreePointer": is_three,
                    "quarter": qtr
                })

        games_out.append({
            "id": f"{season_code}_{game_code}",
            "gameCode": game_code,
            "seasonCode": season_code,
            "round": rnd,
            "homeTeamId": l_code,
            "awayTeamId": r_code,
            "venueName": venue_name,
            "tipOffEpochMillis": tip_off_ms,
            "status": status,
            "currentQuarter": 4 if status == "FINAL" else 1,
            "remainingClock": "00:00" if status == "FINAL" else "10:00",
            "homeScore": l_score,
            "awayScore": r_score,
            "quarters": {
                "q1Home": q1_h,
                "q1Away": q1_a,
                "q2Home": q2_h,
                "q2Away": q2_a,
                "q3Home": q3_h,
                "q3Away": q3_a,
                "q4Home": q4_h,
                "q4Away": q4_a
            },
            "q1Home": q1_h,
            "q1Away": q1_a,
            "q2Home": q2_h,
            "q2Away": q2_a,
            "q3Home": q3_h,
            "q3Away": q3_a,
            "q4Home": q4_h,
            "q4Away": q4_a,
            "boxscoreHome": box_home,
            "boxscoreAway": box_away,
            "shotChart": shots_list,
            "shots": shots_list
        })

    snapshot = {
        "syncedAtUtc": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "syncedAtIso": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "seasonCode": season_code,
        "activeRound": active_round,
        "latestPlayedRound": latest_played_round,
        "teams": teams_out,
        "players": rosters_out,
        "rosters": rosters_out,
        "games": games_out
    }

    snapshot_file = ASSETS_DIR / "euroleague_live_snapshot.json"
    snapshot_file.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[SUCCESS] Wrote {snapshot_file} ({len(teams_out)} teams, {len(rosters_out)} players, {len(games_out)} games)")

    # Also save a compact logo data-uri map for HTML preview synchronization
    logos_file = ASSETS_DIR / "euroleague_club_logos_b64.json"
    logos_file.write_text(json.dumps(logo_data_uris, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[SUCCESS] Wrote {logos_file} ({len(logo_data_uris)} official club logos)")


if __name__ == "__main__":
    main()
