"""Create test-owned media; never download a copyrighted film or trailer."""
from pathlib import Path
import subprocess

target=Path(__file__).resolve().parents[1]/'app/src/androidTest/assets/fixture.mp4'
target.parent.mkdir(parents=True,exist_ok=True)
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','lavfi','-i',
    'testsrc2=size=1920x1080:rate=24','-f','lavfi','-i',
    'sine=frequency=440:sample_rate=48000','-t','65','-c:v','libx264','-preset','ultrafast',
    '-crf','30','-pix_fmt','yuv420p','-c:a','aac','-b:a','64k','-movflags','+faststart',str(target)],check=True)
print('Generated',target)
