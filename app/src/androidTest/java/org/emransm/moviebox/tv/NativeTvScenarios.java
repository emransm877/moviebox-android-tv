package org.emransm.moviebox.tv;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.exoplayer.ExoPlayer;
import android.content.Context;
import android.net.Uri;
import android.view.KeyEvent;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real decoded video and physical key events; all fixture media is generated. */
@RunWith(AndroidJUnit4.class)
public class NativeTvScenarios {
    interface Check {boolean ready()throws Exception;}
    static Object field(Object target,String name)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
    static void put(Object target,String name,Object value)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    static Object call(Object target,String name,Class<?>[] types,Object...values)throws Exception{Method method=target.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,values);}
    static void waitFor(Check check,long timeout)throws Exception{long until=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<until){if(check.ready())return;Thread.sleep(100);}fail("Scenario condition did not complete in "+timeout+" ms");}
    interface Action {void run(TvActivity activity)throws Exception;}
    static void main(ActivityScenario<TvActivity> scenario,Action action){AtomicReference<Throwable> failure=new AtomicReference<>();scenario.onActivity(activity->{try{action.run(activity);}catch(Throwable error){failure.set(error);}});if(failure.get()!=null)throw new AssertionError(failure.get());}
    static long position(ActivityScenario<TvActivity> scenario){long[] value={-1};main(scenario,activity->{ExoPlayer player=(ExoPlayer)field(activity,"player");if(player!=null)value[0]=player.getCurrentPosition();});return value[0];}
    static boolean playing(ActivityScenario<TvActivity> scenario){boolean[] value={false};main(scenario,activity->{ExoPlayer player=(ExoPlayer)field(activity,"player");value[0]=player!=null&&player.isPlaying();});return value[0];}
    static File fixture()throws Exception{Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();File file=new File(target.getFilesDir(),"generated-fixture.mp4");if(!file.exists())try(InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("fixture.mp4");OutputStream output=new FileOutputStream(file)){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);}return file;}
    static JSONObject movie()throws Exception{return new JSONObject().put("subjectId","generated-native-tv-fixture").put("subjectType",1).put("title","Generated TV acceptance fixture").put("description","Locally generated test video, not service content.");}
    static void start(ActivityScenario<TvActivity> scenario,String url)throws Exception{main(scenario,activity->{call(activity,"releasePlayer",new Class[]{});put(activity,"generation",(Integer)field(activity,"generation")+1);JSONObject subject=movie();put(activity,"subject",subject);put(activity,"currentAudio",subject);put(activity,"season",0);put(activity,"episode",0);((TreeMap<?,?>)field(activity,"seasons")).clear();List<JSONObject> streams=new ArrayList<>();streams.add(new JSONObject().put("resourceLink",url).put("resolution",1080).put("duration",65).put("resourceId","generated-fixture"));put(activity,"streams",streams);put(activity,"streamIndex",0);call(activity,"startStream",new Class[]{long.class,boolean.class,File.class},0L,true,null);});}
    @Test public void decodedPlaybackRemoteToggleSeekResumeAndTerminalReturn()throws Exception{
        File file=fixture();
        try(ActivityScenario<TvActivity> scenario=ActivityScenario.launch(TvActivity.class)){
            start(scenario,Uri.fromFile(file).toString());waitFor(()->playing(scenario),15000);waitFor(()->position(scenario)>1200,10000);
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);waitFor(()->!playing(scenario),1000);
            long paused=position(scenario);Thread.sleep(400);assertTrue(Math.abs(position(scenario)-paused)<100);
            // The second OK must resume immediately, while controls are still visible.
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);waitFor(()->playing(scenario),1000);
            main(scenario,activity->((Runnable)field(activity,"hideControls")).run());
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            waitFor(()->position(scenario)>paused+17000,5000);
            main(scenario,activity->call(activity,"saveProgress",new Class[]{boolean.class},false));Thread.sleep(700);
            Library disk=new Library(InstrumentationRegistry.getInstrumentation().getTargetContext());assertTrue(disk.position(movie(),0,0)>15000);
            main(scenario,activity->{ExoPlayer player=(ExoPlayer)field(activity,"player");player.seekTo(player.getDuration()-400);player.play();});
            waitFor(()->{boolean[] ended={false};main(scenario,activity->ended[0]=field(activity,"player")==null);return ended[0];},10000);
            // A completed movie returns to Details and remains completed on disk.
            Thread.sleep(700);Library completed=new Library(InstrumentationRegistry.getInstrumentation().getTargetContext());assertEquals(0,completed.position(movie(),0,0));
        }
    }
    @Test public void failedSourceStillAllowsRemoteExit()throws Exception{
        try(ActivityScenario<TvActivity> scenario=ActivityScenario.launch(TvActivity.class)){
            start(scenario,"https://127.0.0.1:9/unavailable.mp4");Thread.sleep(1500);
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            waitFor(()->{boolean[] exited={false};main(scenario,activity->exited[0]=field(activity,"player")==null);return exited[0];},2000);
        }
    }
    @Test public void allFiveThemesAndSavedListSurviveRecreation()throws Exception{
        try(ActivityScenario<TvActivity> scenario=ActivityScenario.launch(TvActivity.class)){
            for(String key:new String[]{"obsidian","orbital","prism","aurora","eclipse"}){
                main(scenario,activity->{Library library=(Library)field(activity,"library");library.settings().put("theme",key);library.save();call(activity,"applyTheme",new Class[]{});call(activity,"browse",new Class[]{String.class},"Settings");assertEquals(key,field(activity,"themeKey"));});
            }
            main(scenario,activity->{Library library=(Library)field(activity,"library");if(!library.contains(movie()))library.toggle(movie());});Thread.sleep(700);
            scenario.recreate();main(scenario,activity->{Library library=(Library)field(activity,"library");assertTrue(library.contains(movie()));assertEquals("eclipse",field(activity,"themeKey"));});
        }
    }
    @Test public void subtitleTrackAndHiddenControlsEpisodeShortcut()throws Exception{
        File file=fixture();Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File subtitle=new File(context.getFilesDir(),"acceptance.srt");
        try(OutputStream output=new FileOutputStream(subtitle)){output.write("1\n00:00:00,000 --> 00:01:05,000\nGenerated subtitle acceptance fixture\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        try(ActivityScenario<TvActivity> scenario=ActivityScenario.launch(TvActivity.class)){
            start(scenario,Uri.fromFile(file).toString());waitFor(()->playing(scenario),15000);
            main(scenario,activity->{JSONObject series=movie().put("subjectType",2);put(activity,"subject",series);put(activity,"currentAudio",series);put(activity,"season",5);put(activity,"episode",4);TreeMap<Integer,int[]> seasons=(TreeMap<Integer,int[]>)field(activity,"seasons");seasons.put(5,new int[]{4,7});call(activity,"startStream",new Class[]{long.class,boolean.class,File.class},0L,true,subtitle);});
            waitFor(()->playing(scenario),10000);
            waitFor(()->{boolean[] selected={false};main(scenario,activity->selected[0]=((ExoPlayer)field(activity,"player")).getCurrentTracks().isTypeSelected(androidx.media3.common.C.TRACK_TYPE_TEXT));return selected[0];},7000);
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
            main(scenario,activity->assertFalse((Boolean)field(activity,"drawerOpen")));
            main(scenario,activity->((Runnable)field(activity,"hideControls")).run());
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
            main(scenario,activity->{assertTrue((Boolean)field(activity,"drawerOpen"));assertArrayEquals(new int[]{5,7},(int[])call(activity,"nextEpisode",new Class[]{}));});
        }
    }
}
