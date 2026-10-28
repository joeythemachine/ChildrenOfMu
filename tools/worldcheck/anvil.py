"""Minimal Anvil reader: blocks of a region file -> {(x,y,z): name+props} for a box."""
import struct, zlib, gzip, io, sys, json

def read_nbt(buf):
    f = io.BytesIO(buf)
    def rd(n): return f.read(n)
    def tag(t):
        if t == 1: return struct.unpack('>b', rd(1))[0]
        if t == 2: return struct.unpack('>h', rd(2))[0]
        if t == 3: return struct.unpack('>i', rd(4))[0]
        if t == 4: return struct.unpack('>q', rd(8))[0]
        if t == 5: return struct.unpack('>f', rd(4))[0]
        if t == 6: return struct.unpack('>d', rd(8))[0]
        if t == 7: n = struct.unpack('>i', rd(4))[0]; return rd(n)
        if t == 8: n = struct.unpack('>H', rd(2))[0]; return rd(n).decode('utf-8', 'replace')
        if t == 9:
            et = rd(1)[0]; n = struct.unpack('>i', rd(4))[0]
            return [tag(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = rd(1)[0]
                if tt == 0: return d
                nl = struct.unpack('>H', rd(2))[0]; name = rd(nl).decode('utf-8', 'replace')
                d[name] = tag(tt)
        if t == 11: n = struct.unpack('>i', rd(4))[0]; return list(struct.unpack('>%di' % n, rd(4 * n)))
        if t == 12: n = struct.unpack('>i', rd(4))[0]; return list(struct.unpack('>%dq' % n, rd(8 * n)))
        raise ValueError(t)
    t = rd(1)[0]; nl = struct.unpack('>H', rd(2))[0]; rd(nl)
    return tag(t)

def chunks(path):
    data = open(path, 'rb').read()
    for i in range(1024):
        off = int.from_bytes(data[i*4:i*4+3], 'big'); cnt = data[i*4+3]
        if off == 0: continue
        p = off * 4096
        ln = struct.unpack('>i', data[p:p+4])[0]; comp = data[p+4]
        raw = data[p+5:p+4+ln]
        raw = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
        yield read_nbt(raw)

def state_name(s):
    n = s['Name']
    if 'Properties' in s:
        n += '[' + ','.join(f'{k}={v}' for k, v in sorted(s['Properties'].items())) + ']'
    return n

def blocks(path, x0, x1, z0, z1, y0=-64, y1=320):
    out = {}
    for c in chunks(path):
        cx, cz = c['xPos'], c['zPos']
        if cx*16 > x1 or cx*16+15 < x0 or cz*16 > z1 or cz*16+15 < z0: continue
        for sec in c.get('sections', []):
            sy = sec['Y']
            if sy*16 > y1 or sy*16+15 < y0: continue
            bs = sec.get('block_states')
            if not bs: continue
            pal = [state_name(s) for s in bs['palette']]
            if len(pal) == 1:
                if pal[0] == 'minecraft:air': continue
                idx = [0]*4096
            else:
                bits = max(4, (len(pal)-1).bit_length()); per = 64 // bits; mask = (1 << bits) - 1
                idx = []
                for v in bs['data']:
                    v &= (1 << 64) - 1
                    for k in range(per):
                        idx.append((v >> (k*bits)) & mask)
                idx = idx[:4096]
            for i, pi in enumerate(idx):
                name = pal[pi]
                if name == 'minecraft:air' or name == 'minecraft:cave_air': continue
                x = cx*16 + (i & 15); z = cz*16 + ((i >> 4) & 15); y = sy*16 + (i >> 8)
                if x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1:
                    out[(x, y, z)] = name
    return out
