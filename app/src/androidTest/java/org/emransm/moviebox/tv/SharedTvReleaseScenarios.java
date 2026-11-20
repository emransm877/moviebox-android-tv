package org.emransm.moviebox.tv;

import android.content.Context;
import android.view.KeyEvent;
import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Exercises the signed non-debuggable launcher without WebView debugging. */
@RunWith(AndroidJUnit4.class)
public class SharedTvReleaseScenarios {
    interface Check { boolean ready() throws Exception; }
    static void until(Check check,long timeout)throws Exception{long end=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<end){if(check.ready())return;Thread.sleep(100);}fail("Runtime condition timed out");}
    static String js(ActivityScenario<SharedTvActivity> scenario,String code)throws Exception{
        CountDownLatch latch=new CountDownLatch(1);AtomicReference<String> value=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        scenario.onActivity(activity->{try{Field field=SharedTvActivity.class.getDeclaredField("web");field.setAccessible(true);((WebView)field.get(activity)).evaluateJavascript(code,result->{value.set(result);latch.countDown();});}catch(Throwable error){failure.set(error);latch.countDown();}});
        assertTrue("JavaScript callback must remain responsive",latch.await(8,TimeUnit.SECONDS));if(failure.get()!=null)throw new AssertionError(failure.get());return value.get();
    }
    static boolean yes(ActivityScenario<SharedTvActivity> scenario,String code)throws Exception{return "true".equals(js(scenario,code));}
    static void key(int code){InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code);}
    static File fixture()throws Exception{Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File file=new File(context.getFilesDir(),"generated-fixture.mp4");try(InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("fixture.mp4");OutputStream output=new FileOutputStream(file)){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);}return file;}
    static void open(ActivityScenario<SharedTvActivity> scenario,boolean series,boolean bad)throws Exception{
        js(scenario,"""
          (()=>{const app=MovieBoxTV,series=SERIES,bad=BAD,subject={subjectId:bad?'release-failure':series?'release-series':'release-movie',subjectType:series?2:1,title:'Generated release acceptance',description:'Synthetic local acceptance media',hasResource:true};
          delete app.state.saved[subject.subjectId];Object.keys(app.state.history).filter(key=>key.startsWith(subject.subjectId+':')).forEach(key=>delete app.state.history[key]);
          app.api.detail=async()=>({subject,isSeries:series,resource:{seasons:series?[{se:5,episodeNumbers:[4,7]}]:[]}});
          app.api.playback=async()=>[{url:bad?'https://invalid.invalid/unavailable.mp4':'file:///data/user/0/org.emransm.moviebox.tv/files/generated-fixture.mp4',resolutions:1080,duration:65,size:12400000,id:'release-fixture'}];
          app.api.captions=async()=>({captions:[{id:'release-en',lan:'en',lanName:'English',url:'https://fixture.invalid/release.srt'}]});
          app.api.text=async()=> ['1','00:00:00,000 --> 00:01:04,000','Generated release subtitle',''].join(String.fromCharCode(10));
          app.openDetail(subject);return true;})()
          """.replace("SERIES",String.valueOf(series)).replace("BAD",String.valueOf(bad)));
        until(()->yes(scenario,"!document.getElementById('watch-button').disabled"),10000);
        if(series)js(scenario,"document.querySelector('#episodes [data-episode=\"4\"]').click()");
        js(scenario,"MovieBoxTV.watch()");
    }
    @Test public void signedPlaybackRemoteSeekingSubtitlesAudioAndSeries()throws Exception{
        fixture();
        try(ActivityScenario<SharedTvActivity> scenario=ActivityScenario.launch(SharedTvActivity.class)){
            until(()->yes(scenario,"!!window.MovieBoxTV && document.querySelectorAll('.poster-card').length>0"),30000);
            assertTrue(yes(scenario,"Math.abs(visualViewport.scale*devicePixelRatio-1)<.1 && document.documentElement.scrollWidth<=innerWidth"));
            assertFalse((InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0);
            open(scenario,false,false);until(()->yes(scenario,"MovieBoxTV.player.native && MovieBoxTV.player.position>1000 && !MovieBoxTV.player.preparing"),15000);
            until(()->yes(scenario,"document.getElementById('subtitle-overlay').textContent.includes('Generated release subtitle')"),10000);
            js(scenario,"document.getElementById('play-pause').focus();MovieBoxTV.player.reveal()");
            key(KeyEvent.KEYCODE_DPAD_CENTER);until(()->yes(scenario,"MovieBoxTV.player.paused"),2000);
            key(KeyEvent.KEYCODE_DPAD_CENTER);until(()->yes(scenario,"!MovieBoxTV.player.paused"),2000);
            js(scenario,"MovieBoxTV.player.seek(30000)");until(()->yes(scenario,"MovieBoxTV.player.position>=29000 && !MovieBoxTV.player.seekInFlight"),10000);
            js(scenario,"MovieBoxTV.player.seek(10000)");until(()->yes(scenario,"MovieBoxTV.player.position>=9000 && MovieBoxTV.player.position<15000 && !MovieBoxTV.player.seekInFlight"),10000);
            js(scenario,"for(let i=0;i<12;i++)MovieBoxTV.player.skip(1000)");until(()->yes(scenario,"!MovieBoxTV.player.seekInFlight && MovieBoxTV.player.seekTarget===null"),10000);
            js(scenario,"MovieBoxTV.player.selectEmbeddedTrack(MovieBoxTV.player.audioTracks[0].index)");assertTrue(yes(scenario,"MovieBoxTV.player.audioSelection.startsWith('track:')"));
            key(KeyEvent.KEYCODE_BACK);until(()->yes(scenario,"!MovieBoxTV.player.active"),3000);
            js(scenario,"document.getElementById('save-button').click();MovieBoxTV.state.settings.theme='eclipse';MovieBoxThemes.apply('eclipse');MovieBoxAndroidFlush()");
            File saved=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"shared-tv-state.json");
            until(()->saved.isFile()&&new String(java.nio.file.Files.readAllBytes(saved.toPath()),StandardCharsets.UTF_8).contains("release-movie"),5000);
            open(scenario,true,false);until(()->yes(scenario,"MovieBoxTV.player.position>300 && MovieBoxTV.player.options.episode===4"),15000);
            js(scenario,"MovieBoxTV.player.reveal();document.getElementById('play-pause').focus()");key(KeyEvent.KEYCODE_DPAD_UP);assertTrue(yes(scenario,"document.getElementById('episode-drawer').hidden"));
            js(scenario,"document.getElementById('player-page').classList.add('controls-hidden')");key(KeyEvent.KEYCODE_DPAD_UP);until(()->yes(scenario,"!document.getElementById('episode-drawer').hidden"),3000);key(KeyEvent.KEYCODE_BACK);
            js(scenario,"MovieBoxTV.player.seek(64000)");until(()->yes(scenario,"MovieBoxTV.player.options.episode===7 && MovieBoxTV.player.position>300"),15000);
            js(scenario,"MovieBoxTV.player.seek(64000)");until(()->yes(scenario,"!MovieBoxTV.player.active && !document.getElementById('detail-page').hidden"),15000);
            open(scenario,false,true);until(()->yes(scenario,"!!MovieBoxTV.player.lastNativeError && MovieBoxTV.player.subject.subjectId==='release-failure'"),20000);key(KeyEvent.KEYCODE_BACK);until(()->yes(scenario,"!MovieBoxTV.player.active"),3000);
            for(String route:new String[]{"movies","tv","saved","home"}){open(scenario,false,false);until(()->yes(scenario,"MovieBoxTV.player.position>300"),15000);js(scenario,"MovieBoxTV.browse('"+route+"')");until(()->yes(scenario,"!MovieBoxTV.player.active && !document.getElementById('browse-page').hidden"),3000);}
            js(scenario,"MovieBoxTV.state.saved['release-persistent']={subjectId:'release-persistent',title:'Release persistence check',subjectType:1};MovieBoxAndroidFlush()");
            until(()->new String(java.nio.file.Files.readAllBytes(saved.toPath()),StandardCharsets.UTF_8).contains("release-persistent"),5000);
        }
    }
    @Test public void coldProcessRestoresLibraryAndTheme()throws Exception{
        try(ActivityScenario<SharedTvActivity> scenario=ActivityScenario.launch(SharedTvActivity.class)){
            until(()->yes(scenario,"!!window.MovieBoxTV"),15000);
            assertTrue(yes(scenario,"!!MovieBoxTV.state.saved['release-persistent'] && MovieBoxTV.state.settings.theme==='eclipse'"));
            assertTrue(yes(scenario,"MovieBoxTV.state.history['release-movie:0:0'].position>0"));
        }
    }
}
