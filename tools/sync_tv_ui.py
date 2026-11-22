"""Reuse the reviewed TV frontend; keep its CSS and artwork files byte-identical.

Only platform adapters and platform-specific copy are changed. Native Android
networking, playback, keyboard and lifecycle live outside the shared design.
"""
import argparse,hashlib,json,re,shutil
from pathlib import Path

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=root.parent/'MovieBoxPublic/moviebox-tizen/app');args=parser.parse_args()
source=args.source.resolve();target=root/'app/src/main/assets/tv-ui'
if not (source/'index.html').is_file():raise SystemExit('Pass the existing Tizen app directory with --source.')
records=[]
for path in source.rglob('*'):
    if not path.is_file() or (path.suffix.lower() not in {'.html','.css','.js','.svg','.png','.webp','.json'} and path.name.upper() not in {'LICENSE','LICENSE.TXT','NOTICE','NOTICE.TXT'}):continue
    relative=path.relative_to(source);destination=target/relative;destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,destination)
    records.append({'path':relative.as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})

def edit(name,old,new):
    path=target/name;text=path.read_text(encoding='utf-8')
    if old not in text:raise SystemExit('Missing platform adapter marker: '+name)
    path.write_text(text.replace(old,new),encoding='utf-8')

edit('index.html','width=1920, initial-scale=1','width=1920')
edit('index.html','<script src="js/theme-bootstrap.js"></script>','<script src="js/android-preflight.js"></script><script src="js/android-bootstrap.js"></script><script src="js/theme-bootstrap.js"></script>')
edit('index.html','<script src="$WEBAPIS/webapis/webapis.js"></script>','')
edit('index.html','<script src="js/api.js"></script>','<script src="js/api.js"></script><script src="js/android-adapter.js"></script>')
edit('index.html','</head>','<link rel="stylesheet" href="css/android-platform.css"></head>')
edit('index.html','Samsung native player','Android Media3 player')
edit('index.html','Samsung native video','Android native video')
edit('index.html','Auto uses the TV player for compatible video.','Android plays compatible video and audio tracks natively.')
edit('index.html','On your computer, open the release page below and download its TV package. Follow the installation guide for your TV. This app never installs updates automatically.','Open the release page on your phone or computer and download the Android TV APK. Install it using your Android TV’s normal sideloading process. Updates are optional.')
edit('js/media-agent-bootstrap.js',"  'use strict';","  'use strict';\n  if(global.MovieBoxAndroidEnvironment){global.MovieBoxOriginalMediaAgent=navigator.userAgent;global.MovieBoxMediaUserAgent=global.MovieBoxAndroidEnvironment.userAgent;global.MovieBoxMediaAgentReady=Promise.resolve(true);return;}")
edit('js/api.js',"this.preview = !root.tizen", "this.preview = !root.MovieBoxAndroidEnvironment && !root.tizen")
edit('js/api.js',"sp_code:'20801','X-Child-UID':'','X-Play-Mode':'2','X-Idle-Data':'1','X-Family-Mode':'0','X-Content-Mode':'0'};", "sp_code:'20801','X-Child-UID':'','X-Play-Mode':'2','X-Idle-Data':'1','X-Family-Mode':'0','X-Content-Mode':'0'};\n      if(root.MovieBoxAndroidEnvironment){var device=root.MovieBoxAndroidEnvironment;Object.assign(this.client,{os:'android',os_version:device.osVersion,brand:device.brand,model:device.model,install_ch:'independent',install_store:'sideload'});}")
edit('js/player.js',"this.native = !!(global.webapis && webapis.avplay); this.av = this.native ? webapis.avplay : null;", "this.native = !!(global.MovieBoxAndroidAVPlayer || (global.webapis && webapis.avplay)); this.av = global.MovieBoxAndroidAVPlayer || (this.native ? webapis.avplay : null);")
edit('js/player.js',"this.native ? 'SAMSUNG NATIVE PLAYER' : 'BROWSER PREVIEW'", "global.MovieBoxAndroidEnvironment ? 'ANDROID NATIVE PLAYER' : this.native ? 'SAMSUNG NATIVE PLAYER' : 'BROWSER PREVIEW'")
edit('js/media-transport.js',"var realTV=!!(global.tizen&&tizen.websetting)","var realTV=!!(global.MovieBoxAndroidEnvironment||(global.tizen&&tizen.websetting))")
edit('js/media-transport.js',"var web=realTV&&mode", "var web=realTV&&!global.MovieBoxAndroidEnvironment&&mode")
edit('js/media-transport.js',"native?'SAMSUNG NATIVE PLAYER'", "native?(global.MovieBoxAndroidEnvironment?'ANDROID NATIVE PLAYER':'SAMSUNG NATIVE PLAYER')")
edit('js/inline-controls.js',"return global.tizen&&tizen.tvaudiocontrol;","return global.MovieBoxAndroidAudio || (global.tizen&&tizen.tvaudiocontrol);")
edit('js/app.js',"if (global.tizen && tizen.application) tizen.application.getCurrentApplication().exit();", "if(global.MovieBoxAndroidBridge){global.MovieBoxAndroidBridge.exit();return;} if (global.tizen && tizen.application) tizen.application.getCurrentApplication().exit();")
edit('js/app.js',"$('settings-platform').textContent = global.tizen ?", "$('settings-platform').textContent = global.MovieBoxAndroidEnvironment ? 'Android TV · Saved titles and subtitles stay on this TV.' : global.tizen ?")
edit('js/app.js',"localStorage.setItem('moviebox-tv-library-backup',serialized);", "localStorage.setItem('moviebox-tv-library-backup',serialized);if(global.MovieBoxAndroidBridge)global.MovieBoxAndroidBridge.saveState(serialized);")
edit('js/app.js',"global.MovieBoxTV =", "global.MovieBoxAndroidFlush=function(){if(player.active){saveProgress(player.position,player.duration);}persist(true);};\n  global.MovieBoxTV =")
edit('js/releases.js',"REPOSITORY = 'emransm877/moviebox-tizen', VERSION = '0.9.3'", "REPOSITORY = 'emransm877/moviebox-android-tv', VERSION = '"+json.loads((root/'release.json').read_text(encoding='utf-8'))['source_version']+"'")
for name in ['android-preflight.js','android-bootstrap.js','android-adapter.js']:
    shutil.copyfile(root/'platform'/name,target/'js'/name)
shutil.copyfile(root/'platform/android-platform.css',target/'css/android-platform.css')
for record in records:
    path=target/record['path'];record['androidSha256']=hashlib.sha256(path.read_bytes()).hexdigest();record['modifiedForPlatform']=record['sha256']!=record['androidSha256']
    if path.suffix=='.css' and record['modifiedForPlatform']:raise SystemExit('Shared stylesheet was modified: '+str(path))
(root/'ui-source.json').write_text(json.dumps({'sourceProject':'emransm877/moviebox-tizen','sourceVersion':'0.9.3 source branch','files':records},indent=2),encoding='utf-8')
print('Copied',len(records),'shared UI assets; original stylesheets remain byte-identical.')
