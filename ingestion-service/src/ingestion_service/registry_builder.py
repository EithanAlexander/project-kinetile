"""Build infrastructure-registry.json from a previously-seeded legacy file catalog."""

from __future__ import annotations

import argparse
import json
import random
import re
import uuid
from datetime import date, timedelta
from pathlib import Path
from typing import Final

MANUFACTURERS: Final = ["Aslan", "Æ Inc", "GFS"]
SIZE_POOL: Final = ["600x600", "800x800"]
COLOR_POOL: Final = ["Slate Gray", "Safety Yellow"]

CITY_CODES: Final[dict[str, str]] = {
    "Tel Aviv-Yafo": "TLV",
    "Jerusalem": "JLM",
    "Haifa": "HFA",
    "Be'er Sheva": "BEV",
    "Eilat": "EIL",
    "Bnei Brak": "BBR",
    "Netanya": "NTY",
    "Ashdod": "ASD",
    "Petah Tikva": "PTK",
    "Rishon LeZion": "RIS",
    "Holon": "HLN",
    "Herzliya": "HRZ",
    "Rehovot": "RVT",
    "Ramat Gan": "RMG",
    "Ashkelon": "AKL",
}

TIER_RANGES: Final[dict[str, tuple[int, int]]] = {
    "HIGH": (65, 90),
    "MEDIUM": (35, 64),
    "LOW": (10, 34),
}

PLACE_TYPE_TIERS: Final[dict[str, str]] = {
    "MARKET": "HIGH",
    "BUS_STATION": "HIGH",
    "TRAIN_STATION": "HIGH",
    "TURNSTILE": "HIGH",
    "MALL": "HIGH",
    "SQUARE": "MEDIUM",
    "PROMENADE": "MEDIUM",
    "WALKWAY": "MEDIUM",
    "BIKE_LANE": "MEDIUM",
    "BEACH": "MEDIUM",
    "PARK": "MEDIUM",
    "BRIDGE": "MEDIUM",
    "STREET": "LOW",
}

_LEGACY_CHOKEPOINTS: Final[dict[str, list[str]]] = {
    "Tel Aviv-Yafo": [
        "Tel Aviv Savidor Station Turnstiles",
        "Rothschild Blvd Bike Lane",
        "Habima Square Promenade",
        "Dizengoff Center Entrance",
        "Azrieli Center Pedestrian Bridge Area",
        "Sarona Market Entrance",
        "Tel Aviv University Train Station Exit",
        "Tayelet (Herbert Samuel Promenade) - Gordon Beach Segment",
        "Carmel Market / Allenby Junction",
        "Ibn Gabirol St - Rabin Square Segment",
    ],
    "Jerusalem": [
        "Jaffa Road Light Rail Corridor",
        "Ben Yehuda Pedestrian Mall",
        "Mahane Yehuda Market Entrance",
        "Mamilla Pedestrian Avenue",
        "Jerusalem Central Bus Station / Yitzhak Navon Station Connection",
        "Zion Square",
        "King George St - Downtown Segment",
        "Sacher Park Edge Walkways",
    ],
    "Haifa": [
        "Bat Galim Promenade",
        "German Colony - Ben Gurion Boulevard",
        "Grand Canyon Mall Entrance",
        "Horev Center Junction",
        "Carmel Center Pedestrian Zone",
        "HaMifratz Central Station Access Walkways",
        "Hof HaCarmel Central Bus Station Approaches",
    ],
    "Bnei Brak": [
        "Rabbi Akiva Street",
        "Rabbi Akiva St / Rashi St Junction",
        "Jabotinsky St - Bnei Brak Segment",
        "Kahaneman Street Near City Center",
        "Ahuza / Ezra Street Commercial Segment",
    ],
    "Netanya": [
        "Herzl Street Pedestrian Segment",
        "Atzmaut Square",
        "Herzl Beach Promenade Access",
        "Sironit Promenade",
        "Poleg Beach Boardwalk Area",
        "Netanya Central Bus Station Approaches",
    ],
    "Be'er Sheva": [
        "Ben-Gurion University Main Gate Walkways",
        "Grand Kanyon Entrance",
        "Old City Pedestrian Streets",
        "Be'er Sheva Central Bus Station Access",
        "Beer Sheva North University Station Exit",
        "Rager Boulevard Commercial Segment",
    ],
    "Ashdod": [
        "BIG Fashion Ashdod Entrance",
        "Ashdod Beach Promenade",
        "Rogozin Street Commercial Segment",
        "Marina / Lido Beach Walkway",
        "City Mall Entrance Area",
        "Ashdod Central Bus Station Approaches",
    ],
    "Petah Tikva": [
        "Grand Mall Petah Tikva Entrance",
        "Jabotinsky Street Commercial Segment",
        "Kfar Ganim Commercial Center Walkways",
        "Petah Tikva Central Bus Station Area",
        "Yachin Center Entrance",
    ],
    "Rishon LeZion": [
        "Rothschild Street Central Segment",
        "Hazahav Mall Entrance",
        "Moshe Dayan Station Approaches",
        "Rishon Beach Promenade",
        "HaMeyasdim Street / Old City Area",
    ],
    "Holon": [
        "Azrieli Holon / Holon Junction Walkways",
        "Golda Park / Peres Park Promenade",
        "Sokolov Street Commercial Segment",
        "Holon Design Museum Area",
        "Wolfson Medical Center Approaches",
    ],
    "Herzliya": [
        "Herzliya Marina Boardwalk",
        "Seven Stars Mall Entrance",
        "Herzliya Train Station Exit",
        "Ben Gurion Blvd - City Center Segment",
        "Beachfront Promenade - Arena Segment",
    ],
    "Rehovot": [
        "Herzl Street Central Segment",
        "Weizmann Institute Main Gate Area",
        "Rehovot Train Station Exit",
        "Ofer Rehovot Mall Entrance",
        "Science Park Pedestrian Axis",
    ],
    "Ramat Gan": [
        "Bursa District Pedestrian Spine",
        "Ayalon Mall Entrance",
        "Bialik Street Central Segment",
        "Ramat Gan Safari Entrance Plaza",
        "Diamond Exchange Area Walkways",
    ],
    "Ashkelon": [
        "Ashkelon Marina Promenade",
        "Barnea Beach Boardwalk",
        "Afridar Commercial Strip",
        "Ashkelon Central Bus Station Area",
        "Ashkelon Mall Entrance",
    ],
    "Eilat": [
        "Eilat North Beach Promenade",
        "Ice Mall Entrance",
        "Mall Hayam Entrance",
        "Coral Beach Visitor Walkways",
        "Central Bus Station / Tourism Zone Connection",
    ],
}


def _load_legacy_chokepoints() -> dict[str, list[str]]:
    return {city: list(sites) for city, sites in _LEGACY_CHOKEPOINTS.items()}


def classify_place_type(name: str) -> str:
    """Heuristic place-type classification from chokepoint name."""
    lower = name.lower()
    if "turnstile" in lower:
        return "TURNSTILE"
    if "market" in lower or "carmel" in lower or "mahane" in lower or "sarona" in lower:
        return "MARKET"
    if "bus station" in lower or "central station" in lower and "train" not in lower:
        return "BUS_STATION"
    if "train station" in lower or "station exit" in lower or "station approaches" in lower:
        return "TRAIN_STATION"
    if "mall" in lower or "kanyon" in lower or "fashion" in lower:
        return "MALL"
    if "square" in lower:
        return "SQUARE"
    if "bike lane" in lower or "bike" in lower:
        return "BIKE_LANE"
    if "bridge" in lower:
        return "BRIDGE"
    if "park" in lower or "safari" in lower:
        return "PARK"
    if "beach" in lower or "boardwalk" in lower or "tayelet" in lower or "promenade" in lower:
        if "beach" in lower or "boardwalk" in lower or "tayelet" in lower:
            return "BEACH"
        return "PROMENADE"
    if "walkway" in lower or "spine" in lower or "axis" in lower or "plaza" in lower:
        return "WALKWAY"
    if "street" in lower or "boulevard" in lower or "blvd" in lower or "road" in lower:
        return "STREET"
    if "entrance" in lower or "junction" in lower or "segment" in lower:
        return "WALKWAY"
    return "STREET"


def _slug_words(name: str) -> list[str]:
    cleaned = re.sub(r"[^\w\s]", " ", name)
    return [w for w in cleaned.split() if w]


def generate_chokepoint_code(name: str, used: set[str]) -> str:
    """Derive a short unique slug per city from the site name."""
    words = _slug_words(name)
    candidates: list[str] = []
    if words:
        candidates.append("".join(w[0] for w in words[:4]).upper()[:4])
        candidates.append(words[0][:3].upper())
        if len(words) > 1:
            candidates.append((words[0][:2] + words[1][:2]).upper())
        candidates.append("".join(w[:2] for w in words[:3]).upper()[:4])
    candidates.append(re.sub(r"[^A-Z0-9]", "", name.upper())[:4] or "SITE")
    for base in candidates:
        code = base[:4] if len(base) >= 3 else base.ljust(3, "X")
        if code not in used:
            used.add(code)
            return code
        for i in range(2, 100):
            suffix = f"{code[:3]}{i}"[:4]
            if suffix not in used:
                used.add(suffix)
                return suffix
    raise RuntimeError(f"Unable to generate unique code for {name!r}")


def _random_installation_dates(rng: random.Random, seed_date: date) -> tuple[date, date]:
    months_ago = rng.randint(12, 18)
    install = seed_date - timedelta(days=months_ago * 30)
    max_inspection_offset = min(90, (seed_date - install).days)
    inspection_offset = rng.randint(0, max(0, max_inspection_offset))
    last_inspection = install + timedelta(days=inspection_offset)
    return install, last_inspection


def build_registry(*, seed: int = 42, seed_date: date | None = None) -> dict:
    """Build the full infrastructure registry document."""
    rng = random.Random(seed)
    reference_date = seed_date or date.today()
    legacy = _load_legacy_chokepoints()
    cities_out: list[dict] = []

    for city_name, sites in legacy.items():
        code = CITY_CODES.get(city_name)
        if code is None:
            raise ValueError(f"No city code mapping for {city_name!r}")
        used_codes: set[str] = set()
        chokepoints: list[dict] = []
        for site_name in sites:
            place_type = classify_place_type(site_name)
            tier = PLACE_TYPE_TIERS[place_type]
            lo, hi = TIER_RANGES[tier]
            tile_count = rng.randint(lo, hi)
            tile_instances = []
            for _ in range(tile_count):
                install, inspection = _random_installation_dates(rng, reference_date)
                tile_instances.append(
                    {
                        "tileId": str(uuid.uuid4()),
                        "manufacturer": rng.choice(MANUFACTURERS),
                        "size": rng.choice(SIZE_POOL),
                        "color": rng.choice(COLOR_POOL),
                        "installationDate": install.isoformat(),
                        "lastInspectionDate": inspection.isoformat(),
                        "isActive": True,
                    }
                )
            chokepoints.append(
                {
                    "code": generate_chokepoint_code(site_name, used_codes),
                    "name": site_name,
                    "placeType": place_type,
                    "tileInstances": tile_instances,
                }
            )
        cities_out.append({"code": code, "name": city_name, "chokepoints": chokepoints})

    return {"manufacturers": list(MANUFACTURERS), "cities": cities_out}


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate infrastructure-registry.json")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("db-init/src/main/resources/data/infrastructure-registry.json"),
        help="Output path for registry JSON",
    )
    parser.add_argument("--seed", type=int, default=42, help="RNG seed for reproducible output")
    args = parser.parse_args()

    registry = build_registry(seed=args.seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(registry, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    tile_count = sum(
        len(cp["tileInstances"])
        for city in registry["cities"]
        for cp in city["chokepoints"]
    )
    chokepoint_count = sum(len(city["chokepoints"]) for city in registry["cities"])
    print(
        f"Wrote {args.output}: {len(registry['cities'])} cities, "
        f"{chokepoint_count} chokepoints, {tile_count} tiles"
    )


if __name__ == "__main__":
    main()
