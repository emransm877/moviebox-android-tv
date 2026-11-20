"""Check version, identity and public artifact naming contracts before release."""
import json,re,sys,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
data=json.loads((ROOT/'release.json').read_text(encoding='utf-8'))
version=data['source_version'];published=data['published_version']
gradle=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
assert re.search(r"versionName\s+'"+re.escape(version)+"'",gradle)
assert re.search(r'versionCode\s+'+str(data['version_code'])+r'\b',gradle)
assert data['asset']==f'MovieBox-Android-TV-{published}.apk'
assert data['tag']=='v'+published
assert data['application_id'] in gradle
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
assert 'android:label="'+data['installed_name']+'"' in manifest
updates=(ROOT/'app/src/main/java/org/emransm/moviebox/tv/Updates.java').read_text(encoding='utf-8')
assert 'VERSION="'+version+'"' in updates and data['repository'] in updates
assert 'MovieBoxAndroidTV/'+version in (ROOT/'app/src/main/java/org/emransm/moviebox/tv/Api.java').read_text(encoding='utf-8')
shared=(ROOT/'app/src/main/assets/tv-ui/js/releases.js').read_text(encoding='utf-8')
assert "VERSION = '"+version+"'" in shared and data['repository'] in shared
readme=(ROOT/'README.md').read_text(encoding='utf-8')
assert data['asset'] in readme and '/releases/download/'+data['tag']+'/' in readme
for record in json.loads((ROOT/'ui-source.json').read_text(encoding='utf-8'))['files']:
    path=ROOT/'app/src/main/assets/tv-ui'/record['path']
    assert hashlib.sha256(path.read_bytes()).hexdigest()==record['androidSha256'],record['path']
    if path.suffix=='.css':assert not record['modifiedForPlatform'],record['path']
for path in ROOT.glob('docs/*.md'):
    for target in re.findall(r'!?\[[^\]\n]*\]\(([^)\s]+)\)',path.read_text(encoding='utf-8')):
        if '://' not in target and not target.startswith('#'):assert (path.parent/target.split('#')[0]).exists(),(path,target)
print('Release identity, versions, APK filename, updater and shared UI hashes are consistent.')
