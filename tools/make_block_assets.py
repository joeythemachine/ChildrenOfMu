"""
Generates the JSON files (blockstates, models, loot, recipes, tags) for the mod's simple blocks.

    python tools/make_block_assets.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
NS = "childrenofmu"

# name -> (texture, full block it's made from)
STAIRS = {"amethyst_stairs": ("minecraft:block/amethyst_block", "minecraft:amethyst_block")}
SLABS = {"amethyst_slab": ("minecraft:block/amethyst_block", "minecraft:amethyst_block")}
# Plain cubes that borrow a vanilla texture and drop themselves.
CUBES = {
    "inlaid_emerald": "minecraft:block/emerald_block",
    "inlaid_gold": "minecraft:block/gold_block",
    "inlaid_lapis": "minecraft:block/lapis_block",
}
# Which tool tier is needed to get them back (matches the vanilla block).
NEEDS_IRON = ["inlaid_emerald", "inlaid_gold"]
NEEDS_STONE = ["inlaid_lapis"]


def write(path, data):
    path = os.path.join(RES, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def stairs_blockstate(name):
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y0 in base.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = f"{NS}:block/{name}" + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                if half == "bottom":
                    y = y0 - 90 if shape in ("inner_left", "outer_left") else y0
                else:
                    y = y0 + 90 if shape in ("inner_right", "outer_right") else y0
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y % 360:
                    v["y"] = y % 360
                if "x" in v or "y" in v:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def main():
    for name, (tex, full) in STAIRS.items():
        t = {"bottom": tex, "top": tex, "side": tex}
        write(f"assets/{NS}/blockstates/{name}.json", stairs_blockstate(name))
        write(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/stairs", "textures": t})
        write(f"assets/{NS}/models/block/{name}_inner.json", {"parent": "minecraft:block/inner_stairs", "textures": t})
        write(f"assets/{NS}/models/block/{name}_outer.json", {"parent": "minecraft:block/outer_stairs", "textures": t})
        write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        write(f"data/{NS}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        write(f"data/{NS}/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "building",
            "pattern": ["#  ", "## ", "###"], "key": {"#": {"item": full}},
            "result": {"id": f"{NS}:{name}", "count": 4}})
        write(f"data/{NS}/recipe/{name}_from_stonecutting.json", {
            "type": "minecraft:stonecutting", "ingredient": {"item": full},
            "result": {"id": f"{NS}:{name}", "count": 1}})

    for name, (tex, full) in SLABS.items():
        write(f"assets/{NS}/blockstates/{name}.json", {"variants": {
            "type=bottom": {"model": f"{NS}:block/{name}"},
            "type=top": {"model": f"{NS}:block/{name}_top"},
            "type=double": {"model": full.replace(":", ":block/")},
        }})
        t = {"bottom": tex, "top": tex, "side": tex}
        write(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/slab", "textures": t})
        write(f"assets/{NS}/models/block/{name}_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
        write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        write(f"data/{NS}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{
                "type": "minecraft:item", "name": f"{NS}:{name}",
                "functions": [
                    {"function": "minecraft:set_count", "count": 2, "add": False,
                     "conditions": [{"condition": "minecraft:block_state_property", "block": f"{NS}:{name}",
                                     "properties": {"type": "double"}}]},
                    {"function": "minecraft:explosion_decay"}]}]}]})
        write(f"data/{NS}/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "building",
            "pattern": ["###"], "key": {"#": {"item": full}},
            "result": {"id": f"{NS}:{name}", "count": 6}})
        write(f"data/{NS}/recipe/{name}_from_stonecutting.json", {
            "type": "minecraft:stonecutting", "ingredient": {"item": full},
            "result": {"id": f"{NS}:{name}", "count": 2}})

    for name, tex in CUBES.items():
        write(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
        write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        write(f"data/{NS}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    write("data/minecraft/tags/block/needs_iron_tool.json", {"values": [f"{NS}:{n}" for n in NEEDS_IRON]})
    write("data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{NS}:{n}" for n in NEEDS_STONE]})
    every = [f"{NS}:{n}" for n in list(STAIRS) + list(SLABS) + list(CUBES)]
    write("data/minecraft/tags/block/mineable/pickaxe.json", {"values": every})
    write("data/minecraft/tags/block/stairs.json", {"values": [f"{NS}:{n}" for n in STAIRS]})
    write("data/minecraft/tags/block/slabs.json", {"values": [f"{NS}:{n}" for n in SLABS]})
    write("data/minecraft/tags/item/stairs.json", {"values": [f"{NS}:{n}" for n in STAIRS]})
    write("data/minecraft/tags/item/slabs.json", {"values": [f"{NS}:{n}" for n in SLABS]})
    print("Block assets written")


if __name__ == "__main__":
    main()
