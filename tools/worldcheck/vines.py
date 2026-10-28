import json, re, place, collections, os
ch=json.load(open('changes1.json'))
ps=place.city_pieces()
OV="/home/claude/ChildrenOfMu/tools/overrides/"
def parse(n):
    m=re.match(r"([^\[]+)(?:\[(.*)\])?$",n)
    return m.group(1), dict(kv.split("=") for kv in m.group(2).split(",")) if m.group(2) else {}
D={'north':(0,0,-1),'east':(1,0,0),'south':(0,0,1),'west':(-1,0,0),'up':(0,1,0)}
per=collections.defaultdict(dict); skipped=collections.Counter()
for p,a,b in sorted(ch,key=lambda e:-e[0][1]):
    if 'vine' not in b or 'vine' in a and False: continue
    nm,props=parse(b)
    if nm!='minecraft:vine': skipped['other']+=1; continue
    cands=[q for q in ps if q['BB'][0]<=p[0]<=q['BB'][3] and q['BB'][1]<=p[1]<=q['BB'][4] and q['BB'][2]<=p[2]<=q['BB'][5]]
    placed=False
    for q in cands:
        size,blocks=place.template(q['Template'])
        l=place.to_local(p,q)
        if not all(0<=l[i]<size[i] for i in range(3)): continue
        cur=blocks.get(l,'minecraft:air')
        if not cur.endswith(':air') and 'vine' not in cur: continue
        tp={}
        for k,v in props.items():
            tp[place.w2t_dir(k,q['Rot']) if k in place.DIRS else k]=v
        att=False
        for k,v in tp.items():
            if v=='true' and k in D:
                d=D[k]; n=blocks.get((l[0]+d[0],l[1]+d[1],l[2]+d[2]))
                if n and not n.endswith(':air') and 'vine' not in n: att=True
        if not att:
            # hanging vine under another vine counts if the one above maps too
            above=blocks.get((l[0],l[1]+1,l[2]))
            if not (tp and (l[0],l[1]+1,l[2]) in per[q['Template']]): continue
        per[q['Template']][l]=tp; placed=True; break
    skipped['placed' if placed else ('nobld' if not cands else 'nomatch')]+=1
print(skipped)
for t,v in per.items(): print(t,len(v))
json.dump({t:[[list(l),'minecraft:vine',pr] for l,pr in v.items()] for t,v in per.items()},open('vine_over.json','w'))
