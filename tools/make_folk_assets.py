"""
Generates the walking folk (Rat folk, Cat folk, Lilbuns): their geometry and textures, from the character
reference sheets in "CoM Reference Images".

    python tools/make_folk_assets.py

Writes:
    src/main/java/com/childrenofmu/client/model/FolkShapes.java
    src/main/resources/assets/childrenofmu/textures/entity/folk/<race>_<variant>.png

Every race is modelled at the same design size (24 units = 1.5 blocks tall, feet at y = 24, y pointing down,
face toward -z) and shrunk in game by the race's scale (FolkRace.java), so textures stay crisp at any size.
Requires Pillow (pip install pillow).
"""
import importlib.util
import math
import os
import random
import zlib

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "com", "childrenofmu", "client", "model", "FolkShapes.java")
TEX_OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "childrenofmu", "textures", "entity", "folk")

_spec = importlib.util.spec_from_file_location("ithk_assets", os.path.join(HERE, "make_ithk_assets.py"))
assets = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(assets)
face_pixels, pack = assets.face_pixels, assets.pack

TEX_W = 64
SCALE = 2          # image pixels per texture unit
CLEAR = (0, 0, 0, 0)


# ------------------------------------------------------------------ helpers
def clamp(c):
    return max(0, min(255, int(c)))


def shade(rgb, delta):
    return tuple(clamp(c + delta) for c in rgb[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noise(*key):
    """A repeatable random number in [0, 1) for any key."""
    return random.Random(zlib.crc32(repr(key).encode())).random()   # stable from run to run


def rgba(rgb, a=255):
    return (rgb[0], rgb[1], rgb[2], a)


FACE_LIGHT = {"up": -10, "down": 10, "west": -4, "east": -4, "north": 0, "south": -6}   # model y points down


def box(x, y, z, w, h, d, tag=None):
    assert all(float(v).is_integer() for v in (w, h, d)), "box sizes must be whole numbers"
    return {"x": x, "y": y, "z": z, "w": int(w), "h": int(h), "d": int(d), "tag": tag}


def part(name, pivot, boxes, rot=(0, 0, 0), parent=None, only=None):
    """only: the variants that have this part (None = every variant)."""
    return {"name": name, "pivot": pivot, "rot": rot, "boxes": boxes, "parent": parent, "only": only}


def texel(tx, ty):
    """A per-texel random number, for grain."""
    return noise("t", tx, ty)


# ------------------------------------------------------------------ the three bodies
def rat_parts():
    return [
        part("head", (0, 9, 0), [box(-5, -9, -4, 10, 9, 8, "head"), box(-2, -4, -7, 4, 3, 3, "snout"),
                                 box(-1, -4, -8, 2, 2, 1, "nose")]),
        part("left_ear", (3.5, 0.5, 0), [box(-2, -4, -0.5, 4, 4, 1, "ear")], rot=(0, -15, 38), parent="head_ears"),
        part("right_ear", (-3.5, 0.5, 0), [box(-2, -4, -0.5, 4, 4, 1, "ear")], rot=(0, 15, -38), parent="head_ears"),
        part("whiskers", (0, -2.5, -6), [box(-7, -1, 0, 14, 2, 0, "whiskers")], parent="head"),
        part("body", (0, 9, 0), [box(-3.5, 0, -2, 7, 9, 4, "body")]),
        part("right_arm", (-4.5, 10, 0), [box(-1, -1, -1, 2, 8, 2, "arm")]),
        part("left_arm", (4.5, 10, 0), [box(-1, -1, -1, 2, 8, 2, "arm")]),
        part("right_leg", (-2, 18, 0), [box(-1.5, 0, -1.5, 3, 6, 3, "leg"), box(-1.5, 5, -3.5, 3, 1, 2, "toes")]),
        part("left_leg", (2, 18, 0), [box(-1.5, 0, -1.5, 3, 6, 3, "leg"), box(-1.5, 5, -3.5, 3, 1, 2, "toes")]),
        part("tail", (0, 16, 2), [box(-0.5, -0.5, 0, 1, 1, 6, "tail")], rot=(35, 0, 0)),
        part("tail_tip", (0, 0, 6), [box(-0.5, -0.5, 0, 1, 1, 5, "tail")], rot=(-70, 0, 0), parent="tail"),
        # City jobs. Sweepers tie a kerchief over their heads; the post rat wears a peaked cap and a satchel; the
        # accountant a green eyeshade; the engineer (mutated, electrically modified) brass goggles and an antenna.
        part("kerchief", (0, 0, 0), [box(-5.5, -10, -4.5, 11, 2, 9, "kerchief"), box(-1, -9, 4, 2, 3, 1, "kerchief")],
             parent="head", only=["sweeper"]),
        part("post_cap", (0, 0, 0), [box(-5.5, -10.5, -4.5, 11, 2, 9, "postcap"), box(-4, -9, -7, 8, 1, 3, "postcap")],
             parent="head", only=["postrat"]),
        part("satchel", (3.5, 1, 0), [box(0, 3, -2, 2, 4, 4, "satchel")], parent="body", only=["postrat"]),
        part("eyeshade", (0, 0, 0), [box(-5.5, -9.5, -4.5, 11, 1, 9, "visorband"), box(-4.5, -9.5, -8, 9, 1, 4, "visor")],
             parent="head", only=["accountant"]),
        part("goggles", (0, 0, 0), [box(-5, -7, -4.5, 10, 2, 1, "goggles")], parent="head", only=["engineer"]),
        part("antenna", (0, 0, 0), [box(1.5, -13, -0.5, 1, 4, 1, "antenna"), box(1, -14, -1, 2, 1, 2, "antenna_tip")],
             parent="head", only=["engineer"]),
    ]


def cat_parts():
    return [
        part("head", (0, 8, 0), [box(-5, -9, -4, 10, 9, 8, "head"), box(-2, -4, -6, 4, 3, 2, "snout")]),
        part("left_ear", (3.5, -9, -0.5), [box(-1.5, -2, -0.5, 3, 2, 1, "ear"), box(-0.5, -4, -0.5, 1, 2, 1, "ear")],
             rot=(0, 0, 8), parent="head"),
        part("right_ear", (-3.5, -9, -0.5), [box(-1.5, -2, -0.5, 3, 2, 1, "ear"), box(-0.5, -4, -0.5, 1, 2, 1, "ear")],
             rot=(0, 0, -8), parent="head"),
        part("body", (0, 8, 0), [box(-4, 0, -2, 8, 9, 4, "body")]),
        part("right_arm", (-5.5, 9, 0), [box(-1.5, -1, -1.5, 3, 8, 3, "arm")]),
        part("left_arm", (5.5, 9, 0), [box(-1.5, -1, -1.5, 3, 8, 3, "arm")]),
        part("right_leg", (-2, 17, 0), [box(-1.5, 0, -1.5, 3, 7, 3, "leg"), box(-1.5, 6, -2.5, 3, 1, 1, "toes")]),
        part("left_leg", (2, 17, 0), [box(-1.5, 0, -1.5, 3, 7, 3, "leg"), box(-1.5, 6, -2.5, 3, 1, 1, "toes")]),
        part("tail", (0, 15.5, 2), [box(-1, -1, 0, 2, 2, 8, "tail")], rot=(-35, 0, 0)),
        part("cape", (0, 0.5, 2), [box(-4.5, 0, 0, 9, 13, 1, "cape")], rot=(8, 0, 0), parent="body", only=["hunter"]),
        part("cap", (0, 0, 0), [box(-5.5, -10, -4.5, 11, 3, 9, "cap")], parent="head", only=["fellmonger"]),
        # The law: a cop's tall custodian helmet, a city guard's iron kettle hat.
        part("helmet", (0, 0, 0), [box(-5.5, -11, -4.5, 11, 3, 9, "helmet"), box(-4.5, -14, -3.5, 9, 3, 7, "helmet"),
                                   box(-1, -15, -1, 2, 1, 2, "helmet_knob")], parent="head", only=["cop"]),
        part("kettle", (0, 0, 0), [box(-7.5, -10, -6.5, 15, 1, 13, "kettle"), box(-5.5, -13, -4.5, 11, 3, 9, "kettle")],
             parent="head", only=["guard", "archer"]),
        # The Great Gar: a vast belly in a purple robe, an ermine mantle, fat cheeks and a little gold crown.
        part("belly", (0, 0, 0), [box(-6, 0.9, -5, 12, 7, 6, "belly")], parent="body", only=["gar"]),
        part("mantle", (0, 0, 0), [box(-6.5, -0.5, -3.5, 13, 2, 6, "mantle")], parent="body", only=["gar"]),
        part("jowls", (0, 0, 0), [box(-6.2, -4.6, -3.2, 2, 4, 5, "jowls"), box(4.2, -4.6, -3.2, 2, 4, 5, "jowls")],
             parent="head", only=["gar"]),
        part("crown", (0, 0, 0), [box(-3, -12, -2, 6, 3, 4, "crown")], parent="head", only=["gar"]),
    ]


def dog_parts():
    return [
        part("head", (0, 8, 0), [box(-5, -9, -4, 10, 9, 8, "head"), box(-2, -4, -7, 4, 3, 3, "snout")]),
        # Spaniel: long floppy ears. Husky: tall pointed ears.
        part("left_ear", (5, -8, 0), [box(0, 0, -2, 2, 10, 4, "flop")], rot=(0, 0, -8), parent="head", only=['monk', 'weaver', 'tailor', 'shepherd', 'tanner', 'cook', 'server', 'teacher']),
        part("right_ear", (-5, -8, 0), [box(-2, 0, -2, 2, 10, 4, "flop")], rot=(0, 0, 8), parent="head", only=['monk', 'weaver', 'tailor', 'shepherd', 'tanner', 'cook', 'server', 'teacher']),
        part("left_point", (3.5, -9, -0.5), [box(-1.5, -3, -0.5, 3, 3, 1, "ear"), box(-0.5, -5, -0.5, 1, 2, 1, "ear")],
             rot=(0, 0, 6), parent="head", only=['husky_monk', 'archer', 'dyer', 'smith', 'woodcutter', 'butcher', 'musician', 'biter', 'high_guard']),
        part("right_point", (-3.5, -9, -0.5), [box(-1.5, -3, -0.5, 3, 3, 1, "ear"), box(-0.5, -5, -0.5, 1, 2, 1, "ear")],
             rot=(0, 0, -6), parent="head", only=['husky_monk', 'archer', 'dyer', 'smith', 'woodcutter', 'butcher', 'musician', 'biter', 'high_guard']),
        # Feudal Japan: the samurai's helmet with its gold crest, the stablemaster's straw hat, the archer's quiver.
        part("kabuto", (0, 0, 0), [box(-5.5, -10.5, -4.5, 11, 3, 9, "kabuto"), box(-6, -8.5, 0, 12, 3, 5, "kabuto"),
                                   box(-3, -14, -4.6, 6, 4, 0, "crest")], parent="head", only=["samurai"]),
        part("kasa", (0, 0, 0), [box(-7, -10, -7, 14, 1, 14, "kasa"), box(-4.5, -12, -4.5, 9, 2, 9, "kasa"),
                                 box(-2, -13, -2, 4, 1, 4, "kasa")], parent="head", only=["stablemaster", "pikeman"]),
        part("quiver", (1.5, 0, 2.5), [box(-1.5, -3, 0, 3, 10, 2, "quiver")], rot=(0, 0, 22), parent="body",
             only=["archer"]),
        part("body", (0, 8, 0), [box(-4, 0, -2.5, 8, 9, 5, "body")]),
        part("skirt", (0, 9, 0), [box(-4.5, 0, -3, 9, 6, 6, "skirt")], parent="body"),
        part("right_arm", (-5.5, 9, 0), [box(-1.5, -1, -1.5, 3, 8, 3, "arm")]),
        part("left_arm", (5.5, 9, 0), [box(-1.5, -1, -1.5, 3, 8, 3, "arm")]),
        part("right_sode", (0, 0, 0), [box(-2.5, -1.5, -2, 4, 4, 4, "sode")], parent="right_arm",
             only=["samurai", "high_guard"]),
        part("left_sode", (0, 0, 0), [box(-1.5, -1.5, -2, 4, 4, 4, "sode")], parent="left_arm",
             only=["samurai", "high_guard"]),
        # The Whipped (the one in the tower): an iron cauldron over his head, handles at its sides, torn strips of
        # bandage hanging below its rim and from his hands.
        part("cauldron", (0, 0, 0), [box(-6, -11, -5, 12, 8, 10, "cauldron"), box(-6.5, -4, -5.5, 13, 1, 11, "rim"),
                                     box(-7.5, -9, -1, 1, 3, 2, "rim"), box(6.5, -9, -1, 1, 3, 2, "rim"),
                                     box(-4.5, -3, -5.6, 1, 5, 1, "strip"), box(3.5, -3, -5.6, 1, 6, 1, "strip"),
                                     box(-6, -3, 2, 1, 4, 1, "strip"), box(5, -3, -1, 1, 7, 1, "strip")],
             parent="head", only=["bandaged"]),
        part("right_strip", (0, 0, 0), [box(-1, 6, -0.5, 1, 4, 1, "strip")], parent="right_arm", only=["bandaged"]),
        part("left_strip", (0, 0, 0), [box(0, 6, -0.5, 1, 3, 1, "strip")], parent="left_arm", only=["bandaged"]),
        # The High Guard of the dark tower: a sashimono on his back, a pole and crossbar flying the tower's banner.
        # (Stood clear of the head's back, the flag a thin board, not a zero-thick plane: no z-fighting.)
        part("sashimono", (0, 0, 0), [box(-0.5, -15, 4.5, 1, 22, 1, "pole"), box(-3.5, -15, 4.5, 7, 1, 1, "pole"),
                                      box(-3, -14, 5.5, 6, 10, 1, "flag")], parent="body", only=["high_guard"]),
        part("right_leg", (-2, 17, 0), [box(-1.5, 0, -1.5, 3, 7, 3, "leg"), box(-1.5, 6, -2.5, 3, 1, 1, "toes")]),
        part("left_leg", (2, 17, 0), [box(-1.5, 0, -1.5, 3, 7, 3, "leg"), box(-1.5, 6, -2.5, 3, 1, 1, "toes")]),
        part("tail", (0, 15, 3), [box(-1, -1, 0, 2, 2, 4, "tail")], rot=(-30, 0, 0), only=['monk', 'stablemaster', 'weaver', 'tailor', 'shepherd', 'tanner', 'cook', 'server', 'teacher']),
        part("tail_bushy", (0, 15, 3), [box(-1.5, -1.5, 0, 3, 3, 7, "tail")], rot=(-55, 0, 0),
             only=['husky_monk', 'samurai', 'archer', 'dyer', 'smith', 'woodcutter', 'butcher', 'musician', 'pikeman', 'biter', 'high_guard']),
    ]


def frog_parts():
    return [
        part("head", (0, 9, 0), [box(-6, -6, -5, 12, 6, 10, "head")]),
        part("left_eye", (0, 0, 0), [box(3.5, -9, -3.5, 3, 3, 3, "eye")], parent="head"),
        part("right_eye", (0, 0, 0), [box(-6.5, -9, -3.5, 3, 3, 3, "eye")], parent="head"),
        # The Chitinier is the town's fancy insect chef: a tall puffed toque. His Cooks wear short ones.
        part("toque", (0, 0, 0), [box(-3, -13, -3, 6, 6, 6, "toque"), box(-3.5, -7, -3.5, 7, 1, 7, "toque_band")],
             parent="head", only=["chitinier"]),
        part("chef_cap", (0, 0, 0), [box(-2.5, -10, -2.5, 5, 3, 5, "toque"), box(-3, -7, -3, 6, 1, 6, "toque_band")],
             parent="head", only=["cook"]),
        part("flat_cap", (0, 0, 0), [box(-3, -7.5, -3.5, 6, 2, 7, "flatcap"), box(-3, -6.5, -5.5, 6, 1, 2, "flatcap")],
             parent="head", only=["fishmonger"]),
        part("body", (0, 9, 0), [box(-3.5, 0, -2, 7, 8, 4, "body")]),
        part("glider", (0, 0.5, 2.2), [box(-9, -1, 0, 18, 10, 0, "glider")], rot=(10, 0, 0), parent="body",
             only=["jumper"]),
        part("satchel", (0, 0, 0), [box(3.5, 4, -1.5, 2, 3, 3, "satchel")], parent="body",
             only=["jumper", "guard"]),
        # New roles: the Fisher's bucket hat, the Treasurer's top hat, the Plumber's cap, the Elder's robe and stick.
        part("bucket_hat", (0, 0, 0), [box(-3, -8.5, -3.5, 6, 2, 7, "bucket_hat"), box(-4, -6.5, -4.5, 8, 1, 9, "bucket_hat")],
             parent="head", only=["fisher"]),
        part("top_hat", (0, 0, 0), [box(-2.5, -11, -2.5, 5, 5, 5, "top_hat"), box(-3.5, -7, -3.5, 7, 1, 7, "top_hat")],
             parent="head", only=["treasurer"]),
        part("cap", (0, 0, 0), [box(-3, -7.5, -3, 6, 2, 6, "cap"), box(-3, -6.5, -6, 6, 1, 3, "cap")],
             parent="head", only=["plumber"]),
        part("robe", (0, 0, 0), [box(-4, 6, -2.5, 8, 7, 5, "robe")], parent="body", only=["elder", "schoolmarm"]),
        part("bonnet", (0, 0, 0), [box(-3, -8, -3, 6, 2, 7, "bonnet"), box(-3.5, -7, -4, 7, 1, 1, "bonnet_brim")],
             parent="head", only=["schoolmarm"]),
        part("pointer", (0, 6, -1.5), [box(-0.5, -1, -0.5, 1, 9, 1, "pointer")], rot=(-70, 0, 0), parent="left_arm",
             only=["schoolmarm"]),
        part("stick", (0, 6, -1.5), [box(-0.5, -3, -0.5, 1, 11, 1, "stick")], rot=(-6, 0, 0), parent="right_arm",
             only=["elder"]),
        part("tadpole_tail", (0, 6, 2), [box(-0.5, -0.5, 0, 1, 1, 3, "tadpole")], rot=(20, 0, 0), parent="body",
             only=["froglet"]),
        part("right_arm", (-4.5, 10, 0), [box(-1, -1, -1, 2, 7, 2, "arm")]),
        part("left_arm", (4.5, 10, 0), [box(-1, -1, -1, 2, 7, 2, "arm")]),
        part("right_leg", (-2.2, 17, 0), [box(-2, 0, -2, 4, 6, 4, "leg"), box(-2.5, 6, -4, 5, 1, 4, "toes")]),
        part("left_leg", (2.2, 17, 0), [box(-2, 0, -2, 4, 6, 4, "leg"), box(-2.5, 6, -4, 5, 1, 4, "toes")]),
    ]


def goxa_parts():
    """Tall (2.25 blocks in game), slender and long-limbed."""
    return [
        part("head", (0, 7, 0), [box(-3, -7, -3, 6, 7, 6, "head")]),
        part("crest", (0, -6, 0.5), [box(-4, -6, 0, 8, 6, 0, "crest")], parent="head", only=["havu"]),
        part("hat", (0, -7, 0.5), [box(-3.5, -2, -3.5, 7, 2, 7, "hat"), box(-2.5, -5, -2.5, 5, 3, 5, "hat"),
                                   box(-1.5, -8, -1.5, 3, 3, 3, "hat"), box(-0.5, -11, -0.5, 1, 3, 1, "hat")],
             rot=(-10, 0, 0), parent="head", only=["seminata"]),
        part("body", (0, 7, 0), [box(-2.5, 0, -1.5, 5, 9, 3, "body")]),
        part("skirt", (0, 9, 0), [box(-3, 0, -2, 6, 7, 4, "skirt")], parent="body"),
        part("right_arm", (-3.5, 8, 0), [box(-1, -1, -1, 2, 13, 2, "arm")]),
        part("left_arm", (3.5, 8, 0), [box(-1, -1, -1, 2, 13, 2, "arm")]),
        part("right_wing", (-0.5, 2, 0.6), [box(-4, 0, 0, 4, 14, 0, "wing")], rot=(0, -15, 0), parent="right_arm",
             only=["havu"]),
        part("left_wing", (0.5, 2, 0.6), [box(0, 0, 0, 4, 14, 0, "wing")], rot=(0, 15, 0), parent="left_arm",
             only=["havu"]),
        part("staff", (0, 11, -1), [box(-0.5, -12, -0.5, 1, 22, 1, "staff"), box(-1.5, -15, -0.5, 3, 3, 1, "staff")],
             parent="right_arm", only=["seminata"]),
        part("right_leg", (-1.2, 15, 0), [box(-1, 0, -1, 2, 9, 2, "leg"), box(-1, 8, -2, 2, 1, 1, "toes")]),
        part("left_leg", (1.2, 15, 0), [box(-1, 0, -1, 2, 9, 2, "leg"), box(-1, 8, -2, 2, 1, 1, "toes")]),
    ]


def bat_parts():
    return [
        part("head", (0, 9, 0), [box(-5, -9, -4, 10, 9, 8, "head"), box(-2, -4, -6, 4, 3, 2, "snout")]),
        part("left_ear", (3.5, -8.5, 0), [box(-2, -3, -0.5, 4, 3, 1, "ear"), box(-1, -5, -0.5, 2, 2, 1, "ear")],
             rot=(0, 0, 26), parent="head"),
        part("right_ear", (-3.5, -8.5, 0), [box(-2, -3, -0.5, 4, 3, 1, "ear"), box(-1, -5, -0.5, 2, 2, 1, "ear")],
             rot=(0, 0, -26), parent="head"),
        part("body", (0, 9, 0), [box(-3.5, 0, -2, 7, 9, 4, "body")]),
        part("right_arm", (-4.5, 10, 0), [box(-1, -1, -1, 2, 8, 2, "arm")]),
        part("left_arm", (4.5, 10, 0), [box(-1, -1, -1, 2, 8, 2, "arm")]),
        # Wing membranes hang from the arms, angled back.
        part("right_wing", (-0.5, 0, 1), [box(-6, -1, 0, 6, 11, 0, "wing")], rot=(0, -25, 0), parent="right_arm"),
        part("left_wing", (0.5, 0, 1), [box(0, -1, 0, 6, 11, 0, "wing")], rot=(0, 25, 0), parent="left_arm"),
        part("right_leg", (-2, 18, 0), [box(-1.5, 0, -1.5, 3, 6, 3, "leg"), box(-1.5, 5, -3.5, 3, 1, 2, "toes")]),
        part("left_leg", (2, 18, 0), [box(-1.5, 0, -1.5, 3, 6, 3, "leg"), box(-1.5, 5, -3.5, 3, 1, 2, "toes")]),
        # Variant extras.
        part("belly", (0, 0, 0), [box(-3, 3, -3, 6, 5, 1, "belly")], parent="body", only=["barman"]),
        part("cigar", (0, 0, 0), [box(1, -2, -9, 1, 1, 3, "cigar")], parent="head", only=["barman"]),
        part("beret", (0, 0, 0), [box(-5.5, -11, -5, 11, 2, 10, "beret"), box(-0.5, -12, -0.5, 1, 1, 1, "beret")],
             parent="head", only=["jazz"]),
        part("sax_case", (0, 1, 2), [box(-2, 0, 0, 4, 9, 2, "case")], rot=(0, 0, 22), parent="body", only=["jazz"]),
        part("feather", (1.5, -9, 0), [box(-1, -6, 0, 3, 6, 0, "feather")], rot=(0, 0, 12), parent="head",
             only=["flapper"]),
        part("fringe", (0, 0, 0), [box(-4, 6, -2.5, 8, 4, 5, "fringe")], parent="body", only=["flapper"]),
        # The bookie: a checked flat cap, and a pencil behind the ear.
        part("bookie_cap", (0, 0, 0), [box(-5.5, -10.5, -4.5, 11, 2, 9, "flatcap"), box(-4.5, -9, -7, 9, 1, 3, "flatcap")],
             parent="head", only=["bookie"]),
        part("pencil", (0, 0, 0), [box(5, -7, -1, 1, 1, 4, "pencil")], parent="head", only=["bookie"]),
    ]


def lilbun_parts():
    return [
        part("head", (0, 15, 0), [box(-3.5, -7, -3, 7, 7, 6, "face"),
                                  box(-7, -10, -7, 14, 4, 14, "brim"),
                                  box(-5, -12, -5, 10, 2, 10, "dome"),
                                  box(-3, -13, -3, 6, 1, 6, "dome")]),
        part("body", (0, 15, 0), [box(-3, 0, -2, 6, 6, 4, "body")]),
        part("right_arm", (-4, 15.5, 0), [box(-1, -0.5, -1, 2, 5, 2, "arm")]),
        part("left_arm", (4, 15.5, 0), [box(-1, -0.5, -1, 2, 5, 2, "arm")]),
        part("right_leg", (-1.5, 21, 0), [box(-1, 0, -1, 2, 3, 2, "leg")]),
        part("left_leg", (1.5, 21, 0), [box(-1, 0, -1, 2, 3, 2, "leg")]),
    ]


# The rat's ears sit on a holder part so they can splay outward from the top of the head.
def with_ear_holder(parts):
    out = list(parts)
    out.insert(1, part("head_ears", (0, -9, 0), [], parent="head"))
    return out


# ------------------------------------------------------------------ painters
def fur(base, tx, ty, var=10):
    if var is None:
        var = 10
    return shade(base, (texel(tx, ty) - 0.5) * 2 * var)


def rat_painter(pal):
    gong = pal.get("gong", False)
    groomed = pal.get("groomed", False)     # the Beautiful Ones: spotless, glossy, no clothes, a silk ribbon

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        skin = pal["skin"]
        c = None
        hat = {"kerchief": pal.get("hat"), "postcap": pal.get("hat"), "visorband": pal.get("hat"),
               "satchel": (124, 82, 46), "visor": (60, 150, 80), "goggles": (176, 132, 56),
               "antenna": (150, 150, 156), "antenna_tip": (120, 255, 150)}
        if tag in hat:
            c = fur(hat[tag], tx, ty, 6)
            if tag == "kerchief" and noise("dot", tx // 2, ty // 2) < 0.18:
                c = (236, 230, 220)                                                # white polka dots
            elif tag == "postcap" and y > -9.2 and abs(x) < 1.2 and face == "north":
                c = (214, 176, 60)                                                 # a brass post badge
            elif tag == "visor":
                c = rgba(fur((60, 150, 80), tx, ty, 6), 200)
                return c                                                           # translucent green eyeshade
            elif tag == "goggles" and face == "north" and abs(abs(x) - 2.3) < 1.1:
                c = (120, 255, 150) if x > 0 else (150, 200, 220)                  # one lens glows green
            elif tag == "antenna_tip":
                return rgba((140, 255, 170))
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "whiskers":
            # Thin pale lines on a clear plane, either side of the snout.
            if abs(x) < 2.6:
                return CLEAR
            row = (ty % (2 * SCALE))
            return rgba(pal["whisker"], 230) if row == 0 and texel(tx, 0) > 0.15 else CLEAR
        if tag == "nose":
            c = pal["pink"]
        elif tag == "ear":
            if abs(x) > 1.4 and y < -3.4:
                return CLEAR                                                       # round off the top corners
            inner = face == "north" and abs(x) < 1.4 and y > -3.4
            c = pal["pink"] if inner else (pal["pink"] if gong else fur(pal["fur"], tx, ty))
        elif tag == "tail":
            c = shade(pal["pink"], -18 if int(z) % 2 == 0 else 0)
        elif tag == "snout":
            c = fur(pal["belly"], tx, ty, 6)
            if face == "north" and y > -2.3:
                c = (245, 240, 230) if abs(x) < 1.0 else shade(pal["belly"], -30)   # two front teeth
            if face == "north" and -2.6 < y < -2.0:
                c = shade(pal["fur"], -40)                                         # the mouth line
        elif tag == "head":
            c = fur(skin if gong else pal["fur"], tx, ty)
            if face == "north":
                if y > -4.5 and abs(x) < 3.5:
                    c = fur(pal["belly"], tx, ty, 6)                               # pale muzzle and cheeks
                ex = abs(x) - 2.2
                if -6.5 < y < -4.0 and abs(ex) < 1.1:
                    if gong:
                        c = (232, 230, 222) if abs(ex) > 0.3 or y < -5.6 else (200, 200, 195)   # milky cataracts
                    else:
                        c = (20, 18, 18)
                        if -6.4 < y < -5.6 and -0.9 < ex * (1 if x > 0 else -1) < -0.2:
                            c = (235, 235, 235)                                    # a glint
                        if groomed and y < -5.2:
                            c = fur(pal["fur"], tx, ty, 3)                         # heavy, contented lids
                            if -5.6 < y:
                                c = (150, 120, 130)                                # long lashes
                    if abs(ex) > 0.85 and gong:
                        c = shade(skin, -45)
                    if pal.get("spectacles") and (abs(ex) > 0.85 or y < -6.2):
                        c = (40, 34, 30)                                           # wire spectacles
                    if pal.get("mutant") and x < 0 and abs(ex) < 0.8:
                        c = (120, 255, 150)                                        # the glowing replacement eye
                if pal.get("mutant") and face == "north" and x < 0 and -1.4 < abs(ex) < 1.4 and -7.4 < y < -3.6 \
                        and (abs(y + 3.8) < 0.3 or abs(y + 7.2) < 0.3):
                    c = (60, 50, 50)                                               # stitches round the new eye
            elif pal.get("mutant") and face in ("east", "west") and abs(y + 5) < 0.4:
                c = (60, 50, 50)                                                   # a stitched scar
            elif face == "up" or face == "south":
                c = shade(c, -8)
        elif tag == "body":
            torso = fur(skin if gong else pal["fur"], tx, ty)
            if face == "north" and not gong and abs(x) < 2.2 and y < 5:
                torso = fur(pal["belly"], tx, ty, 6)
            c = torso
            if groomed:
                if face == "north" and abs(x) < 2.6:
                    c = fur(pal["belly"], tx, ty, 3)                               # a plump, spotless belly
                if y < 1.2:
                    c = pal["ribbon"]                                              # a silk ribbon at the neck
                    if face == "north" and abs(x) < 0.9:
                        c = shade(pal["ribbon"], 30)
            elif gong:
                # A tattered sack-cloth smock over the whole body.
                hem = 8.2 + noise("hem", int(x * 2), face) * 0.8
                if y < hem:
                    c = fur(pal["cloth"], tx, ty, 14)
                    if noise("dirt", tx // 3, ty // 3) < 0.25:
                        c = mix(c, pal["dirt"], 0.6)
                if 4.4 < y < 5.4:
                    c = pal["belt"]
            else:
                if y >= 5 or pal.get("top"):
                    c = fur(pal["cloth"], tx, ty, 12)
                    if noise("dirt", tx // 3, ty // 3) < 0.2:
                        c = mix(c, pal["dirt"], 0.55)
                if pal.get("top") and y < 5 and not (face == "north" and abs(x) < 1.0 and y < 3):
                    c = fur(pal["top"], tx, ty, 8)                                 # a coat or waistcoat
                    if pal.get("buttons") and face == "north" and abs(abs(x) - 1.5) < 0.4 and int(y * 2) % 3 == 0:
                        c = pal["buttons"]
                    if pal.get("wires") and noise("wire", tx // 2, ty) < 0.08:
                        c = (200, 120, 60) if noise("w2", tx, ty) < 0.5 else (60, 200, 120)   # copper wiring
                if 4.4 < y < 5.6:
                    c = pal["belt"]
                    if face == "north" and abs(x) < 0.7:
                        c = shade(pal["belt"], 25)                                 # the knot
            if gong and noise("scratch", tx // 2, ty) < 0.04:
                c = pal["scratch"]
        elif tag == "arm":
            c = fur(skin if gong else pal["fur"], tx, ty)
            if pal.get("sleeves") and y < 4.6:
                c = fur(pal["sleeves"], tx, ty, 8)
            if y > 5.4:
                c = pal["pink"]                                                    # hands
            elif gong and y < 2.5:
                c = fur(pal["cloth"], tx, ty, 14)
            if gong and noise("scratch", tx // 2, ty) < 0.05:
                c = pal["scratch"]
        elif tag in ("leg", "toes"):
            c = fur(skin if gong else pal["fur"], tx, ty)
            ragged = 3.0 + noise("rag", int(x * 2), int(z * 2), face) * 1.6
            if y < ragged and tag == "leg" and not groomed:
                c = fur(pal["cloth"], tx, ty, 12)
                if noise("dirt", tx // 3, ty // 3) < 0.22:
                    c = mix(c, pal["dirt"], 0.55)
            if y > 4.6 or tag == "toes":
                c = pal["pink"]
        if c is None:
            c = (255, 0, 255)
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


def cat_painter(pal):
    def stripe(c, x, y, z, tx, ty):
        if pal.get("calico"):
            return c
        k = (y * 0.75 + z * 0.12 + 20) % 3.4
        if k < 0.8:
            return fur(pal["dark"], tx, ty, 8)
        if k < 1.2:
            return fur(pal["tan"], tx, ty, 8)
        return c

    def calico(x, y, z, face, tx, ty):
        r = noise("calico", int((x + 20) // 2.5), int((y + 20) // 2.5), int((z + 20) // 2.5), b_part[0])
        return fur((214, 120, 40) if r < 0.35 else (34, 30, 30) if r < 0.55 else (236, 228, 214), tx, ty, 8)

    b_part = [""]

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        b_part[0] = p["name"]
        grey = calico(x, y, z, face, tx, ty) if pal.get("calico") else fur(pal["fur"], tx, ty, 10)
        c = grey
        if tag in ("helmet", "helmet_knob"):
            c = fur((30, 36, 64), tx, ty, 5)
            if tag == "helmet_knob" or (face == "north" and abs(x) < 1.3 and -13.5 < y < -10.5):
                c = (214, 176, 60)                                                 # brass star badge and knob
            elif y > -8.6:
                c = (24, 24, 30)                                                   # chin strap edge
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "crown":
            c = (214, 172, 52) if noise("crown", tx, ty) < 0.8 else (240, 210, 96)
            if y < -11.2 and int(x + 10) % 2 == 1:
                return CLEAR                                                       # the crown's points
            if face in ("north", "south") and abs(x) < 0.8 and -11 < y < -9.6:
                c = (180, 30, 40)                                                  # a ruby
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "mantle":
            c = fur((244, 240, 232), tx, ty, 5)
            if noise("ermine", tx // 2, ty // 2) < 0.12:
                c = (24, 22, 24)                                                   # ermine tails
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "belly":
            c = fur(pal["cloth"], tx, ty, 8)
            if y > 6.8:
                c = fur((244, 240, 232), tx, ty, 5) if noise("ermhem", tx // 2) > 0.12 else (24, 22, 24)
            elif face == "north" and abs(x) < 0.7:
                c = pal["buckle"]                                                  # gold trim down the front
            elif 4.4 < y < 5.6:
                c = pal["belt"]
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "jowls":
            c = fur(pal["white"] if y > -2 else pal["fur"], tx, ty, 6)
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "kettle":
            c = fur((150, 152, 158), tx, ty, 10)
            if noise("rivet", tx // 2, ty // 2) < 0.06 or y < -12.6:
                c = (110, 112, 118)
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "cap":
            c = fur(pal["capc"], tx, ty, 8)
            if y > -7.8:
                c = shade(pal["capc"], -20)                                        # rolled brim
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "ear":
            c = pal["pink"] if face == "north" and abs(x) < 0.8 and y > -3 else fur(pal["dark"], tx, ty, 8)
        elif tag == "snout":
            c = fur(pal["white"], tx, ty, 5)
            if face == "north" and y < -3.2 and abs(x) < 1.0:
                c = (25, 22, 22)                                                   # nose
            elif face == "north" and y > -1.7 and abs(x) < 0.25:
                c = shade(pal["white"], -60)
        elif tag == "head":
            c = stripe(grey, x, y, z, tx, ty) if face != "north" and not pal.get("calico") else grey
            if face == "north":
                if abs(x) < 1.6 and y < -4.5:
                    c = fur(pal["white"], tx, ty, 5)                               # white blaze
                if y > -3.4 and abs(x) < 3.2:
                    c = fur(pal["white"], tx, ty, 5)                               # white muzzle
                elif y > -4.4 and abs(x) > 3.2:
                    c = fur(pal["tan"], tx, ty, 6)                                 # tan cheeks
                ex = abs(x) - 2.6
                if -6.6 < y < -4.4 and abs(ex) < 1.2:
                    c = pal["eye"]
                    if abs(ex) < 0.35:
                        c = (20, 18, 15)                                           # slit pupil
                    if abs(ex) > 1.0 or y < -6.4:
                        c = (25, 22, 20)                                           # dark rim
                if -7.6 < y < -6.6 and abs(ex) < 1.3:
                    c = fur(pal["dark"], tx, ty, 6)                                # brow
        elif tag == "body":
            c = fur(pal["cloth"], tx, ty, 12)
            hem = 7.6 + noise("hem", int(x * 2), face) * 1.4
            if y > hem:
                c = fur(pal["pants"], tx, ty, 10)
            if face == "north" and y < 3.5 and abs(x) < (3.5 - y) * 0.9:
                c = fur(pal["white"], tx, ty, 5)                                   # white chest at the neckline
            if 5.4 < y < 6.6:
                c = pal["belt"]
                if face == "north" and abs(x) < 1.2:
                    c = pal["buckle"] if (abs(x) > 0.5 or abs(y - 6) > 0.35) else pal["belt"]
            if noise("tear", tx // 3, ty // 3) < 0.05 and y < 5:
                c = shade(c, -25)
            if pal.get("straps") and face in ("north", "south") and y < 5.4 and abs(x - (y - 2.5) * 0.9) < 0.55:
                c = pal["strap"]                                                   # a leather strap across the chest
            if pal.get("uniform"):
                c = fur(pal["cloth"], tx, ty, 5)                                   # a long blue watch coat
                if face == "north" and abs(abs(x) - 1.6) < 0.45 and int(y * 2) % 3 == 0 and y < 8:
                    c = (214, 176, 60)                                             # two rows of brass buttons
                if face == "north" and y < 1.4 and abs(x) < 1.0:
                    c = fur(pal["white"], tx, ty, 5)
                if 5.4 < y < 6.6:
                    c = pal["belt"]
            if pal.get("tabard"):
                c = fur((150, 152, 158), tx, ty, 14) if (int(tx / 2) + int(ty / 2)) % 2 else fur((126, 128, 134), tx, ty, 10)
                if face in ("north", "south") and abs(x) < 3.0:
                    c = fur(pal["tabard"], tx, ty, 6)                              # the city's red tabard...
                    if abs(x) < 0.7:
                        c = pal["buckle"]                                          # ...with a gold stripe
                if 5.4 < y < 6.6:
                    c = pal["belt"]
        elif tag == "cape":
            if y > 11.6 and noise("capehem", int(x * 2)) < 0.5:
                return CLEAR                                                       # ragged hem
            c = fur(pal["cape"], tx, ty, 8)
            if abs(x) > 4.0 or y < 0.6:
                c = shade(pal["cape"], -18)
        elif tag == "arm":
            c = fur(pal["cloth"], tx, ty, 12)
            if pal.get("tabard"):
                c = fur((150, 152, 158), tx, ty, 14) if (int(tx / 2) + int(ty / 2)) % 2 else fur((126, 128, 134), tx, ty, 10)
            if y > (4.8 if pal.get("long_sleeves") else 2.0):
                c = stripe(grey, x * 2, y, z, tx, ty)
            if y > 5.4:
                c = fur(pal["paw"], tx, ty, 5)                                     # paws
        elif tag in ("leg", "toes"):
            ragged = 3.6 + noise("rag", int(x * 2), int(z * 2), face) * 1.4
            c = fur(pal["pants"], tx, ty, 10) if (y < ragged and tag == "leg") else stripe(grey, x * 2, y, z, tx, ty)
            if pal.get("boots") and (y > 3.4 or tag == "toes"):
                c = fur((26, 26, 28), tx, ty, 5) if y > 4.0 or tag == "toes" else pal["strap"]   # boots, brown cuff
            elif y > 5.6 or tag == "toes":
                c = fur(pal["paw"], tx, ty, 5)
        elif tag == "tail":
            c = fur(pal["dark"], tx, ty, 8) if int(z) % 3 == 1 else grey
            if z > 5.5 and pal.get("tip", True):
                c = fur(pal["white"], tx, ty, 5)
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


def dog_painter(pal):
    husky = pal["kind"] == "husky"

    def plates(base, y, x, tx, ty):
        """Rows of lacquered armor plates laced with gold."""
        c = fur(base, tx, ty, 6)
        row = (y + 20) % 1.6
        if row < 0.35:
            c = shade(base, -45)
            if int((x + 20) * 2) % 3 == 0:
                c = pal["lace"]
        return c

    def bandage(x, y, z, face, tx, ty):
        """Linen wrappings, wound round and round, grimy and grey-tan, with old blood come through in places."""
        base = (214, 200, 174) if noise("wrap", tx // 2, ty // 4) < 0.6 else (176, 164, 140)    # dirty and worn
        c = fur(base, tx, ty, 9)
        band = (y * 1.6 + (x if face in ("north", "south") else z) * 0.45 + 40) % 2.0
        if band < 0.32:
            c = shade(c, -38)                                                     # the edge of each turn
        if noise("stain", tx // 4, ty // 4) < 0.14:
            c = mix(c, (110, 84, 60), 0.5)                                        # grime
        b = noise("blood", tx // 5, ty // 4)
        edge = noise("bloodedge", tx, ty)
        if b < 0.16 and edge < 0.8:
            c = mix(c, (118, 24, 20), 0.75 if b < 0.08 else 0.45)                 # blood seeping through, in patches
        return c

    def tower(u, v):
        """The dark stone tower: u across (-1..1), v down (0 top .. 1 foot). True where it's dark."""
        if v < 0 or v > 1 or abs(u) > 0.62:
            return False
        if v < 0.2:
            return int((u + 0.62) / 0.248) % 2 == 0                               # merlons
        if v > 0.72 and abs(u) < 0.2:
            return False                                                          # the door
        if 0.34 < v < 0.46 and abs(u) < 0.12:
            return False                                                          # a window slit
        return abs(u) < (0.46 if v < 0.85 else 0.62)                              # the shaft, a broad foot

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        if tag == "pole":
            return rgba(shade(fur((74, 50, 32), tx, ty, 6), FACE_LIGHT[face]))
        if tag == "flag":
            u, v = x / 3.0, (y + 14) / 10.0                                        # across, down the banner
            if v > 0.9 and noise("tatter", int(x * 2), 0) < 0.45:
                return CLEAR                                                      # a tattered foot
            c = fur(pal["cloth"], tx, ty, 6)
            if abs(u) > 0.8 or v < 0.06:
                c = pal["trim"]                                                   # crimson edging
            elif tower(u * 1.35, (v - 0.14) / 0.68):
                c = fur(pal["emblem"], tx, ty, 4)
            return rgba(shade(c, FACE_LIGHT[face]))
        if pal.get("templar") and tag in ("body", "skirt") and face == "north" and abs(x) < 3.0:
            # The tabard over the armour, the tower on the breast and again on the skirt; its hem in tatters.
            top = 1.0 if tag == "body" else 0.0
            if tag == "skirt" and y > 4.6 and noise("tatter", int(x * 2), 1) < 0.5:
                return CLEAR
            if not (tag == "body" and 6.0 < y < 7.4):
                c = fur(pal["cloth"], tx, ty, 6)
                if abs(x) > 2.3:
                    c = pal["trim"]
                elif tag == "body" and tower(x / 1.9, (y - 1.6) / 4.2):
                    c = fur(pal["emblem"], tx, ty, 4)
                elif tag == "skirt" and tower(x / 1.5, (y - 0.6) / 3.6):
                    c = fur(pal["emblem"], tx, ty, 4)
                return rgba(shade(c, FACE_LIGHT[face]))
        if tag in ("cauldron", "rim"):
            c = fur((70, 68, 68), tx, ty, 8)                                       # old iron
            if tag == "cauldron" and (y > -4.6 or y < -10.4):
                c = shade(c, -22)                                                  # its lip and its base
            if noise("rust", tx // 2, ty // 2) < 0.08:
                c = mix(c, (110, 70, 44), 0.5)
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "strip":
            c = bandage(x, y, z, face, tx, ty)
            if y > 1 + noise("fray", int(x * 2), face) * 3 and noise("tear", tx, ty) < 0.3:
                return CLEAR                                                      # frayed ends
            return rgba(shade(c, FACE_LIGHT[face]))
        if pal.get("bandaged"):
            c = bandage(x, y, z, face, tx, ty)
            if tag == "head" and face == "north" and -6.0 < y < -5.0 and 1.2 < abs(x) < 3.4:
                c = (20, 14, 12)                                                  # a slit for each eye
            return rgba(shade(c, FACE_LIGHT[face]))
        coat = fur(pal["fur"], tx, ty, 9)
        if pal.get("brindle") and noise("brindle", int((y + x * 0.4 + 20) * 1.5), face) < 0.35:
            coat = fur(shade(pal["fur"], -35), tx, ty, 6)
        c = coat
        if tag == "kabuto":
            c = plates(pal["helmet"], y, x, tx, ty)
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "crest":
            # A gold V (the maedate) standing up from the brow of the helmet.
            if abs(abs(x) - (y + 14) * 0.55 - 0.3) > 0.55 and not (y > -10.8 and abs(x) < 0.8):
                return CLEAR
            return rgba(fur(pal["lace"], tx, ty, 6))
        if tag == "kasa":
            c = fur((214, 184, 118), tx, ty, 8)
            if int(math.hypot(x, z) * 2) % 3 == 0:
                c = shade(c, -24)                                                  # woven rings of straw
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "quiver":
            c = fur((110, 72, 44), tx, ty, 6)
            if y < -1.4:
                c = (236, 232, 222) if int((x + 5) * 2) % 2 == 0 else (176, 40, 36)   # fletchings
            return rgba(shade(c, FACE_LIGHT[face]))
        if tag == "sode":
            return rgba(shade(plates(pal["armor"], y, x + z, tx, ty), FACE_LIGHT[face]))
        if tag == "flop":
            c = fur(pal["fur"], tx, ty, 12)
            if (int(y + 20) + int(z + 20)) % 3 == 0:
                c = shade(c, -14)                                                  # curly, wavy fur
        elif tag == "ear":
            c = pal["pink"] if face == "north" and abs(x) < 0.8 and y > -4 else coat
        elif tag == "snout":
            c = fur(pal["light"], tx, ty, 5)
            if face == "north" and y < -3.0 and abs(x) < 1.2:
                c = (34, 26, 22)                                                   # nose
            elif face == "up" and z < -6 and abs(x) < 1.2:
                c = (34, 26, 22)
            elif face == "north" and y > -1.4:
                c = shade(pal["light"], -25)
        elif tag == "head":
            c = coat
            if face == "north":
                if not husky and abs(x) < 1.3 and y < -4:
                    c = fur(pal["light"], tx, ty, 5)                               # pale stripe down the brow
                if y > -3.6 and abs(x) < 3.4:
                    c = fur(pal["light"], tx, ty, 5)
                ex = abs(x) - 2.5
                if pal.get("fierce") and abs(ex) < 1.3:
                    # Narrow, hard eyes under a brow that bears down toward the nose: a wolf's squint.
                    lid = -5.4 - 0.35 * ex                                         # lower at the inner corner
                    if lid < y < -4.5:
                        c = pal["eye"]
                        if abs(ex) < 0.4:
                            c = (20, 14, 10)                                       # pupil
                    elif lid - 1.0 < y <= lid:
                        c = shade(pal["fur"], -40)                                 # the brow
                    return rgba(shade(c, FACE_LIGHT[face]))
                if -6.4 < y < -4.2 and abs(ex) < 1.1:
                    c = pal["eye"]
                    if abs(ex) < 0.45 and y > -5.6:
                        c = (25, 18, 14)                                           # pupil
                    if ex * (1 if x > 0 else -1) < -0.6:
                        c = (240, 236, 228)                                        # the white of the eye
                if -7.2 < y < -6.4 and abs(ex) < 1.2:
                    c = shade(pal["fur"], -30)                                     # brow
        elif pal.get("feral") and tag in ("body", "arm", "skirt"):
            # A Biter: no clothes but a ragged loincloth and a spiked collar; old scars through the fur.
            c = coat
            if tag == "body" and y < 1.3:
                c = (46, 32, 22)
                if face in ("north", "south", "east", "west") and int((x + z + 20) * 1.5) % 3 == 0 and y < 0.7:
                    c = (200, 200, 206)                                           # the collar's studs
            elif tag in ("body", "arm") and abs((x * 0.8 + y + z * 0.3) % 3.1 - 1.5) < 0.12 and noise("scar", int(y), face) < 0.5:
                c = (196, 120, 116)                                               # a scar
            elif tag == "skirt":
                c = fur((70, 52, 36), tx, ty, 10)
                if y > 3.2 and noise("rag", tx, face) < 0.55:
                    return CLEAR                                                  # a torn hem
            elif tag == "arm" and y >= 5.4:
                c = shade(coat, -20)
        elif tag == "body" and pal.get("armor"):
            c = plates(pal["armor"], y, x, tx, ty)
            if 6.0 < y < 7.4:
                c = pal["trim"]                                                    # obi sash under the armor
        elif tag == "skirt" and pal.get("hakama"):
            c = fur(pal["hakama"], tx, ty, 6)
            if int((x + z + 20) * 1.3) % 2 == 0 and face in ("north", "south"):
                c = shade(pal["hakama"], -16)                                      # pleats
        elif tag in ("body", "skirt", "arm"):
            c = fur(pal["robe"], tx, ty, 8)
            trim = pal["trim"]
            if tag == "body":
                # The crossed collar of the robe, then the sash.
                if face == "north" and y < 5 and abs(abs(x) - (4.5 - y) * 0.8) < 0.7:
                    c = trim
                if face == "north" and y < 4.5 and abs(x) < (4.5 - y) * 0.8 - 0.7:
                    c = fur(pal["light"] if not husky else pal["fur"], tx, ty, 5)  # chest fur in the V
                if 5.6 < y < 7.4:
                    c = trim
            elif tag == "skirt":
                if y > 4.8:
                    c = trim                                                       # trimmed hem
                if face == "north" and y < 4.0 and abs(x) < 0.9:
                    c = trim                                                       # sash ends hanging down
            else:
                if 4.4 < y < 5.4:
                    c = trim                                                       # sleeve cuff
                elif y >= 5.4:
                    c = coat                                                       # paws
        elif tag in ("leg", "toes"):
            c = coat
            if tag == "toes" or y > 6.2:
                c = fur(pal["light"], tx, ty, 5)
        elif tag == "tail":
            c = coat if not husky or z < 4.5 else fur(pal["light"], tx, ty, 5)
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


def bat_painter(pal):
    kind = pal["kind"]

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        coat = fur(pal["fur"], tx, ty, 9)
        c = coat
        if tag == "wing":
            ax = abs(x)
            reach = 6.0 * (1 - max(0.0, y + 1) / 12.0) + 0.6
            scallop = 0.7 * abs(math.sin(ax * 1.6))
            if ax > reach - scallop and y > 1:
                return CLEAR
            if y < -0.4 or ax < 0.6 or abs(ax - (y + 1) * 0.55) < 0.35 or abs(ax - (y + 1) * 1.1) < 0.35:
                return rgba(shade(pal["fur"], -10))                                # finger bones
            return rgba(fur(pal["wing"], tx, ty, 8), 240)
        if tag == "ear":
            c = pal["wing"] if face == "north" and abs(x) < 1.2 and y > -5 else coat
        elif tag == "snout":
            c = fur(pal["light"], tx, ty, 6)
            if face == "north" and y < -3.2 and abs(x) < 0.9:
                c = shade(pal["fur"], -30)                                         # little nose
            if face == "north" and y > -1.6 and 0.6 < abs(x) < 1.5:
                c = (238, 230, 214)                                                # fangs
        elif tag == "head":
            c = coat
            if face == "north":
                ex = abs(x) - 2.3
                if -6.4 < y < -4.2 and abs(ex) < 1.1:
                    c = pal["eye"]
                    if ex * (1 if x > 0 else -1) < -0.5 and y < -5.4:
                        c = (250, 240, 230)                                        # glint
                if kind == "barman" and -6.8 < y < -3.8 and abs(ex) < 1.6:
                    ring = abs(ex) > 1.15 or y < -6.4 or y > -4.2
                    c = pal["gold"] if ring else (22, 20, 22)                      # round dark glasses
                    if abs(x) < 0.7 and -5.8 < y < -5.2:
                        c = pal["gold"]
                if kind == "flapper" and -8.4 < y < -7.4:
                    c = pal["gold"]                                                # headband
            elif kind == "flapper" and -8.4 < y < -7.4:
                c = pal["gold"]
        elif tag == "body":
            c = coat
            if face == "north" and abs(x) < 2.4 and y < 6:
                c = fur(pal["light"], tx, ty, 6)                                   # pale chest
            if kind == "folk":
                if y > 5.0:
                    c = fur(pal["pants"], tx, ty, 10)
                    if noise("dirt", tx // 3, ty // 3) < 0.2:
                        c = mix(c, pal["fur"], 0.6)
                if face == "north" and 4.6 < y < 5.6 and abs(x) < 0.7:
                    c = shade(pal["pants"], 30)                                    # drawstring
            elif kind == "barman":
                c = fur(pal["shirt"], tx, ty, 5)
                if face in ("north", "south", "east", "west") and y < 6.5 and (face != "north" or abs(x) > 0.9):
                    c = fur(pal["vest"], tx, ty, 6)
                if face == "north" and y < 1.2 and abs(x) < 1.6:
                    c = pal["tie"]                                                 # bow tie
                if y >= 6.0:
                    c = fur(pal["apron"], tx, ty, 6)
            elif kind == "jazz":
                c = fur(pal["coat"], tx, ty, 6)
                if face == "north" and abs(x) < 1.6:
                    c = pal["shirt"] if int(y * 2) % 2 == 0 else (30, 28, 30)      # striped shirt
                if y < 1.4:
                    c = pal["scarf"]
            elif kind == "bookie":
                c = fur(pal["shirt"], tx, ty, 5)
                if face != "north" or abs(x) > 1.0:
                    check = (int(tx / 3) + int(ty / 3)) % 2
                    c = fur(pal["check"] if check else pal["vest"], tx, ty, 5)     # a loud checked waistcoat
                if face == "north" and y < 1.2 and abs(x) < 0.8:
                    c = pal["tie"]
                if y >= 6.0:
                    c = fur(pal["pants"], tx, ty, 6)
            elif kind == "flapper":
                c = fur(pal["dress"], tx, ty, 6)
                if noise("gold", tx // 2, ty // 2) < 0.08:
                    c = pal["gold"]
                if face == "north" and y < 4.2 and abs(abs(x) - (4.2 - y) * 0.6) < 0.35:
                    c = pal["pearl"]                                               # pearl necklace
        elif tag == "belly":
            c = fur(pal["apron"], tx, ty, 6) if y > 6 else fur(pal["shirt"], tx, ty, 5)
        elif tag == "fringe":
            if y > 8.6 and noise("fringe", int(x * 3), face) < 0.45:
                return CLEAR
            c = pal["gold"] if int(x * 2 + 40) % 3 == 0 else fur(pal["dress"], tx, ty, 6)
        elif tag == "arm":
            c = coat
            sleeve = {"barman": pal.get("shirt"), "jazz": pal.get("coat"), "bookie": pal.get("shirt")}.get(kind)
            if sleeve and y < 5.4:
                c = fur(sleeve, tx, ty, 5)
            if y > 6.2:
                c = fur(pal["light"], tx, ty, 6)                                   # hands
        elif tag in ("leg", "toes"):
            c = coat
            pants = {"folk": pal.get("pants"), "barman": pal.get("apron"), "jazz": pal.get("coat"),
                     "bookie": pal.get("pants")}.get(kind)
            if pants and y < 3.4 and tag == "leg":
                c = fur(pants, tx, ty, 8)
            if tag == "toes" and face == "north" and int((x + 2) * 2) % 2 == 0:
                c = (238, 230, 214)                                                # claws
        elif tag == "flatcap":
            check = (int(tx / 3) + int(ty / 3)) % 2
            c = fur(pal["check"] if check else pal["vest"], tx, ty, 5)
        elif tag == "pencil":
            c = (230, 190, 60) if z < 2.2 else (40, 36, 36)
        elif tag == "cigar":
            c = (120, 70, 40) if z > -8.6 else (230, 110, 40)
        elif tag == "beret":
            c = fur((32, 30, 32), tx, ty, 5)
        elif tag == "case":
            c = fur((28, 26, 28), tx, ty, 5)
            if abs(y - 4.5) < 0.4:
                c = (90, 90, 95)
        elif tag == "feather":
            if abs(x - 0.5 + (y + 6) * 0.08) > 1.4 - (y + 6) * 0.05:
                return CLEAR
            c = fur((22, 22, 26), tx, ty, 6)
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


def frog_painter(pal):
    kind = pal["kind"]

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        green = fur(pal["green"], tx, ty, 10)
        if noise("spot", int((x + 20) * 1.2), int((y + 20) * 1.2), int(z), face) < 0.06:
            green = fur(pal["light"], tx, ty, 8)                                   # mottled skin
        c = green
        if tag == "eye":
            c = (232, 196, 36)
            if face == "north" and abs(y + 7.5) < 0.6 and abs(abs(x) - 5) < 0.6:
                c = (22, 20, 18)                                                   # pupil, on the front only
            if face == "up" and pal.get("lids"):
                c = fur(pal["dark"], tx, ty, 6)
            if pal.get("lids") and y < -7.6:
                c = fur(pal["dark"], tx, ty, 6)                                    # half-closed, smug lids
            if pal.get("monocle") and x > 0 and face == "north" and (abs(abs(x - 5) - 1.2) < 0.3 or abs(abs(y + 7.5) - 1.2) < 0.3):
                c = (214, 176, 60)                                                 # a gold monocle
            if pal.get("spectacles") and face == "north" and (abs(y + 7.5) < 1.5) and abs(abs(x) - 5) > 1.05:
                c = (60, 50, 40)                                                   # round spectacles
        elif tag == "head":
            c = green
            if face == "north" or (face in ("east", "west") and z < -2.5):
                if y > -2.6:
                    c = fur((236, 226, 200), tx, ty, 5)                            # cream jaw
                if -2.9 < y < -2.1:
                    c = (176, 66, 56)                                              # the wide mouth
                if pal.get("mustache") and face == "north" and -3.7 < y < -3.1 and 0.4 < abs(x) < 3.2:
                    c = (40, 30, 24)                                               # pencil mustache
                if pal.get("whiskers") and face in ("north", "east", "west") and -3.4 < y < -1.0 and \
                        (abs(x) > 1.0 and abs(x) - 1.0 > -(y + 1.0) * 1.6 - 2):
                    c = (236, 236, 230)                                            # long drooping white whiskers
            if face == "down":
                c = fur((236, 226, 200), tx, ty, 5)
        elif tag == "body":
            c = fur(pal["tunic"], tx, ty, 9) if pal.get("tunic") else green
            if not pal.get("tunic") and face == "north" and abs(x) < 2.4:
                c = fur((236, 226, 200), tx, ty, 6)                                # cream belly
            if pal.get("vest") and (face != "north" or abs(x) > 1.0) and y < 6:
                c = fur(pal["vest"], tx, ty, 10)
            if pal.get("scarf") and y < 1.6:
                c = pal["scarf"]
            if 5.4 < y < 6.6:
                c = pal["belt"]
                if face == "north" and abs(x) < 1.0:
                    c = pal["buckle"]
            if y > 6.6 and pal.get("pants"):
                c = fur(pal["pants"], tx, ty, 9)
            if pal.get("straps") and face in ("north", "south") and abs(abs(x) - (3.4 - y * 0.35)) < 0.4 and y < 5.4:
                c = pal["belt"]
            if pal.get("overalls") and (y > 2.4 or (face in ("north", "south") and abs(abs(x) - 1.8) < 0.5)):
                c = fur(pal["overalls"], tx, ty, 8)                                # bib and straps
            if pal.get("chain") and face == "north" and abs(y - (2.2 + abs(x) * 0.5)) < 0.3 and abs(x) < 2.5:
                c = (214, 176, 60)                                                 # a gold watch chain
            if pal.get("buttons") and face == "north" and abs(abs(x) - 1.3) < 0.35 and int(y * 1.2) % 2 == 0 and y < 5:
                c = pal["buttons"]                                                 # double-breasted chef's jacket
            if pal.get("apron") and face == "north" and y > 2.0 and abs(x) < 3.0:
                c = fur(pal["apron"], tx, ty, 5)
                if pal.get("apron_stripes") and int((x + 4) * 1.4) % 2 == 0:
                    c = pal["apron_stripes"]
                if pal.get("apron_stains") and noise("stain", int(x * 3), int(y * 3)) < 0.08:
                    c = pal["apron_stains"]
        elif tag == "arm":
            c = green
            if pal.get("tunic") and y < 3.5:
                c = fur(pal["tunic"], tx, ty, 9)
            if pal.get("vest") and y < 2.0:
                c = fur(pal["vest"], tx, ty, 9)
            if pal.get("cuff") and 2.6 < y < 3.6:
                c = pal["cuff"]
        elif tag in ("leg", "toes"):
            c = green
            if pal.get("pants") and y < 3.5 and tag == "leg":
                c = fur(pal["pants"], tx, ty, 9)
            if pal.get("boots") and (y > 2.5 or tag == "toes"):
                c = fur(pal["boots"], tx, ty, 6)
            elif tag == "toes" and face == "north" and int((x + 3) * 1.6) % 2 == 0:
                c = (224, 214, 190)                                                # toe pads
        elif tag == "bucket_hat":
            c = fur((168, 160, 100), tx, ty, 8)
            if y > -6.6 and y < -6.4:
                c = (120, 110, 70)
        elif tag == "top_hat":
            c = fur((30, 28, 32), tx, ty, 4)
            if -7.8 < y < -7.0:
                c = (214, 176, 60)                                                 # gold hatband
        elif tag == "cap":
            c = fur((196, 112, 64), tx, ty, 6)                                     # copper-orange cap
        elif tag == "robe":
            if y > 12.2 and noise("robehem", int(x * 2), face) < 0.4:
                return CLEAR
            c = fur(pal["tunic"], tx, ty, 8)
            if face == "north" and abs(x) < 0.7:
                c = pal["trim"]
        elif tag == "stick":
            c = fur((110, 80, 50), tx, ty, 8) if y > -2 else (70, 120, 50)         # a gnarled stick, leaf at the top
        elif tag == "tadpole":
            c = green
        elif tag == "toque":
            c = fur((244, 242, 236), tx, ty, 4)
            if face != "down" and int((x + 10) * 1.3) % 3 == 0:
                c = (226, 222, 214)                                                # pleats
        elif tag == "toque_band":
            c = (236, 232, 222)
        elif tag == "flatcap":
            c = fur((92, 84, 74), tx, ty, 8)
            if (int(x * 2) + int(z * 2)) % 4 == 0:
                c = (74, 68, 60)                                                   # tweed
        elif tag == "bonnet":
            c = fur((236, 230, 214), tx, ty, 5)
            if abs(y + 7.0) < 0.5:
                c = (120, 60, 90)                                                  # ribbon
        elif tag == "bonnet_brim":
            c = (226, 220, 204)
        elif tag == "pointer":
            c = fur((150, 110, 60), tx, ty, 6) if y < 7 else (230, 230, 230)       # a willow pointer, chalky tip
        elif tag == "fez":
            c = fur((150, 30, 30), tx, ty, 6)
        elif tag == "tassel":
            c = (222, 176, 50)
        elif tag == "satchel":
            c = fur((120, 78, 44), tx, ty, 8)
            if abs(y - 5) < 0.3:
                c = (180, 150, 100)
        elif tag == "glider":
            ax = abs(x)
            if y > 8.5 - ax * 0.5:
                return CLEAR                                                       # swept-back wing tips
            if ax < 0.5 or abs(ax - 4.5) < 0.35 or abs(ax - 8.5) < 0.4 or y < -0.4:
                return rgba(fur((110, 72, 42), tx, ty, 6))                         # wooden struts
            return rgba(fur((226, 208, 170), tx, ty, 6), 245)
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


def goxa_painter(pal):
    havu = pal["kind"] == "havu"
    skin = pal["skin"]

    def feathers(tx, ty, x, y):
        c = fur(pal["cloak"], tx, ty, 6)
        if int((x + 20) * 2) % 3 == 0 and noise("fth", int(x * 2), int(y)) < 0.6:
            c = shade(pal["cloak"], -22)                                           # feather edges
        return c

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        cloth = feathers(tx, ty, x, y) if havu else fur(pal["cloak"], tx, ty, 6)
        c = cloth
        if tag == "head":
            if face == "north" and abs(x) < 2.6 and -6.2 < y < -0.6:
                c = fur(skin, tx, ty, 5)                                           # the flat mask of a face
                if abs(abs(x) - 1.2) < 0.55 and -4.6 < y < -3.6:
                    c = (24, 30, 38)                                               # eye holes
                if abs(x) < 0.9 and -2.0 < y < -1.4:
                    c = (24, 30, 38)                                               # mouth hole
                if abs(x) < 0.55 and -6.2 < y < -5.3:
                    c = (178, 30, 40)                                              # ruby
        elif tag == "crest":
            spike = abs(math.sin((x + 4.5) * 1.4)) * 5.5
            if y < -6 + (6 - spike):
                return CLEAR
            return rgba(feathers(tx, ty, x, y))
        elif tag == "hat":
            c = fur(pal["hat"], tx, ty, 6)
        elif tag in ("body", "skirt"):
            c = cloth
            if tag == "skirt" and y > 6.0 and noise("hem", int(x * 2), face) < (0.6 if havu else 0.0):
                return CLEAR                                                       # ragged feathered hem
            if not havu and tag == "body" and y < 1.6:
                c = fur(pal["wrap"], tx, ty, 6)                                    # cloth wrap at the neck
        elif tag == "arm":
            c = cloth
            if y > (5.0 if not havu else 9.0):
                c = fur(skin, tx, ty, 5)                                           # long blue forearms and hands
                if not havu and 4.0 < y < 8.0:
                    c = fur(pal["cloak"], tx, ty, 6)                               # tighter sleeve to the wrist
            if y > 10.6 and int((x + z + 10) * 2) % 2 == 0:
                c = (222, 176, 50)                                                 # gold rings on every finger
        elif tag == "wing":
            ax = abs(x)
            length = 14 - ax * 0.9 - noise("wl", int(ax * 2)) * 2.5
            if y > length:
                return CLEAR
            return rgba(feathers(tx, ty, x, y))
        elif tag == "staff":
            c = fur((222, 176, 50), tx, ty, 8)
            if y < -12 and abs(x) < 0.5 and abs(y + 13.5) < 0.6:
                return CLEAR                                                       # the ring's hole
        elif tag in ("leg", "toes"):
            c = fur(skin, tx, ty, 5)
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


def lilbun_painter(pal):
    def spotted(tx, ty, x, y, z, face):
        """Spots of random size, spread evenly: about one per grid cell, each nudged a little so they don't line up."""
        if face in ("up", "down"):
            u, v = x, z
        elif face in ("north", "south"):
            u, v = x, y * 1.4
        else:
            u, v = z, y * 1.4
        cell = 3.2
        cu, cv = math.floor(u / cell), math.floor(v / cell)
        for du in (-1, 0, 1):
            for dv in (-1, 0, 1):
                key = (cu + du, cv + dv, face)
                if noise("spot", *key) > 0.8:
                    continue
                sx = (cu + du + 0.32 + 0.36 * noise("sx", *key)) * cell
                sy = (cv + dv + 0.32 + 0.36 * noise("sy", *key)) * cell
                r = 0.55 + 0.85 * noise("sr", *key) ** 1.3      # mostly small, a few big, rarely touching
                if (u - sx) ** 2 + (v - sy) ** 2 < r * r:
                    return True
        return False

    def paint(p, b, face, tx, ty, pt, n):
        x, y, z = pt
        tag = b["tag"]
        stalk = fur(pal["stalk"], tx, ty, 5)
        c = stalk
        if tag in ("brim", "dome"):
            if tag == "brim" and face == "down":
                # Yellow underside with gills radiating from the stalk.
                c = fur(pal["under"], tx, ty, 6)
                ang = math.atan2(z, x)
                if int((ang + math.pi) / (2 * math.pi) * 28) % 2 == 0:
                    c = shade(pal["under"], -18)
            else:
                c = fur(pal["cap"], tx, ty, 8)
                if tag == "brim" and face not in ("up", "down") and y > -6.8:
                    c = fur(pal["cap_shade"], tx, ty, 6)                          # darker lip of the cap
                if spotted(tx, ty, x, y, z, face):
                    c = fur(pal["spot"], tx, ty, 4)
        elif tag == "face":
            if face == "north":
                ex = abs(x) - 1.7
                if -5.0 < y < -2.2 and abs(ex) < 0.9:
                    c = (35, 26, 20)                                               # big dark eyes
                    if -4.8 < y < -4.2 and -0.7 < ex * (1 if x > 0 else -1) < -0.1:
                        c = (245, 240, 230)
            c = shade(c, -10 if y < -6 else 0)                                     # shadow under the cap
        elif tag == "body":
            c = stalk
            if pal.get("overalls"):
                if y > 1.5 or (face in ("north", "south") and abs(abs(x) - 1.6) < 0.55):
                    c = fur(pal["overalls"], tx, ty, 8)                            # bib and straps
                if face == "north" and 1.6 < y < 2.4 and abs(abs(x) - 1.6) < 0.5:
                    c = pal["button"]
        elif tag == "arm":
            c = pal["hands"] if y > 3.2 else stalk
        elif tag == "leg":
            c = fur(pal["overalls"], tx, ty, 8) if pal.get("overalls") and y < 1.8 else stalk
            if y > 1.8:
                c = pal["hands"]                                                   # little brown shoes
        return rgba(shade(c, FACE_LIGHT[face]))
    return paint


RACES = {
    "rat": (lambda: with_ear_holder(rat_parts()), {
        "runner": rat_painter({"fur": (112, 88, 70), "belly": (196, 172, 146), "pink": (216, 142, 134),
                               "skin": (112, 88, 70), "cloth": (124, 124, 122), "dirt": (110, 85, 60),
                               "belt": (150, 36, 36), "whisker": (240, 236, 226)}),
        "scavenger": rat_painter({"fur": (112, 110, 110), "belly": (192, 190, 188), "pink": (214, 162, 160),
                                  "skin": (112, 110, 110), "cloth": (186, 160, 112), "dirt": (120, 92, 60),
                                  "belt": (212, 192, 150), "whisker": (240, 236, 226)}),
        "beautiful": rat_painter({"groomed": True, "fur": (238, 232, 226), "belly": (250, 247, 242),
                                  "pink": (232, 172, 172), "skin": (238, 232, 226), "cloth": (238, 232, 226),
                                  "dirt": (238, 232, 226), "belt": (238, 232, 226), "ribbon": (186, 150, 210),
                                  "whisker": (250, 250, 250)}),
        "gong": rat_painter({"gong": True, "fur": (226, 176, 166), "belly": (236, 196, 186), "pink": (222, 150, 146),
                             "skin": (226, 176, 166), "cloth": (124, 116, 102), "dirt": (92, 76, 56),
                             "belt": (196, 172, 120), "scratch": (170, 62, 60), "whisker": (236, 230, 220)}),
        "sweeper": rat_painter({"fur": (98, 84, 76), "belly": (176, 160, 146), "pink": (214, 146, 140),
                                "skin": (98, 84, 76), "cloth": (92, 104, 120), "dirt": (90, 76, 60),
                                "belt": (120, 90, 50), "whisker": (236, 230, 220), "hat": (176, 40, 36)}),
        "postrat": rat_painter({"fur": (132, 104, 82), "belly": (206, 184, 160), "pink": (220, 150, 142),
                                "skin": (132, 104, 82), "cloth": (60, 60, 70), "dirt": (90, 80, 70),
                                "belt": (90, 60, 34), "whisker": (240, 236, 226), "hat": (54, 74, 130),
                                "top": (54, 74, 130), "buttons": (214, 176, 60), "sleeves": (54, 74, 130)}),
        "accountant": rat_painter({"fur": (150, 146, 142), "belly": (210, 206, 200), "pink": (220, 160, 156),
                                   "skin": (150, 146, 142), "cloth": (52, 52, 56), "dirt": (52, 52, 56),
                                   "belt": (30, 30, 34), "whisker": (240, 236, 226), "hat": (40, 40, 44),
                                   "top": (120, 104, 76), "buttons": (200, 200, 206), "sleeves": (236, 232, 222),
                                   "spectacles": True}),
        "engineer": rat_painter({"fur": (96, 92, 98), "belly": (170, 164, 168), "pink": (200, 150, 160),
                                 "skin": (96, 92, 98), "cloth": (70, 66, 60), "dirt": (60, 50, 40),
                                 "belt": (176, 132, 56), "whisker": (200, 240, 210), "top": (110, 90, 60),
                                 "sleeves": (110, 90, 60), "mutant": True, "wires": True}),
    }),
    "cat": (cat_parts, {
        "rogue": cat_painter({"fur": (152, 150, 147), "dark": (92, 90, 90), "tan": (192, 160, 124),
                              "white": (236, 231, 222), "paw": (236, 231, 222), "cloth": (120, 146, 80),
                              "pants": (110, 74, 44), "belt": (34, 34, 34), "buckle": (196, 196, 202),
                              "eye": (232, 172, 32), "pink": (214, 150, 146)}),
        "adventurer": cat_painter({"fur": (216, 132, 52), "dark": (150, 80, 30), "tan": (236, 170, 96),
                                   "white": (240, 228, 206), "paw": (240, 228, 206), "cloth": (46, 78, 42),
                                   "pants": (206, 182, 140), "belt": (30, 30, 30), "buckle": (176, 176, 180),
                                   "eye": (52, 150, 52), "pink": (226, 150, 150)}),
        "hunter": cat_painter({"fur": (34, 34, 36), "dark": (22, 22, 24), "tan": (48, 48, 52),
                               "white": (112, 112, 118), "paw": (30, 30, 32), "cloth": (118, 36, 30),
                               "pants": (44, 74, 44), "belt": (90, 58, 34), "buckle": (176, 176, 180),
                               "eye": (236, 196, 40), "pink": (170, 120, 120), "straps": True, "strap": (96, 62, 36),
                               "boots": True, "cape": (40, 70, 42), "long_sleeves": True, "tip": False}),
        "fellmonger": cat_painter({"calico": True, "fur": (214, 120, 40), "dark": (34, 30, 30), "tan": (214, 120, 40),
                                   "white": (236, 228, 214), "paw": (236, 228, 214), "cloth": (98, 64, 38),
                                   "pants": (98, 64, 38), "belt": (70, 46, 28), "buckle": (212, 176, 70),
                                   "eye": (232, 120, 30), "pink": (226, 150, 150), "boots": True, "strap": (70, 46, 28),
                                   "capc": (126, 88, 56), "long_sleeves": True}),
        "cop": cat_painter({"fur": (238, 236, 232), "dark": (60, 58, 58), "tan": (220, 214, 206),
                            "white": (246, 244, 240), "paw": (246, 244, 240), "cloth": (34, 42, 78),
                            "pants": (30, 36, 66), "belt": (26, 24, 24), "buckle": (214, 176, 60),
                            "eye": (90, 170, 210), "pink": (226, 150, 150), "uniform": True, "long_sleeves": True,
                            "boots": True, "strap": (26, 24, 24)}),
        "guard": cat_painter({"fur": (120, 82, 50), "dark": (70, 46, 28), "tan": (160, 116, 72),
                              "white": (226, 214, 196), "paw": (226, 214, 196), "cloth": (140, 30, 30),
                              "pants": (60, 50, 44), "belt": (60, 40, 26), "buckle": (220, 180, 60),
                              "eye": (220, 190, 40), "pink": (200, 140, 140), "tabard": (150, 30, 32),
                              "long_sleeves": True, "boots": True, "strap": (60, 40, 26)}),
        "archer": cat_painter({"fur": (60, 60, 64), "dark": (30, 30, 34), "tan": (100, 96, 92),
                               "white": (214, 210, 204), "paw": (214, 210, 204), "cloth": (50, 90, 40),
                               "pants": (52, 46, 40), "belt": (60, 40, 26), "buckle": (200, 170, 60),
                               "eye": (120, 210, 60), "pink": (200, 140, 140), "tabard": (44, 96, 40),
                               "long_sleeves": True, "boots": True, "strap": (60, 40, 26), "straps": True}),
        "gar": cat_painter({"fur": (214, 140, 60), "dark": (150, 86, 30), "tan": (236, 180, 110),
                            "white": (244, 236, 220), "paw": (244, 236, 220), "cloth": (96, 36, 110),
                            "pants": (70, 30, 80), "belt": (200, 160, 50), "buckle": (232, 200, 84),
                            "eye": (120, 200, 90), "pink": (226, 150, 150), "long_sleeves": True}),
    }),
    "dog": (dog_parts, {
        "monk": dog_painter({"kind": "spaniel", "fur": (122, 82, 52), "light": (196, 160, 122), "pink": (214, 150, 146),
                             "robe": (226, 172, 56), "trim": (238, 226, 196), "eye": (110, 62, 30)}),
        "husky_monk": dog_painter({"kind": "husky", "fur": (234, 230, 224), "light": (200, 198, 196),
                                   "pink": (220, 160, 150), "robe": (40, 74, 42), "trim": (216, 170, 56),
                                   "eye": (228, 184, 40)}),
        "samurai": dog_painter({"kind": "husky", "fur": (44, 38, 36), "light": (222, 196, 158), "pink": (200, 150, 140),
                                "robe": (150, 32, 32), "trim": (236, 228, 210), "eye": (210, 140, 40),
                                "armor": (150, 32, 32), "helmet": (40, 38, 42), "lace": (222, 180, 70),
                                "hakama": (36, 34, 40)}),
        "archer": dog_painter({"kind": "husky", "fur": (198, 120, 70), "light": (246, 238, 226), "pink": (220, 160, 150),
                               "robe": (236, 232, 222), "trim": (44, 56, 104), "eye": (70, 44, 26),
                               "hakama": (44, 56, 104)}),
        "stablemaster": dog_painter({"kind": "husky", "fur": (96, 66, 42), "light": (176, 146, 110), "brindle": True,
                                     "pink": (200, 150, 140), "robe": (120, 96, 60), "trim": (70, 52, 34),
                                     "eye": (70, 44, 26), "hakama": (62, 80, 54)}),
        # ---- Dog City: the trades of the quarters, the pikemen, the Biters, and the one in the tower.
        "weaver": dog_painter({"kind": "spaniel", "fur": (220, 196, 150), "light": (244, 232, 210), "pink": (214, 150, 146),
                               "robe": (110, 140, 190), "trim": (238, 234, 224), "eye": (90, 60, 34)}),
        "dyer": dog_painter({"kind": "husky", "fur": (200, 200, 204), "light": (240, 240, 240), "pink": (210, 150, 150),
                             "robe": (40, 50, 110), "trim": (190, 60, 60), "eye": (90, 140, 200), "hakama": (40, 50, 110)}),
        "tailor": dog_painter({"kind": "spaniel", "fur": (40, 36, 34), "light": (236, 230, 220), "pink": (200, 140, 140),
                               "robe": (150, 40, 60), "trim": (220, 180, 70), "eye": (110, 70, 34)}),
        "shepherd": dog_painter({"kind": "spaniel", "fur": (150, 104, 66), "light": (226, 206, 170), "brindle": True,
                                 "pink": (210, 150, 140), "robe": (120, 140, 80), "trim": (232, 222, 196),
                                 "eye": (80, 54, 30)}),
        "smith": dog_painter({"kind": "husky", "fur": (90, 80, 72), "light": (190, 180, 170), "pink": (200, 150, 140),
                              "robe": (60, 56, 52), "trim": (150, 90, 40), "eye": (200, 140, 40), "hakama": (50, 46, 44)}),
        "tanner": dog_painter({"kind": "spaniel", "fur": (150, 96, 60), "light": (214, 176, 136), "pink": (210, 150, 140),
                               "robe": (140, 100, 60), "trim": (90, 60, 36), "eye": (70, 44, 26)}),
        "woodcutter": dog_painter({"kind": "husky", "fur": (180, 110, 60), "light": (240, 226, 206), "pink": (214, 154, 144),
                                   "robe": (70, 96, 60), "trim": (120, 90, 50), "eye": (70, 44, 26), "hakama": (90, 70, 46)}),
        "butcher": dog_painter({"kind": "husky", "fur": (232, 228, 222), "light": (246, 244, 240), "pink": (220, 160, 150),
                                "robe": (236, 232, 226), "trim": (170, 40, 40), "eye": (60, 90, 150), "hakama": (60, 60, 66)}),
        "cook": dog_painter({"kind": "spaniel", "fur": (200, 150, 90), "light": (240, 220, 180), "pink": (214, 150, 146),
                             "robe": (236, 232, 222), "trim": (60, 60, 70), "eye": (90, 60, 34)}),
        "server": dog_painter({"kind": "spaniel", "fur": (170, 120, 80), "light": (228, 200, 160), "pink": (214, 150, 146),
                               "robe": (190, 60, 50), "trim": (236, 230, 210), "eye": (90, 60, 34)}),
        "musician": dog_painter({"kind": "husky", "fur": (60, 50, 46), "light": (210, 196, 176), "pink": (200, 150, 140),
                                 "robe": (90, 60, 130), "trim": (230, 190, 70), "eye": (220, 170, 60), "hakama": (40, 30, 60)}),
        "teacher": dog_painter({"kind": "spaniel", "fur": (110, 80, 60), "light": (200, 176, 146), "pink": (204, 146, 140),
                                "robe": (50, 80, 60), "trim": (230, 220, 190), "eye": (70, 44, 26)}),
        "pikeman": dog_painter({"kind": "husky", "fur": (200, 170, 130), "light": (244, 232, 214), "pink": (214, 154, 144),
                                "robe": (40, 60, 110), "trim": (220, 220, 210), "eye": (80, 54, 30),
                                "armor": (40, 60, 110), "lace": (200, 200, 206), "hakama": (36, 40, 60)}),
        "high_guard": dog_painter({"kind": "husky", "fur": (54, 40, 32), "light": (186, 136, 80),
                                   "pink": (170, 120, 110), "robe": (52, 52, 58), "trim": (140, 30, 30),
                                   "eye": (214, 150, 36), "armor": (50, 50, 56), "lace": (176, 140, 70),
                                   "hakama": (42, 40, 44), "cloth": (226, 214, 188), "emblem": (34, 32, 36),
                                   "templar": True, "fierce": True}),
        "biter": dog_painter({"kind": "husky", "fur": (52, 46, 44), "light": (120, 110, 100), "pink": (170, 100, 100),
                              "robe": (52, 46, 44), "trim": (46, 32, 22), "eye": (226, 40, 30), "feral": True}),
        "bandaged": dog_painter({"kind": "spaniel", "fur": (222, 210, 184), "light": (222, 210, 184),
                                 "pink": (222, 210, 184), "robe": (222, 210, 184), "trim": (222, 210, 184),
                                 "eye": (20, 14, 12), "bandaged": True}),
    }),
    "frog": (frog_parts, {
        "jumper": frog_painter({"kind": "jumper", "green": (66, 160, 52), "light": (150, 196, 100), "vest": (140, 100, 62),
                                "scarf": (170, 64, 46), "belt": (96, 64, 38), "buckle": (180, 150, 100),
                                "straps": True}),
        "guard": frog_painter({"kind": "guard", "green": (66, 160, 52), "light": (150, 196, 100), "vest": (120, 84, 52),
                               "belt": (84, 56, 34), "buckle": (180, 150, 100), "straps": True}),
        "potioneer": frog_painter({"kind": "potioneer", "green": (66, 160, 52), "light": (150, 196, 100),
                                   "tunic": (230, 186, 50), "cuff": (196, 120, 50), "pants": (170, 50, 36),
                                   "belt": (30, 30, 30), "buckle": (180, 180, 186)}),
        "coppersmith": frog_painter({"kind": "coppersmith", "green": (66, 160, 52), "light": (150, 196, 100),
                                     "tunic": (110, 72, 44), "pants": (110, 30, 30), "belt": (80, 52, 30),
                                     "buckle": (200, 110, 60), "boots": (90, 60, 36)}),
        "fisher": frog_painter({"kind": "fisher", "green": (70, 150, 60), "light": (150, 196, 100),
                                "tunic": (116, 124, 70), "pants": (190, 168, 120), "belt": (96, 64, 38),
                                "buckle": (180, 150, 100), "boots": (52, 74, 52)}),
        "treasurer": frog_painter({"kind": "treasurer", "green": (60, 140, 56), "light": (140, 186, 96),
                                   "tunic": (40, 80, 50), "pants": (40, 40, 46), "belt": (30, 30, 34),
                                   "buckle": (214, 176, 60), "cuff": (236, 232, 222), "monocle": True, "chain": True}),
        "plumber": frog_painter({"kind": "plumber", "green": (66, 160, 52), "light": (150, 196, 100),
                                 "tunic": (196, 112, 64), "overalls": (70, 92, 140), "pants": (70, 92, 140),
                                 "belt": (60, 44, 30), "buckle": (180, 150, 100), "boots": (60, 44, 30)}),
        "elder": frog_painter({"kind": "elder", "green": (110, 140, 80), "light": (170, 180, 130), "dark": (80, 100, 60),
                               "lids": True, "whiskers": True, "spectacles": True, "tunic": (96, 74, 124),
                               "trim": (214, 176, 60), "belt": (214, 176, 60), "buckle": (214, 176, 60)}),
        "froglet": frog_painter({"kind": "froglet", "green": (90, 180, 70), "light": (170, 210, 120),
                                 "belt": (90, 180, 70), "buckle": (90, 180, 70)}),
        "chitinier": frog_painter({"kind": "chitinier", "green": (90, 150, 60), "light": (150, 190, 100),
                                   "dark": (50, 96, 40), "lids": True, "mustache": True, "tunic": (240, 238, 232),
                                   "buttons": (196, 112, 64), "cuff": (196, 112, 64), "pants": (40, 40, 46),
                                   "belt": (196, 112, 64), "buckle": (214, 176, 60)}),
        "schoolmarm": frog_painter({"kind": "schoolmarm", "green": (80, 150, 70), "light": (160, 200, 120),
                                    "spectacles": True, "tunic": (60, 70, 110), "trim": (236, 232, 222),
                                    "scarf": (236, 232, 222), "cuff": (236, 232, 222), "belt": (40, 40, 60),
                                    "buckle": (200, 180, 120)}),
        "cook": frog_painter({"kind": "cook", "green": (72, 156, 56), "light": (150, 196, 100),
                              "tunic": (232, 230, 222), "apron": (214, 206, 186), "apron_stains": (150, 100, 60),
                              "pants": (90, 80, 70), "belt": (214, 206, 186), "buckle": (214, 206, 186)}),
        "fishmonger": frog_painter({"kind": "fishmonger", "green": (60, 146, 70), "light": (140, 190, 110),
                                    "tunic": (180, 196, 210), "apron": (236, 236, 236), "apron_stripes": (60, 90, 150),
                                    "cuff": (60, 90, 150), "pants": (80, 70, 60), "belt": (60, 44, 30),
                                    "buckle": (180, 150, 100), "boots": (40, 60, 44)}),
    }),
    "goxa": (goxa_parts, {
        "havu": goxa_painter({"kind": "havu", "cloak": (236, 232, 222), "skin": (78, 128, 166)}),
        "seminata": goxa_painter({"kind": "seminata", "cloak": (206, 52, 34), "hat": (206, 52, 34),
                                  "wrap": (226, 86, 40), "skin": (96, 136, 170)}),
    }),
    "bat": (bat_parts, {
        "folk": bat_painter({"kind": "folk", "fur": (82, 64, 56), "light": (150, 128, 116), "wing": (186, 120, 112),
                             "eye": (220, 40, 30), "pants": (190, 164, 120)}),
        "flapper": bat_painter({"kind": "flapper", "fur": (82, 64, 56), "light": (150, 128, 116),
                                "wing": (186, 120, 112), "eye": (220, 40, 30), "dress": (32, 30, 32),
                                "gold": (206, 170, 90), "pearl": (240, 236, 226)}),
        "barman": bat_painter({"kind": "barman", "fur": (90, 70, 56), "light": (160, 136, 118),
                               "wing": (186, 120, 112), "eye": (30, 26, 26), "shirt": (236, 228, 208),
                               "vest": (62, 46, 36), "apron": (110, 76, 48), "tie": (150, 30, 30),
                               "gold": (212, 172, 70)}),
        "jazz": bat_painter({"kind": "jazz", "fur": (88, 64, 52), "light": (214, 160, 150), "wing": (196, 128, 120),
                             "eye": (236, 170, 40), "coat": (44, 42, 44), "shirt": (230, 222, 200),
                             "scarf": (176, 34, 34)}),
        "bookie": bat_painter({"kind": "bookie", "fur": (70, 58, 54), "light": (150, 126, 112), "wing": (176, 116, 106),
                               "eye": (240, 200, 60), "shirt": (226, 218, 196), "vest": (150, 120, 60),
                               "check": (90, 56, 30), "tie": (40, 110, 60), "pants": (60, 52, 46)}),
    }),
    "lilbun": (lilbun_parts, {
        "farmer": lilbun_painter({"cap": (206, 54, 54), "cap_shade": (166, 44, 44), "spot": (242, 236, 226),
                                  "under": (242, 216, 132), "stalk": (240, 228, 206), "hands": (150, 108, 82),
                                  "overalls": (78, 110, 182), "button": (236, 200, 70)}),
        "friend": lilbun_painter({"cap": (206, 54, 54), "cap_shade": (166, 44, 44), "spot": (242, 236, 226),
                                  "under": (242, 216, 132), "stalk": (240, 228, 206), "hands": (150, 108, 82)}),
    }),
}


def f(x):
    s = f"{float(x):.3f}".rstrip("0")
    return (s + "0" if s.endswith(".") else s) + "F"


def main():
    os.makedirs(TEX_OUT, exist_ok=True)
    java_parts, sizes_out = [], {}
    for race, (make_parts, variants) in RACES.items():
        parts = make_parts()
        sizes, refs = [], []
        for p in parts:
            for bi, b in enumerate(p["boxes"]):
                sizes.append((2 * b["d"] + 2 * b["w"], b["d"] + b["h"]))
                refs.append((p["name"], bi))
        positions, tex_h = pack(sizes, TEX_W)
        tex_h = max(tex_h, TEX_W)
        uv = dict(zip(refs, positions))
        for p in parts:
            p["uvs"] = [uv[(p["name"], bi)] for bi in range(len(p["boxes"]))]
        sizes_out[race] = tex_h
        for variant, painter in variants.items():
            img = Image.new("RGBA", (TEX_W * SCALE, tex_h * SCALE), CLEAR)
            for p in parts:
                if p["only"] and variant not in p["only"]:
                    continue                      # this variant doesn't have the part: leave its texture clear
                for b, (u, v) in zip(p["boxes"], p["uvs"]):
                    for tx, ty, face, pt, n in face_pixels((b["x"], b["y"], b["z"]), (b["w"], b["h"], b["d"]), (u, v), SCALE):
                        img.putpixel((tx, ty), painter(p, b, face, tx, ty, pt, n))
            img.save(os.path.join(TEX_OUT, f"{race}_{variant}.png"))
        java_parts += [(race, p) for p in parts]

    lines = []
    for race, p in java_parts:
        boxes = ", ".join(f"new Box({u}, {v}, {f(b['x'])}, {f(b['y'])}, {f(b['z'])}, {b['w']}, {b['h']}, {b['d']})"
                          for b, (u, v) in zip(p["boxes"], p["uvs"]))
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        parent = f"\"{p['parent']}\"" if p["parent"] else "null"
        only = ", ".join(f"\"{v}\"" for v in (p["only"] or []))
        lines.append(f"            new Part(\"{race}\", \"{p['name']}\", {parent}, new String[]{{{only}}}, "
                     f"{f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)},\n                    new Box[]{{{boxes}}}),")
    heights = "\n".join(f"            case \"{r}\" -> {h};" for r, h in sizes_out.items())

    java = f"""package com.childrenofmu.client.model;

// GENERATED by tools/make_folk_assets.py. Don't edit by hand; change the script and re-run it.

/**
 * Geometry for the walking folk. Every race is built at the same design size (feet at y = 24, y down, face toward
 * -z) and scaled down in game by its race's scale. Parts with a parent are positioned relative to that parent.
 */
final class FolkShapes {{
    static final int TEX_WIDTH = {TEX_W};

    static int texHeight(String race) {{
        return switch (race) {{
{heights}
            default -> {TEX_W};
        }};
    }}

    record Box(int u, int v, float x, float y, float z, int w, int h, int d) {{}}

    /** Rotations are in degrees. variants: the variants that have this part (empty = all of them). */
    record Part(String race, String name, String parent, String[] variants, float pivotX, float pivotY, float pivotZ,
                float rotX, float rotY, float rotZ, Box[] boxes) {{}}

    static final Part[] PARTS = {{
{chr(10).join(lines)}
    }};

    private FolkShapes() {{}}
}}
"""
    with open(JAVA_OUT, "w", newline="\n") as fh:
        fh.write(java)
    print(f"{len(java_parts)} parts; textures: " + ", ".join(f"{r} {TEX_W}x{h}" for r, h in sizes_out.items()))


if __name__ == "__main__":
    main()
