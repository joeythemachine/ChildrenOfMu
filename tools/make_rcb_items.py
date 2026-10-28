"""
Textures and models for RCB City's food and drink: Calf (the Cats' alcoholic milk), Calf Bite (Calf with chicken
blood thrown in) and fermented tuna.
    python tools/make_rcb_items.py
"""
import os

from make_frog_food import A, canvas, oval, px, write_json, NS


def bottle(liquid, foam, tint=None):
    """A squat brown-glass bottle with a cork, full of something milky."""
    im = canvas()
    glass = (120, 90, 60)
    for y in range(6, 15):
        w = 3 if y > 7 else 2
        for x in range(8 - w - 1, 8 + w + 1):
            edge = x in (8 - w - 1, 8 + w)
            px(im, x, y, glass if edge or y == 14 else liquid)
    for x in range(6, 10):
        px(im, x, 7, foam)                     # a head of froth
    for y in range(3, 6):
        for x in (7, 8):
            px(im, x, y, glass)
    px(im, 7, 2, (170, 130, 80)); px(im, 8, 2, (170, 130, 80))   # the cork
    px(im, 6, 9, (250, 250, 250)); px(im, 6, 10, (240, 240, 240))  # a glint
    if tint:
        for (x, y) in ((7, 11), (9, 12), (8, 10), (6, 12), (9, 9)):
            px(im, x, y, tint)                 # swirls of blood
    return im


def fermented_tuna():
    im = canvas()
    oval(im, 8, 9, 6, 3.2, (150, 120, 110), (120, 92, 84))
    for x in range(3, 13):
        px(im, x, 8, (200, 110, 110) if x % 3 else (170, 90, 90))    # the red flesh showing
    for x, y in ((2, 7), (1, 6), (1, 10), (2, 11)):
        px(im, x, y, (110, 120, 110))           # the tail
    px(im, 13, 8, (30, 30, 30))                 # an eye
    for x, y in ((5, 4), (8, 3), (11, 4), (6, 2)):
        px(im, x, y, (150, 190, 90))            # the smell, visibly
    return im


def main():
    items = {"calf": bottle((236, 232, 214), (250, 248, 240)),
             "calf_bite": bottle((214, 150, 140), (236, 220, 210), tint=(150, 30, 30)),
             "fermented_tuna": fermented_tuna()}
    for name, im in items.items():
        im.save(os.path.join(A, "textures", "item", name + ".png"))
        write_json(os.path.join(A, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    print("RCB items written")


if __name__ == "__main__":
    main()
