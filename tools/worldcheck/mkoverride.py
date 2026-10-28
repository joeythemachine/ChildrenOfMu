"""World edits -> template overrides. Usage: mkoverride.py name ox oy oz rot  (world = f(template))."""
import json, sys, re
name, ox, oy, oz, rot = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
sig = json.load(open("sig1.json"))
SIZE = int(sys.argv[6])
# rot "ccw": world = (ox + tz, oy + ty, oz - tx)  (template east -> world north)
W2T_DIR = {"north": "east", "east": "south", "south": "west", "west": "north"}
def to_template(p):
    x, y, z = p
    return (oz - z, y - oy, x - ox)
def parse(n):
    m = re.match(r"([^\[]+)(?:\[(.*)\])?$", n)
    props = dict(kv.split("=") for kv in m.group(2).split(",")) if m.group(2) else {}
    return m.group(1), props
def rot_props(props):
    out = {}
    for k, v in props.items():
        if k == "facing" and v in W2T_DIR:
            out[k] = W2T_DIR[v]
        elif k == "axis" and v in ("x", "z"):
            out[k] = "z" if v == "x" else "x"
        elif k == "rotation":
            out[k] = str((int(v) + 4) % 16)
        elif k in W2T_DIR:
            out[W2T_DIR[k]] = v
        elif k.startswith("connect_") and k[8:] in W2T_DIR:
            out["connect_" + W2T_DIR[k[8:]]] = v
        else:
            out[k] = v
    return out
over = []
for p, a, b in sig:
    t = to_template(p)
    if not (0 <= t[0] < SIZE and 0 <= t[2] < SIZE and t[1] >= 0):
        continue
    nm, props = parse(b)
    over.append([list(t), nm, rot_props(props)])
json.dump(over, open(f"/home/claude/ChildrenOfMu/tools/overrides/{name}.json", "w"), indent=0)
print(len(over), "overrides")
