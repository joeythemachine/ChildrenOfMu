import json
from collections import Counter
ch=json.load(open("changes1.json"))
def base(n): return n.replace('minecraft:','').split('[')[0]
def noise(a,b):
    A,B=base(a),base(b)
    if 'vine' in (A,B) and {A,B}<= {'vine','air'}: return True
    if {A,B}<={'grass_block','dirt','short_grass','air','dandelion','tall_grass','fern'} and ('grass_block' in (A,B) or 'dirt' in (A,B) or A=='short_grass' or A=='dandelion'): return True
    if A==B and any(k in A for k in ('bed','door','manhole','trapdoor','fence_gate')): return True
    if 'amethyst' in A or 'amethyst' in B: return True
    return False
sig=[(tuple(p),a,b) for p,a,b in ch if not noise(a,b)]
print(len(sig),"significant of",len(ch))
json.dump([[list(p),a,b] for p,a,b in sig],open("sig1.json","w"))
# same-name prop changes
c=Counter()
for p,a,b in sig:
    if base(a)==base(b): c[(a,b)]+=1
for k,v in c.most_common(10): print(v,k)
