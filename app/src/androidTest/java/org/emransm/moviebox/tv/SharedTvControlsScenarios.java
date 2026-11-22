package org.emransm.moviebox.tv;

import android.view.KeyEvent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Launcher controls exercised with actual Android remote events, without service/media dependencies. */
@RunWith(AndroidJUnit4.class)
public class SharedTvControlsScenarios {
    @Test public void remoteVolumeRangeChangesAndClampsWithoutDoubleSeek() throws Exception {
        try (ActivityScenario<SharedTvActivity> scenario=ActivityScenario.launch(SharedTvActivity.class)) {
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"!!window.MovieBoxTV && !!window.MovieBoxInlineControls"),30000);
            SharedTvReleaseScenarios.js(scenario,"""
                (()=>{
                  const slider=document.getElementById('volume-slider');
                  window.controlEvents={inputs:0,changes:0};
                  slider.addEventListener('input',()=>controlEvents.inputs++);
                  slider.addEventListener('change',()=>controlEvents.changes++);
                  document.getElementById('player-page').hidden=false;
                  MovieBoxInlineControls.openVolume();slider.value='50';slider.focus();
                  // Isolate range key behavior from device volume quantization.
                  slider.oninput=null;return true;
                })()
                """);
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_RIGHT);
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"Number(document.getElementById('volume-slider').value)===50+MovieBoxAndroidEnvironment.volumeStep && controlEvents.inputs===1 && controlEvents.changes===1"),3000);
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_LEFT);
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"document.getElementById('volume-slider').value==='50' && controlEvents.inputs===2"),3000);
            SharedTvReleaseScenarios.js(scenario,"document.getElementById('volume-slider').value='100'");
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_RIGHT);
            assertTrue(SharedTvReleaseScenarios.yes(scenario,"document.getElementById('volume-slider').value==='100'"));
            SharedTvReleaseScenarios.js(scenario,"document.getElementById('volume-slider').value='0'");
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_LEFT);
            assertTrue(SharedTvReleaseScenarios.yes(scenario,"document.getElementById('volume-slider').value==='0'"));
            SharedTvReleaseScenarios.js(scenario,"document.getElementById('volume-slider').addEventListener('keydown',event=>{if(event.key==='ArrowRight'){event.preventDefault();event.target.value='42';}},{once:true})");
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_RIGHT);
            assertTrue(SharedTvReleaseScenarios.yes(scenario,"document.getElementById('volume-slider').value==='42'"));
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_BACK);
            assertTrue(SharedTvReleaseScenarios.yes(scenario,"document.getElementById('volume-popover').hidden && document.activeElement.id==='mute-button'"));
        }
    }
    @Test public void remoteVolumeMovesActualDiscreteAndroidStream() throws Exception {
        try (ActivityScenario<SharedTvActivity> scenario=ActivityScenario.launch(SharedTvActivity.class)) {
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"!!window.MovieBoxInlineControls"),30000);
            SharedTvReleaseScenarios.js(scenario,"MovieBoxAndroidAudio.setVolume(40)");
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"MovieBoxAndroidAudio.getVolume()>=35 && MovieBoxAndroidAudio.getVolume()<=45"),3000);
            SharedTvReleaseScenarios.js(scenario,"document.getElementById('player-page').hidden=false;MovieBoxInlineControls.openVolume();window.beforeVolume=MovieBoxAndroidAudio.getVolume()");
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_RIGHT);
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"MovieBoxAndroidAudio.getVolume()>beforeVolume"),3000);
            SharedTvReleaseScenarios.js(scenario,"MovieBoxInlineControls.syncVolume();document.getElementById('volume-slider').focus()");
            SharedTvReleaseScenarios.key(KeyEvent.KEYCODE_DPAD_LEFT);
            SharedTvReleaseScenarios.until(()->SharedTvReleaseScenarios.yes(scenario,"MovieBoxAndroidAudio.getVolume()===beforeVolume"),3000);
        }
    }
}
