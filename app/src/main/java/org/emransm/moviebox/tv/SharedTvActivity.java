package org.emransm.moviebox.tv;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.util.AtomicFile;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.FrameLayout;
import androidx.webkit.WebViewAssetLoader;
import androidx.media3.ui.PlayerView;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Exact shared TV screens with Android-native Media3, input and storage. */
@androidx.media3.common.util.UnstableApi
public class SharedTvActivity extends Activity {
    static final String START="https://appassets.androidplatform.net/assets/tv-ui/index.html";
    private WebView web;private PlayerView video;private AndroidAvPlayer media;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService requests=new ThreadPoolExecutor(4,4,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(32),new ThreadPoolExecutor.AbortPolicy()),writer=Executors.newSingleThreadExecutor();
    private volatile boolean textEditing=false,destroyed=false;private volatile String cachedState="";private String initialTheme="obsidian";
    private AtomicFile stateFile;private AudioManager audio;private int lastVolume=-1;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);immersive();
        stateFile=new AtomicFile(new File(getFilesDir(),"shared-tv-state.json"));try{cachedState=new String(stateFile.readFully(),StandardCharsets.UTF_8);JSONObject state=new JSONObject(cachedState);initialTheme=state.optJSONObject("settings")==null?"obsidian":state.getJSONObject("settings").optString("theme","obsidian");}catch(Exception ignored){
            try{Library old=new Library(this);initialTheme=old.settings().optString("theme","obsidian");cachedState=new JSONObject().put("saved",old.data.getJSONObject("list")).put("history",old.data.getJSONObject("history")).put("settings",old.settings()).toString();old.close();}catch(Exception none){}
        }
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        video=new PlayerView(this);video.setUseController(false);video.setKeepScreenOn(true);video.setVisibility(View.GONE);video.setFocusable(false);root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        web=new WebView(this);web.setBackgroundColor(Color.TRANSPARENT);web.setFocusable(true);web.setFocusableInTouchMode(true);root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setDatabaseEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);settings.setTextZoom(100);web.setInitialScale(100);
        web.setInitialScale(0);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0);
        WebViewAssetLoader assets=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return assets.shouldInterceptRequest(request.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){Uri uri=request.getUrl();if(uri.toString().startsWith("https://appassets.androidplatform.net/assets/tv-ui/"))return false;
                if(request.isForMainFrame()&&"https".equals(uri.getScheme())&&"github.com".equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/emransm877/"))try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException ignored){}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        media=new AndroidAvPlayer(this,video,event->evaluate("window.MovieBoxAndroidEvent&&window.MovieBoxAndroidEvent("+event+");"));
        web.addJavascriptInterface(new Bridge(),"MovieBoxAndroidBridge");web.loadUrl(START);web.requestFocus();main.post(volumePoll);
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    private void evaluate(String script){main.post(()->{if(!destroyed&&web!=null)web.evaluateJavascript(script,null);});}
    private final Runnable volumePoll=new Runnable(){public void run(){if(destroyed)return;int level=volume();if(level!=lastVolume){lastVolume=level;evaluate("window.MovieBoxAndroidVolumeChanged&&window.MovieBoxAndroidVolumeChanged();");}main.postDelayed(this,1000);}};
    private int volume(){if(audio==null)return 100;int maximum=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);return maximum>0?audio.getStreamVolume(AudioManager.STREAM_MUSIC)*100/maximum:100;}
    private void resolve(int id,JSONObject response,String error){evaluate("window.MovieBoxAndroidResolve&&window.MovieBoxAndroidResolve("+id+","+(response==null?"null":response.toString())+","+(error==null?"null":JSONObject.quote(error))+");");}
    private final class Bridge {
        @JavascriptInterface public String environment(){try{int maximum=audio==null?100:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);return new JSONObject().put("brand",Build.BRAND).put("model",Build.MODEL).put("osVersion",Build.VERSION.RELEASE).put("userAgent",Api.UA).put("theme",initialTheme).put("volumeStep",maximum>0?Math.ceil(100.0/maximum):1).toString();}catch(Exception ignored){return "{}";}}
        @JavascriptInterface public String cachedState(){return cachedState;}
        @JavascriptInterface public void saveState(String state){if(destroyed||state==null||state.length()>4*1024*1024)return;try{JSONObject parsed=new JSONObject(state);if(parsed.optJSONObject("saved")==null||parsed.optJSONObject("history")==null||parsed.optJSONObject("settings")==null)return;}catch(Exception ignored){return;}
            cachedState=state;byte[] bytes=state.getBytes(StandardCharsets.UTF_8);try{writer.execute(()->{FileOutputStream output=null;try{output=stateFile.startWrite();output.write(bytes);stateFile.finishWrite(output);}catch(Exception error){if(output!=null)stateFile.failWrite(output);}});}catch(RejectedExecutionException ignored){}
        }
        @JavascriptInterface public void request(int id,String request){if(destroyed)return;if(request==null||request.length()>131072){resolve(id,null,"Request exceeds limit");return;}try{requests.execute(()->{try{resolve(id,http(new JSONObject(request)),null);}catch(Exception error){resolve(id,null,"Service request unavailable. Check your connection and try again.");}});}catch(RejectedExecutionException error){resolve(id,null,"Requests are busy. Please try again.");}}
        @JavascriptInterface public void command(String text){if(destroyed||text==null||text.length()>16384)return;try{JSONObject command=new JSONObject(text);main.post(()->{if(destroyed)return;String action=command.optString("action");JSONObject args=command.optJSONObject("args");if(action.equals("volume")&&audio!=null&&args!=null){int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(max,Math.round(max*args.optInt("value")/100f))),0);}else if(action.equals("mute")&&audio!=null&&args!=null)audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,args.optBoolean("value")?AudioManager.ADJUST_MUTE:AudioManager.ADJUST_UNMUTE,0);else media.command(command);});}catch(Exception ignored){}}
        @JavascriptInterface public void textFocus(boolean value){textEditing=value;}
        @JavascriptInterface public int volume(){return SharedTvActivity.this.volume();}
        @JavascriptInterface public boolean muted(){return audio!=null&&audio.isStreamMute(AudioManager.STREAM_MUSIC);}
        @JavascriptInterface public void videoVisible(boolean value){main.post(()->{if(!destroyed)video.setVisibility(value?View.VISIBLE:View.GONE);});}
        @JavascriptInterface public void exit(){main.post(()->finishAndRemoveTask());}
    }
    private JSONObject http(JSONObject request)throws Exception {
        String mode=request.optString("mode"),location=request.getString("url");URL url=new URL(location);String host=url.getHost().toLowerCase(Locale.ROOT);
        if(!url.getProtocol().equals("https")||url.getUserInfo()!=null||(url.getPort()!=-1&&url.getPort()!=443))throw new IOException("Unsupported service URL");
        if(mode.equals("api")){if(!((host.equals("api6.aoneroom.com")&&url.getPath().startsWith("/wefeed-mobile-bff/"))||(host.equals("h5-api.aoneroom.com")&&url.getPath().startsWith("/wefeed-h5api-bff/"))))throw new IOException("Unsupported API host");}
        else if(mode.equals("subtitle")){if(!(host.equals("aoneroom.com")||host.endsWith(".aoneroom.com")||host.equals("hakunaymatata.com")||host.endsWith(".hakunaymatata.com")))throw new IOException("Unsupported subtitle host");}
        else throw new IOException("Unsupported request mode");
        HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent",Api.UA);
        JSONObject headers=request.optJSONObject("headers");if(mode.equals("api")&&headers!=null){Set<String> allowed=new HashSet<>(Arrays.asList("accept","content-type","authorization","x-client-token","x-client-info","x-client-status","x-request-lang","x-vip-restrict","x-tr-signature","x-child-uid","x-play-mode","x-idle-data","x-family-mode","x-content-mode"));Iterator<String> keys=headers.keys();while(keys.hasNext()){String key=keys.next();if(allowed.contains(key.toLowerCase(Locale.ROOT)))connection.setRequestProperty(key,headers.optString(key));}}
        try{
            if(!request.isNull("body")&&request.has("body")){byte[] bytes=request.getString("body").getBytes(StandardCharsets.UTF_8);connection.setRequestMethod("POST");connection.setDoOutput(true);try(OutputStream output=connection.getOutputStream()){output.write(bytes);}}
            int status=connection.getResponseCode();InputStream input=status>=200&&status<300?connection.getInputStream():connection.getErrorStream();String body=input==null?"":new String(Api.read(input,4*1024*1024),StandardCharsets.UTF_8);
            return new JSONObject().put("status",status).put("body",body).put("user",connection.getHeaderField("x-user"));
        }finally{connection.disconnect();}
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){String key=null;switch(event.getKeyCode()){
        case KeyEvent.KEYCODE_DPAD_UP:key="ArrowUp";break;case KeyEvent.KEYCODE_DPAD_DOWN:key="ArrowDown";break;case KeyEvent.KEYCODE_DPAD_LEFT:key="ArrowLeft";break;case KeyEvent.KEYCODE_DPAD_RIGHT:key="ArrowRight";break;
        case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:key="Enter";break;case KeyEvent.KEYCODE_BACK:key="Back";break;case KeyEvent.KEYCODE_ESCAPE:key="Escape";break;
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:key="MediaPlayPause";break;case KeyEvent.KEYCODE_MEDIA_PLAY:key="MediaPlay";break;case KeyEvent.KEYCODE_MEDIA_PAUSE:key="MediaPause";break;case KeyEvent.KEYCODE_MEDIA_STOP:key="MediaStop";break;case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:key="MediaFastForward";break;case KeyEvent.KEYCODE_MEDIA_REWIND:key="MediaRewind";break;
    }
        if(web!=null&&key!=null){boolean down=event.getAction()==KeyEvent.ACTION_DOWN;if(textEditing&&event.getKeyCode()==KeyEvent.KEYCODE_DPAD_CENTER){if(down)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(web,InputMethodManager.SHOW_IMPLICIT);return true;}if(textEditing&&!key.equals("Back")&&!key.equals("Escape"))return super.dispatchKeyEvent(event);
            evaluate("window.MovieBoxAndroidKey&&window.MovieBoxAndroidKey("+JSONObject.quote(key)+","+down+","+(event.getRepeatCount()>0)+");");return true;
        }return super.dispatchKeyEvent(event);
    }
    @Override protected void onPause(){if(web!=null){web.evaluateJavascript("window.MovieBoxAndroidFlush&&window.MovieBoxAndroidFlush();",null);web.onPause();}if(media!=null)media.pause();super.onPause();}
    @Override protected void onStop(){if(media!=null)media.suspend();super.onStop();}
    @Override protected void onResume(){super.onResume();immersive();if(web!=null)web.onResume();if(media!=null)media.resume();}
    @Override protected void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);if(media!=null)media.close();if(web!=null){web.removeJavascriptInterface("MovieBoxAndroidBridge");web.destroy();web=null;}requests.shutdownNow();writer.shutdown();super.onDestroy();}
}
