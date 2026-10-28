import anvil, json, gzip, re, sys
REG="/mnt/user-data/uploads/ChildrenOfMu/run/saves/New Worldad/region/r.-7.2.mca"
SDIR="/home/claude/ChildrenOfMu/src/main/resources/data/childrenofmu/structure/"
def city_pieces():
    for c in anvil.chunks(REG):
        s=c.get('structures',{}).get('starts',{}).get('childrenofmu:rcb_city')
        if s and 'Children' in s:
            return [p for p in s['Children'] if p['id']=='childrenofmu:ithk_building']
_tc={}
def template(name):
    if name not in _tc:
        t=anvil.read_nbt(gzip.decompress(open(SDIR+name.split(':')[1]+'.nbt','rb').read()))
        pal=[anvil.state_name(s) for s in t['palette']]
        blocks={tuple(b['pos']):pal[b['state']] for b in t['blocks']}
        _tc[name]=(t['size'],blocks)
    return _tc[name]
def to_local(p, piece):
    size,_=template(piece['Template'])
    px,pz=size[0]//2,size[2]//2
    rx,ry,rz=p[0]-piece['TPX'],p[1]-piece['TPY'],p[2]-piece['TPZ']
    rot=piece['Rot']
    if rot=='NONE': return (rx,ry,rz)
    if rot=='CLOCKWISE_90': return (rz-pz+px, ry, px+pz-rx)
    if rot=='COUNTERCLOCKWISE_90': return (px+pz-rz, ry, rx-px+pz)
    return (2*px-rx, ry, 2*pz-rz)
def to_world(l, piece):
    size,_=template(piece['Template'])
    px,pz=size[0]//2,size[2]//2
    x,y,z=l; rot=piece['Rot']
    if rot=='NONE': r=(x,y,z)
    elif rot=='CLOCKWISE_90': r=(px+pz-z,y,pz-px+x)
    elif rot=='COUNTERCLOCKWISE_90': r=(px-pz+z,y,px+pz-x)
    else: r=(2*px-x,y,2*pz-z)
    return (r[0]+piece['TPX'],r[1]+piece['TPY'],r[2]+piece['TPZ'])
# world dir -> template dir (inverse rotation)
DIRS=['north','east','south','west']
def w2t_dir(d, rot):
    steps={'NONE':0,'CLOCKWISE_90':1,'CLOCKWISE_180':2,'COUNTERCLOCKWISE_90':3}[rot]
    return DIRS[(DIRS.index(d)-steps)%4]
