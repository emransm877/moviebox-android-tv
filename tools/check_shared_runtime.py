"""Run the shared UI against real Android Media3 and Android key events.

Requires a debug install and the synthetic acceptance fixture in app-private files.
The catalogue smoke check uses the service; deterministic playback uses our own
generated video and subtitle fixture, not third-party film content.
"""
import json,subprocess,time,hashlib
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
ADB=str(ROOT.parent/'android-sdk/platform-tools/adb.exe')
SERIAL='emulator-5556';PACKAGE='org.emransm.moviebox.tv';out=ROOT/'verification'
checks={};errors=[];passed=False
def adb(*args):return subprocess.run([ADB,'-s',SERIAL,*args],capture_output=True,check=True).stdout
def key(code):adb('shell','input','keyevent',str(code))
def attach(p):
    pid=adb('shell','pidof',PACKAGE).decode().strip()
    adb('forward','tcp:9369','localabstract:webview_devtools_remote_'+pid)
    browser=p.chromium.connect_over_cdp('http://127.0.0.1:9369')
    page=browser.contexts[0].pages[0]
    page.on('pageerror',lambda e:errors.append(str(e)))
    page.wait_for_function('!!window.MovieBoxTV',timeout=20000)
    return browser,page
def fixture(page,series=False,bad=False):
    page.evaluate('''({series,bad})=>{
      const app=MovieBoxTV,subject={subjectId:bad?'generated-failure':series?'generated-series':'generated-movie',subjectType:series?2:1,title:'Generated runtime acceptance',description:'Synthetic local video fixture',hasResource:true};
      window.runtimeSubject=subject;
      delete app.state.saved[subject.subjectId];Object.keys(app.state.history).filter(key=>key.startsWith(subject.subjectId+':')).forEach(key=>delete app.state.history[key]);
      app.api.detail=async()=>({subject,isSeries:series,resource:{seasons:series?[{se:5,episodeNumbers:[4,7]}]:[]}});
      app.api.playback=async()=>[{url:bad?'https://invalid.invalid/no-video.mp4':'file:///data/user/0/org.emransm.moviebox.tv/files/generated-fixture.mp4',resolutions:1080,duration:65,size:12400000,id:'fixture'}];
      app.api.captions=async()=>({captions:[{id:'runtime-en',lan:'en',lanName:'English',url:'https://fixture.invalid/runtime.srt'}]});
      app.api.text=async()=> '1\\n00:00:00,000 --> 00:01:04,000\\nGenerated subtitle acceptance\\n';
      app.openDetail(subject);
    }''',{'series':series,'bad':bad})
    page.wait_for_function("!document.getElementById('watch-button').disabled")
    if series:page.locator('#episodes [data-episode="4"]').click()
    page.evaluate('MovieBoxTV.watch()')
def wait(page,expr,timeout=15000):page.wait_for_function(expr,timeout=timeout)
def capture(name): (out/name).write_bytes(adb('exec-out','screencap','-p'))

with sync_playwright() as p:
    for attempt in range(30):
        try:browser,page=attach(p);break
        except Exception:
            if attempt==29:raise
            time.sleep(.3)
    try:
        wait(page,"document.querySelectorAll('.poster-card').length>0",30000)
        checks['service_catalogue_and_artwork']={'cards':page.locator('.poster-card').count(),'loadedImages':page.evaluate('[...document.images].filter(i=>i.naturalWidth>0).length')}
        checks['viewport']=page.evaluate('({cssWidth:innerWidth,cssHeight:innerHeight,scale:visualViewport.scale,dpr:devicePixelRatio,overflow:document.documentElement.scrollWidth>innerWidth})')
        assert not checks['viewport']['overflow']
        assert abs(checks['viewport']['scale']*checks['viewport']['dpr']-1)<.1,checks['viewport']
        capture('shared-home-runtime.png')
        before=page.evaluate('document.activeElement.outerHTML')
        key(22)
        wait(page,'document.activeElement.outerHTML!=='+json.dumps(before))
        checks['android_dpad_changes_focus']=True
        fixture(page)
        wait(page,'MovieBoxTV.player.position>1000 && !MovieBoxTV.player.preparing')
        assert page.evaluate('MovieBoxTV.player.native && MovieBoxAndroidAVPlayer.getDuration()>60000')
        wait(page,"document.getElementById('subtitle-overlay').textContent.includes('Generated subtitle')")
        checks['native_decode_and_automatic_subtitles']=True
        capture('shared-native-playback.png')
        page.evaluate("document.getElementById('play-pause').focus();MovieBoxTV.player.reveal()")
        key(23);wait(page,'MovieBoxTV.player.paused')
        key(23);wait(page,'!MovieBoxTV.player.paused')
        checks['ok_pauses_and_resumes_without_hiding_controls']=True
        page.evaluate('MovieBoxTV.player.seek(30000)')
        wait(page,'MovieBoxTV.player.position>=29000 && !MovieBoxTV.player.seekInFlight')
        page.evaluate('MovieBoxTV.player.seek(10000)')
        wait(page,'MovieBoxTV.player.position>=9000 && MovieBoxTV.player.position<15000 && !MovieBoxTV.player.seekInFlight')
        page.evaluate('for(let n=0;n<12;n++)MovieBoxTV.player.skip(1000)')
        wait(page,'!MovieBoxTV.player.seekInFlight && MovieBoxTV.player.seekTarget===null')
        checks['forward_backward_and_repeated_seek']=page.evaluate('MovieBoxTV.player.seekDiagnostics')
        page.evaluate('MovieBoxTV.player.selectEmbeddedTrack(MovieBoxTV.player.audioTracks[0].index)')
        wait(page,"MovieBoxTV.player.audioSelection.startsWith('track:')")
        checks['native_embedded_audio_selection']=True
        page.evaluate("MovieBoxTV.player.reveal();document.getElementById('play-pause').focus()")
        key(3);adb('shell','am','start','-n',PACKAGE+'/.SharedTvActivity')
        wait(page,"MovieBoxAndroidAVPlayer.getState()==='PAUSED'",20000)
        key(85);wait(page,'!MovieBoxTV.player.paused && MovieBoxTV.player.position>1000')
        checks['background_resume_releases_and_restores_native_player']=True
        key(4);wait(page,"!MovieBoxTV.player.active && !document.getElementById('detail-page').hidden")
        page.evaluate("document.getElementById('save-button').click();MovieBoxAndroidFlush()")
        assert page.evaluate("!!MovieBoxTV.state.saved['generated-movie']")
        wait(page,"JSON.parse(MovieBoxAndroidBridge.cachedState()).saved['generated-movie']!==undefined")
        saved_position=page.evaluate("MovieBoxTV.state.history['generated-movie:0:0'].position")
        assert saved_position>0
        browser.close();adb('shell','am','force-stop',PACKAGE);adb('shell','am','start','-n',PACKAGE+'/.SharedTvActivity')
        for attempt in range(30):
            try:browser,page=attach(p);break
            except Exception:
                if attempt==29:raise
                time.sleep(.3)
        assert page.evaluate("!!MovieBoxTV.state.saved['generated-movie'] && MovieBoxTV.state.history['generated-movie:0:0'].position>0")
        checks['my_list_and_history_survive_force_stop']=True
        fixture(page,series=True)
        wait(page,'MovieBoxTV.player.position>500')
        assert page.evaluate('MovieBoxTV.player.options.episode===4')
        page.evaluate("MovieBoxTV.player.reveal();document.getElementById('play-pause').focus()")
        key(19)
        assert page.locator('#episode-drawer').is_hidden()
        page.evaluate("document.getElementById('player-page').classList.add('controls-hidden')")
        key(19);wait(page,"!document.getElementById('episode-drawer').hidden")
        checks['episode_drawer_only_opens_from_hidden_controls']=True
        key(4)
        page.evaluate('MovieBoxTV.player.seek(64000)')
        wait(page,'MovieBoxTV.player.options.episode===7 && MovieBoxTV.player.position>300',20000)
        checks['sparse_episode_auto_continuation_4_to_7']=True
        page.evaluate('MovieBoxTV.player.seek(64000)')
        wait(page,"!MovieBoxTV.player.active && !document.getElementById('detail-page').hidden",20000)
        checks['terminal_episode_returns_to_details']=True
        fixture(page,bad=True)
        wait(page,"MovieBoxTV.player.subject.subjectId==='generated-failure' && !!MovieBoxTV.player.lastNativeError",20000)
        key(4);wait(page,'!MovieBoxTV.player.active')
        checks['failed_source_does_not_trap_remote_navigation']=True
        for route in ['movies','tv','saved','home']:
            fixture(page)
            wait(page,'MovieBoxTV.player.position>300')
            page.evaluate('(route)=>MovieBoxTV.browse(route)',route)
            wait(page,"!MovieBoxTV.player.active && !document.getElementById('browse-page').hidden")
        checks['navigate_away_during_playback_without_freeze']=True
        for theme in ['obsidian','orbital','prism','aurora','eclipse']:
            page.evaluate('(theme)=>{MovieBoxThemes.apply(theme);MovieBoxTV.state.settings.theme=theme;MovieBoxAndroidFlush()}',theme)
            assert page.evaluate('document.documentElement.dataset.theme')==theme
        checks['all_five_shared_themes_apply']=True
        page.evaluate("MovieBoxTV.browse('search')")
        wait(page,"document.activeElement.id==='search-input'")
        key(23)
        for attempt in range(20):
            ime=adb('shell','dumpsys','input_method').decode(errors='replace')
            if 'mInputShown=true' in ime or 'isInputViewShown=true' in ime:break
            time.sleep(.2)
        else:raise AssertionError('Native TV input method did not open')
        checks['native_android_search_keyboard_opens']=True
        key(4)
        assert not errors,errors
        checks['uncaught_javascript_errors']=errors
        passed=True
    finally:
        apk=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
        (out/'shared-runtime.json').write_text(json.dumps({'passed':passed,'apkSha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'checks':checks,'errors':errors},indent=2),encoding='utf-8')
        print(json.dumps(checks,indent=2))
        browser.close()
