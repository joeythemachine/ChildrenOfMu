"""Compare the current save of the city with the snapshot: python3 diff.py <region.mca>"""
import anvil, json, gzip, sys
from collections import defaultdict
base = {tuple(map(int, k.split(','))): v for k, v in json.load(gzip.open("city_-3376_1136_base.json.gz", "rt")).items()}
now = anvil.blocks(sys.argv[1], -3376-90, -3376+90, 1136-90, 1136+90, 30, 160)
changes = []
for p in set(base) | set(now):
    a, b = base.get(p, 'air'), now.get(p, 'air')
    if a != b:
        changes.append((p, a, b))
print(len(changes), "blocks changed")
# group into clusters (8-block cells)
groups = defaultdict(list)
for p, a, b in changes:
    groups[(p[0]//8, p[1]//8, p[2]//8)].append((p, a, b))
for g, lst in sorted(groups.items(), key=lambda kv: -len(kv[1])):
    xs = [p[0] for p, _, _ in lst]; ys = [p[1] for p, _, _ in lst]; zs = [p[2] for p, _, _ in lst]
    print(f"--- {len(lst)} around x {min(xs)}..{max(xs)} y {min(ys)}..{max(ys)} z {min(zs)}..{max(zs)}")
    for p, a, b in sorted(lst)[:40]:
        print("  ", p, a.replace('minecraft:', ''), "->", b.replace('minecraft:', ''))
