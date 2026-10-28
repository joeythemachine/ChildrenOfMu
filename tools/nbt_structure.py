"""
Minimal writer for Minecraft structure files (.nbt), using only the Python standard library.
Used by the structure generator scripts in this folder.
"""
import gzip
import struct

DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_BYTE, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 4, 5, 6, 8, 9, 10


class Byte(int):
    pass


class Long(int):
    pass


class Double(float):
    pass


def _tag_type(v):
    if isinstance(v, Byte):
        return TAG_BYTE
    if isinstance(v, Long):
        return TAG_LONG
    if isinstance(v, bool):
        return TAG_BYTE
    if isinstance(v, int):
        return TAG_INT
    if isinstance(v, Double):
        return TAG_DOUBLE
    if isinstance(v, float):
        return TAG_FLOAT
    if isinstance(v, str):
        return TAG_STRING
    if isinstance(v, list):
        return TAG_LIST
    if isinstance(v, dict):
        return TAG_COMPOUND
    raise TypeError(f"can't write {type(v)}")


def _payload(out, t, v):
    if t == TAG_BYTE:
        out += struct.pack(">b", int(v))
    elif t == TAG_INT:
        out += struct.pack(">i", v)
    elif t == TAG_LONG:
        out += struct.pack(">q", v)
    elif t == TAG_FLOAT:
        out += struct.pack(">f", v)
    elif t == TAG_DOUBLE:
        out += struct.pack(">d", v)
    elif t == TAG_STRING:
        b = v.encode("utf-8")
        out += struct.pack(">H", len(b)) + b
    elif t == TAG_LIST:
        et = _tag_type(v[0]) if v else TAG_END
        out += struct.pack(">bi", et, len(v))
        for item in v:
            _payload(out, et, item)
    elif t == TAG_COMPOUND:
        for k, item in v.items():
            it = _tag_type(item)
            kb = k.encode("utf-8")
            out += struct.pack(">bH", it, len(kb)) + kb
            _payload(out, it, item)
        out += struct.pack(">b", TAG_END)


def write_nbt(path, root):
    out = bytearray()
    out += struct.pack(">bH", TAG_COMPOUND, 0)
    _payload(out, TAG_COMPOUND, root)
    with gzip.open(path, "wb") as f:
        f.write(bytes(out))


def write_structure(path, size, blocks, entities=()):
    """
    size: (sx, sy, sz)
    blocks: dict {(x, y, z): (name, {props})} or (name, {props}, {block entity nbt})
    entities: list of (x, y, z, nbt_dict) with x/y/z as floats
    """
    palette, index, block_list = [], {}, []
    for pos in sorted(blocks, key=lambda p: (p[1], p[2], p[0])):
        name, props = blocks[pos][0], blocks[pos][1]
        block_nbt = blocks[pos][2] if len(blocks[pos]) > 2 else None
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {"Name": name}
            if props:
                entry["Properties"] = {k: str(v) for k, v in props.items()}
            palette.append(entry)
        entry = {"pos": list(pos), "state": index[key]}
        if block_nbt:
            entry["nbt"] = block_nbt
        block_list.append(entry)
    ents = []
    for x, y, z, nbt in entities:
        ents.append({
            "pos": [Double(x), Double(y), Double(z)],
            "blockPos": [int(x), int(y), int(z)],
            "nbt": nbt,
        })
    root = {
        "DataVersion": DATA_VERSION,
        "size": list(size),
        "palette": palette,
        "blocks": block_list,
        "entities": ents,
    }
    write_nbt(path, root)
    return len(block_list), len(palette)
