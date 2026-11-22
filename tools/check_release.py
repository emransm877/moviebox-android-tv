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
if 'candidate_asset' in data:
    assert data['candidate_asset']==f'MovieBox-Android-TV-{version}.apk'
    assert data['candidate_tag']=='v'+version
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
for name,folder in [('android-preflight.js','js'),('android-bootstrap.js','js'),('android-adapter.js','js'),('android-platform.css','css')]:
    assert (ROOT/'platform'/name).read_bytes()==(ROOT/'app/src/main/assets/tv-ui'/folder/name).read_bytes(),'Packaged Android platform adapter differs: '+name
suspect=re.compile(r'\u00c2[\u0080-\u00bf]|\u00c3[\u0080-\u00bf]|\u00e2[\u0080-\u009f\u0152\u0153\u0160\u0161\u0178\u017d\u017e\u0192\u02c6\u02dc\u2013-\u2022\u2026\u2030\u2039\u203a\u20ac\u2122]{2}|\ufffd')
for path in (ROOT/'app/src/main/assets/tv-ui').rglob('*'):
    if path.is_file() and path.suffix in {'.html','.js','.css','.json'}:
        text=path.read_text(encoding='utf-8-sig')
        assert not suspect.search(text),'Potential double-decoded UI source: '+str(path.relative_to(ROOT))
for path in ROOT.glob('docs/*.md'):
    for target in re.findall(r'!?\[[^\]\n]*\]\(([^)\s]+)\)',path.read_text(encoding='utf-8')):
        if '://' not in target and not target.startswith('#'):assert (path.parent/target.split('#')[0]).exists(),(path,target)
print('Release identity, versions, APK filename, updater and shared UI hashes are consistent.')
