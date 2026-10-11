import json
import re
import sys
import urllib.parse
import urllib.request

UA = {"User-Agent": "IlMioGiardinoAppBuilder/1.0 (offline garden app asset fetch)"}
API = "https://commons.wikimedia.org/w/api.php"

SPECIES = [
    ("Rosa", "Rosa canina flower"),
    ("Geranio", "Pelargonium zonale"),
    ("Basilico", "Ocimum basilicum"),
    ("Pomodoro", "Solanum lycopersicum plant"),
    ("Salvia", "Salvia officinalis"),
    ("Rosmarino", "Salvia rosmarinus"),
    ("Lavanda", "Lavandula angustifolia"),
    ("Menta", "Mentha spicata"),
    ("Orchidea", "Phalaenopsis flower"),
    ("Ficus", "Ficus benjamina"),
    ("Monstera", "Monstera deliciosa"),
    ("Sansevieria", "Sansevieria trifasciata"),
    ("Aloe", "Aloe vera"),
    ("Oleandro", "Nerium oleander"),
    ("Camelia", "Camelia flower"),
    ("Azalea", "Rhododendron simsii flower"),
    ("Ortensia", "Hydrangea macrophylla"),
    ("Begonia", "Begonia flower"),
    ("Viola", "Viola odorata"),
    ("Girasole", "Helianthus annuus"),
    ("Margherita", "Leucanthemum vulgare"),
    ("Tulipano", "Tulipa flower"),
    ("Giglio", "Lilium flower"),
    ("Peperoncino", "Capsicum annuum plant"),
    ("Zucchina", "Cucurbita pepo plant"),
    ("Insalata", "Lactuca sativa"),
    ("Fragola", "Fragaria ananassa"),
    ("Vite", "Vitis vinifera"),
    ("Limone", "Citrus limon"),
    ("Olivo", "Olea europaea"),
    ("Ciliegio", "Prunus avium flower"),
    ("Pesco", "Prunus persica flower"),
    ("Melo", "Malus domestica flower"),
    ("Ciclamino", "Cyclamen persicum"),
    ("Primula", "Primula vulgaris"),
    ("Petunia", "Petunia flower"),
    ("Tagete", "Tagetes patula"),
    ("Zinnia", "Zinnia elegans"),
    ("Papavero", "Papaver rhoeas"),
    ("Cactus", "Opuntia ficus-indica"),
]


def search_images(term, want=2):
    params = {
        "action": "query",
        "format": "json",
        "generator": "search",
        "gsrsearch": term,
        "gsrnamespace": "6",
        "gsrlimit": "12",
        "prop": "imageinfo",
        "iiprop": "url|mime",
        "iiurlwidth": "640",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=UA)
    data = json.load(urllib.request.urlopen(req, timeout=30))
    pages = data.get("query", {}).get("pages", {})
    out = []
    for p in pages.values():
        ii = (p.get("imageinfo") or [{}])[0]
        mime = ii.get("mime", "")
        if mime in ("image/jpeg", "image/png") and ii.get("thumburl"):
            out.append(ii["thumburl"])
        if len(out) >= want:
            break
    return out


def main():
    assets = sys.argv[1]
    index = {}
    for name, term in SPECIES:
        try:
            urls = search_images(term)
        except Exception as e:
            print(name, "SEARCH FAIL", e, file=sys.stderr)
            continue
        files = []
        for i, u in enumerate(urls, 1):
            fn = f"species/{name.lower()}_{i}.jpg"
            try:
                req = urllib.request.Request(u, headers=UA)
                d = urllib.request.urlopen(req, timeout=30).read()
                if len(d) < 4000:
                    continue
                with open(f"{assets}/{fn}", "wb") as f:
                    f.write(d)
                files.append(fn)
            except Exception as e:
                print(name, u, "DL FAIL", e, file=sys.stderr)
        if files:
            index[name] = files
        print(name, len(files))
    with open(f"{assets}/species_index.json", "w") as f:
        json.dump(index, f, ensure_ascii=False, indent=1)
    print("TOTAL", sum(len(v) for v in index.values()), "images,", len(index), "species")


if __name__ == "__main__":
    main()
