import anvil,pickle,glob,sys
R=sys.argv[1]
starts=None
for f in glob.glob(R+"r.*.mca"):
    for c in anvil.chunks(f):
        s=c.get('structures',{}).get('starts',{}).get('childrenofmu:rcb_city')
        if s and 'Children' in s: starts=s['Children'];break
    if starts: break
walls=[p for p in starts if p['id']=='childrenofmu:city_wall']
xs=[v for w in walls for v in (w['BB'][0],w['BB'][3])]; zs=[v for w in walls for v in (w['BB'][2],w['BB'][5])]
B={}
for f in glob.glob(R+"r.*.mca"):
    B.update(anvil.blocks(f,min(xs),max(xs),min(zs),max(zs),50,110))
pickle.dump((starts,B),open(''+(sys.argv[2] if len(sys.argv)>2 else '/tmp/claude-0/city3.pkl')+'','wb'))
print(min(xs),max(xs),min(zs),max(zs),len(B))
for w in walls: print(w['X0'],w['Z0'],w['Y0'],'->',w['X1'],w['Z1'],w['Y1'])
