"""Build with an externally supplied signing identity; never generate one here."""
import os,subprocess,sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
required=['MOVIEBOX_TV_KEYSTORE','MOVIEBOX_TV_STORE_PASSWORD','MOVIEBOX_TV_KEY_ALIAS','MOVIEBOX_TV_KEY_PASSWORD']
if any(not os.environ.get(key) for key in required):raise SystemExit('Configure the release signing environment locally. No signing credentials are stored in this repository.')
subprocess.run([sys.executable,str(root/'tools/check_release.py')],check=True,cwd=root)
wrapper=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
raise SystemExit(subprocess.run([str(wrapper),':app:assembleRelease',':app:testReleaseUnitTest',':app:lintRelease'],cwd=root).returncode)
