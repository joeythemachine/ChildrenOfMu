import pickle,sys
starts,B=pickle.load(open(sys.argv[1],'rb'))
walls=[p for p in starts if p['id']=='childrenofmu:city_wall']
keep=walls[0]['Keep'];boxes=[keep[i:i+5] for i in range(0,len(keep),5)]
def kept(x,z): return any(b[0]<=x<=b[2] and b[1]<=z<=b[3] for b in boxes)
PASS=(':air',':cave_air','vine','torch','lantern','carpet','short_grass','tall_grass','fern','flower','button','sign','ladder','rail','snow[layers=1')
def name(x,y,z): return B.get((x,y,z),'')
def passable(n): return n=='' or any(k in n for k in PASS) or n.endswith(':air')
def height(n,y):
    b=n.split('[')[0]
    if 'slab' in b and 'type=bottom' in n: return y+0.5
    if 'stairs' in b and 'half=bottom' in n: return y+0.5   # can step up
    if 'fence' in b or b.endswith('_wall') : return y+1.5
    if 'carpet' in b: return y+0.0625
    return y+1
def stand(x,z,lo,hi):
    out=[]
    for y in range(hi,lo-1,-1):
        n=name(x,y,z)
        if passable(n): continue
        h=height(n,y)
        if h>y+1.2: continue
        hy=int(h) if h==int(h) else int(h)+0  # head starts at
        # need 1.8 clear above h
        top=h+1.8; ok=True; yy=int(h) if h==int(h) else int(h)+1
        if h!=int(h): yy=int(h)+1
        while yy<top:
            if not passable(name(x,yy,z)): ok=False;break
            yy+=1
        if ok: out.append(h)
    return out
res=[]
for w in walls:
    x0,z0,x1,z1,y0,y1=w['X0'],w['Z0'],w['X1'],w['Z1'],w['Y0'],w['Y1']
    dx,dz=x1-x0,z1-z0;n=max(abs(dx),abs(dz))
    ix,iz=-(dz>0)+(dz<0),(dx>0)-(dx<0)
    diag=bool(ix and iz)
    prev=None;line='';info=[]
    for t in range(n+1):
        x=x0+round(dx*t/max(1,n)); z=z0+round(dz*t/max(1,n))
        g=round(y0+(y1-y0)*t/max(1,n))-1
        cells=[(x+ix,z),(x,z+iz),(x+ix,z+iz),(x+2*ix,z+iz),(x+ix,z+2*iz)] if diag else [(x+ix,z+iz),(x+2*ix,z+2*iz)]
        hs=set()
        for c in cells:
            if kept(*c): continue
            for h in stand(c[0],c[1],g+2,g+12): hs.add(h)
        if kept(x,z) and kept(x+ix,z+iz): line+='K';prev=None;continue
        if not hs: line+='?';prev=None;info.append((t,(x,z),'none'));continue
        if prev is None: line+='.'
        else:
            best=min(abs(a-b) for a in hs for b in prev)
            if best<=0.5: line+='.'
            elif best<=1.0: line+='j';info.append((t,(x,z),'jump',sorted(prev),sorted(hs)))
            else: line+='X';info.append((t,(x,z),'gap',sorted(prev),sorted(hs)))
        prev=hs
    print((x0,z0),'->',(x1,z1),line)
    for i in info[:12]: print('    ',i)
