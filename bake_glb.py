import struct, json, sys, numpy as np
GLB_PATH = sys.argv[1] if len(sys.argv) > 1 else "psx_snatcher_-_low_poly_horror.glb"
OUT_BIN  = sys.argv[2] if len(sys.argv) > 2 else "snatcher.bin"
OUT_PNG  = sys.argv[3] if len(sys.argv) > 3 else "snatcher.png"
TEX_SIZE = 512          # texture resolution (original is 2048). Lower = more PSX.
FRAME_STEP = 3          # keep 1 of every N source frames (30fps source -> 10fps; the game interpolates)
f=open(GLB_PATH,"rb").read()
off=12; chunks=[]
while off<len(f):
    cl,ct=struct.unpack('<I4s',f[off:off+8]); chunks.append(f[off+8:off+8+cl]); off+=8+cl
j=json.loads(chunks[0]); b=chunks[1]
def acc(i):
    a=j['accessors'][i]; bv=j['bufferViews'][a['bufferView']]
    n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}[a['type']]
    dt={5126:'<f4',5123:'<u2',5125:'<u4',5121:'u1'}[a['componentType']]
    o=bv.get('byteOffset',0)+a.get('byteOffset',0)
    return np.frombuffer(b,dtype=dt,count=a['count']*n,offset=o).reshape(a['count'],n)
nodes=j['nodes']; N=len(nodes)
parent=[-1]*N
for i,n in enumerate(nodes):
    for c in n.get('children',[]): parent[c]=i
def quat_mat(q):
    x,y,z,w=q
    return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w),0],
                     [2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w),0],
                     [2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y),0],[0,0,0,1]])
def local(n,t=None,r=None,s=None):
    t=n.get('translation',[0,0,0]) if t is None else t
    r=n.get('rotation',[0,0,0,1]) if r is None else r
    s=n.get('scale',[1,1,1]) if s is None else s
    if 'matrix' in n: return np.array(n['matrix']).reshape(4,4).T
    T=np.eye(4); T[:3,3]=t
    S=np.diag([*s,1.0])
    return T@quat_mat(r)@S
order=[]
def visit(i):
    order.append(i)
    for c in nodes[i].get('children',[]): visit(c)
for r in [i for i in range(N) if parent[i]==-1]: visit(r)
skin=j['skins'][0]; joints=skin['joints']
ibm=acc(skin['inverseBindMatrices']).reshape(-1,4,4).transpose(0,2,1)
pos=acc(0).astype(np.float64); nor=acc(1).astype(np.float64); uv=acc(2).astype(np.float64)
idx=acc(3).reshape(-1).astype(np.uint32)
jt=acc(5).astype(int); wt=acc(6).astype(np.float64)
an=j['animations'][0]
ts=acc(an['samplers'][0]['input']).reshape(-1)
chan={}
for ch in an['channels']:
    s=an['samplers'][ch['sampler']]
    chan[(ch['target']['node'],ch['target']['path'])]=(acc(s['input']).reshape(-1),acc(s['output']),s.get('interpolation','LINEAR'))
def slerp(a,b,t):
    d=np.dot(a,b)
    if d<0: b=-b; d=-d
    if d>0.9995:
        r=a+t*(b-a); return r/np.linalg.norm(r)
    th=np.arccos(d); return (np.sin((1-t)*th)*a+np.sin(t*th)*b)/np.sin(th)
def sample(node,path,t,default):
    k=(node,path)
    if k not in chan: return default
    tin,out,interp=chan[k]
    if t<=tin[0]: return out[0]
    if t>=tin[-1]: return out[-1]
    i=np.searchsorted(tin,t)-1
    u=(t-tin[i])/(tin[i+1]-tin[i])
    if path=='rotation': return slerp(out[i],out[i+1],u)
    return out[i]*(1-u)+out[i+1]*u
def world_at(t):
    W=[None]*N
    for i in order:
        n=nodes[i]
        tr=sample(i,'translation',t,n.get('translation',[0,0,0]))
        ro=sample(i,'rotation',t,n.get('rotation',[0,0,0,1]))
        sc=sample(i,'scale',t,n.get('scale',[1,1,1]))
        L=local(n,tr,ro,sc)
        W[i]=L if parent[i]==-1 else W[parent[i]]@L
    return W
def skinned(t):
    W=world_at(t)
    M=np.array([W[joints[k]]@ibm[k] for k in range(len(joints))])
    # per-vertex blended matrix
    Mv=np.einsum('vk,vkij->vij',wt,M[jt])
    p=np.concatenate([pos,np.ones((len(pos),1))],1)
    P=np.einsum('vij,vj->vi',Mv,p)[:,:3]
    Nn=np.einsum('vij,vj->vi',Mv[:,:3,:3],nor)
    Nn/= np.linalg.norm(Nn,axis=1,keepdims=True)+1e-9
    return P,Nn,W

# ---------------------------------------------------------------------------
# BAKE: skin the mesh for every animation frame and write a compact binary
# ---------------------------------------------------------------------------
from PIL import Image
import io

t0, t1 = float(ts[0]), float(ts[-1])
dur = t1 - t0
F = int(round((dur * 30.0) / FRAME_STEP))          # number of baked frames
times = [t0 + k * dur / F for k in range(F)]        # loop-safe: frame F == frame 0
fps = F / dur

all_pos, all_nor = [], []
for t in times:
    P, Nn, _ = skinned(t)
    all_pos.append(P); all_nor.append(Nn)
all_pos = np.array(all_pos)          # F,V,3
all_nor = np.array(all_nor)

# recentre: feet on y=0, body centred on x/z (median keeps the outstretched arms from skewing it)
ox = float(np.median(all_pos[..., 0])); oz = float(np.median(all_pos[..., 2])); oy = float(all_pos[..., 1].min())
all_pos[..., 0] -= ox; all_pos[..., 1] -= oy; all_pos[..., 2] -= oz

mn = all_pos.reshape(-1, 3).min(0) - 1e-4
mx = all_pos.reshape(-1, 3).max(0) + 1e-4
rng = mx - mn
q = np.round((all_pos - mn) / rng * 65535).astype('<u2')
qn = np.clip(np.round(all_nor * 127), -127, 127).astype('i1')

V = len(pos); T = len(idx) // 3
assert idx.max() < 65536
out = bytearray()
out += b"SNCH"
out += struct.pack("<iiif", V, T, F, fps)
out += struct.pack("<6f", *mn, *rng)
out += uv.astype('<f4').tobytes()
out += idx.astype('<u2').tobytes()
for k in range(F):
    out += q[k].tobytes()
    out += qn[k].tobytes()
open(OUT_BIN, "wb").write(out)

# texture
img = j["images"][0]; bv = j["bufferViews"][img["bufferView"]]
png = b[bv.get("byteOffset", 0): bv.get("byteOffset", 0) + bv["byteLength"]]
tex = Image.open(io.BytesIO(png)).convert("RGBA")
if TEX_SIZE and tex.size[0] != TEX_SIZE:
    tex = tex.resize((TEX_SIZE, TEX_SIZE), Image.BOX)
tex.save(OUT_PNG, optimize=True)

print(f"verts={V} tris={T} frames={F} fps={fps:.3f} loop={F/fps:.2f}s")
print(f"model height={(mx[1]-mn[1]):.3f}  bin={len(out)/1e6:.2f}MB  tex={tex.size}")
