package org.emransm.moviebox.tv;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.media3.common.*;
import androidx.media3.datasource.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.*;
import org.json.*;
import java.io.File;

/** Native Media3 implementation behind the existing TV player contract. */
@androidx.media3.common.util.UnstableApi
final class AndroidAvPlayer {
    interface Events{void emit(JSONObject event);}
    private final Context context;private final PlayerView view;private final Events events;
    private final Handler main=new Handler(Looper.getMainLooper());
    private ExoPlayer player;private MediaSession session;
    private String url="",agent=Api.UA,state="NONE";
    private int epoch=0,preparingId=0,seekingId=0;private long seekTarget=0,seekStarted=0,suspendedPosition=0;
    private boolean suspended=false,closed=false;
    AndroidAvPlayer(Context context,PlayerView view,Events events){this.context=context;this.view=view;this.events=events;}
    JSONObject snapshot(){JSONObject result=new JSONObject();try{
        JSONArray tracks=new JSONArray(),selected=new JSONArray();
        if(player!=null)for(int groupIndex=0;groupIndex<player.getCurrentTracks().getGroups().size();groupIndex++){
            Tracks.Group group=player.getCurrentTracks().getGroups().get(groupIndex);if(group.getType()!=C.TRACK_TYPE_AUDIO)continue;
            for(int trackIndex=0;trackIndex<group.length;trackIndex++){if(!group.isTrackSupported(trackIndex))continue;Format format=group.getTrackFormat(trackIndex);
                JSONObject item=new JSONObject().put("type","AUDIO").put("index",groupIndex*1000+trackIndex).put("extra_info",new JSONObject().put("language",format.language==null?"":format.language).put("title",format.label==null?"Audio track "+(trackIndex+1):format.label).toString());tracks.put(item);if(group.isTrackSelected(trackIndex))selected.put(item);
            }
        }
        result.put("state",state).put("position",player==null?suspendedPosition:Math.max(0,player.getCurrentPosition())).put("duration",player==null?0:Math.max(0,player.getDuration())).put("tracks",tracks).put("selected",selected);
    }catch(JSONException ignored){}return result;}
    private void emit(String type,int id,String error){try{JSONObject event=new JSONObject().put("epoch",epoch).put("type",type).put("id",id).put("snapshot",snapshot());if(error!=null)event.put("error",error);events.emit(event);}catch(JSONException ignored){}}
    private final Runnable ticker=new Runnable(){public void run(){if(closed)return;if(player!=null){emit("progress",0,null);finishSeekIfReady();}main.postDelayed(this,250);}};
    void command(JSONObject command){if(closed)return;int commandEpoch=command.optInt("epoch"),id=command.optInt("id");String action=command.optString("action");JSONObject args=command.optJSONObject("args");if(args==null)args=new JSONObject();
        try{
            if(action.equals("open")){releaseCurrent();epoch=commandEpoch;url=args.optString("url");validateMediaUrl(url);state="IDLE";suspended=false;suspendedPosition=0;agent=Api.UA;emit("state",0,null);return;}
            if(commandEpoch!=epoch)return;
            switch(action){
                case "property":if(args.optString("key").equals("USER_AGENT"))agent=args.optString("value",Api.UA);break;
                case "prepare":preparingId=id;prepare(false);break;
                case "play":if(player!=null){player.play();state="PLAYING";emit("state",0,null);}break;
                case "pause":pause();break;
                case "close":releaseCurrent();url="";state="NONE";suspended=false;emit("state",0,null);break;
                case "seek":
                    if(player==null||player.getDuration()<=0){emit("failure",id,"NOT_READY");break;}
                    long target=Policies.seekPosition(args.optLong("position"),player.getDuration());if(target<0){emit("failure",id,"INVALID_SEEK");break;}
                    seekingId=id;seekTarget=target;seekStarted=android.os.SystemClock.elapsedRealtime();player.seekTo(target);finishSeekIfReady();break;
                case "display":String mode=args.optString("mode");view.setResizeMode(mode.equals("PLAYER_DISPLAY_MODE_FULL_SCREEN")?AspectRatioFrameLayout.RESIZE_MODE_FILL:mode.equals("PLAYER_DISPLAY_MODE_AUTO_ASPECT_RATIO")?AspectRatioFrameLayout.RESIZE_MODE_ZOOM:AspectRatioFrameLayout.RESIZE_MODE_FIT);break;
                case "audio":if(player!=null){int encoded=args.optInt("index"),groupIndex=encoded/1000,trackIndex=encoded%1000;if(groupIndex<player.getCurrentTracks().getGroups().size()){Tracks.Group group=player.getCurrentTracks().getGroups().get(groupIndex);if(group.getType()==C.TRACK_TYPE_AUDIO&&trackIndex<group.length&&group.isTrackSupported(trackIndex))player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(),trackIndex)).build());}}break;
            }
        }catch(Exception error){emit("failure",id,"ANDROID_PLAYER_ERROR");}
    }
    private void validateMediaUrl(String value)throws Exception {
        Uri uri=Uri.parse(value);if("https".equals(uri.getScheme())&&uri.getHost()!=null&&!uri.getHost().equals("localhost")&&!uri.getHost().equals("127.0.0.1")&&uri.getUserInfo()==null)return;
        if("file".equals(uri.getScheme())&&new File(uri.getPath()).getCanonicalPath().startsWith(context.getFilesDir().getCanonicalPath()+File.separator))return;
        throw new IllegalArgumentException("Unsupported media source");
    }
    private void prepare(boolean restoring){
        int pendingPrepare=preparingId;if(player!=null)releaseCurrent();preparingId=pendingPrepare;final int activeEpoch=epoch;
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent(agent).setConnectTimeoutMs(8000).setReadTimeoutMs(10000).setAllowCrossProtocolRedirects(false);
        player=new ExoPlayer.Builder(context).setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(context,http))).setReleaseTimeoutMs(1000).setLoadControl(new DefaultLoadControl.Builder().setBufferDurationsMs(8000,30000,800,1500).build()).build();
        ExoPlayer active=player;view.setPlayer(player);view.setVisibility(View.VISIBLE);session=new MediaSession.Builder(context,player).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int value){if(player!=active||activeEpoch!=epoch)return;
                if(value==Player.STATE_BUFFERING)emit("buffering",0,null);
                if(value==Player.STATE_READY){state=player.getPlayWhenReady()?"PLAYING":restoring?"PAUSED":"READY";emit("ready",0,null);if(preparingId>0){int request=preparingId;preparingId=0;emit("prepared",request,null);}finishSeekIfReady();}
                if(value==Player.STATE_ENDED){state="READY";emit("ended",0,null);}
            }
            @Override public void onIsPlayingChanged(boolean playing){if(player!=active||activeEpoch!=epoch)return;if(player.getPlaybackState()==Player.STATE_READY){state=player.getPlayWhenReady()?"PLAYING":"PAUSED";emit("state",0,null);}}
            @Override public void onTracksChanged(Tracks tracks){if(player==active&&activeEpoch==epoch)emit("progress",0,null);}
            @Override public void onPlayerError(PlaybackException error){if(player!=active||activeEpoch!=epoch)return;state="PAUSED";String name=error.errorCode>=PlaybackException.ERROR_CODE_IO_UNSPECIFIED&&error.errorCode<PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED?"PLAYER_ERROR_CONNECTION_FAILED":"ANDROID_DECODER_ERROR";
                if(preparingId>0){int request=preparingId;preparingId=0;emit("failure",request,name);}else emit("error",0,name);
                if(seekingId>0){int request=seekingId;seekingId=0;emit("failure",request,name);}
            }
        });
        player.setMediaItem(MediaItem.fromUri(url),restoring?suspendedPosition:0);player.prepare();player.setPlayWhenReady(false);main.removeCallbacks(ticker);main.post(ticker);
    }
    private void finishSeekIfReady(){if(seekingId==0||player==null)return;boolean ready=player.getPlaybackState()==Player.STATE_READY&&Math.abs(player.getCurrentPosition()-seekTarget)<1500;
        if(ready){int request=seekingId;seekingId=0;emit("seeked",request,null);}else if(android.os.SystemClock.elapsedRealtime()-seekStarted>8000){int request=seekingId;seekingId=0;emit("failure",request,"SEEK_TIMEOUT");}}
    void pause(){if(player!=null){player.pause();state="PAUSED";emit("state",0,null);}}
    void suspend(){if(player!=null&&!url.isEmpty()){pause();suspendedPosition=player.getCurrentPosition();suspended=true;releaseCurrent();state="PAUSED";}}
    void resume(){if(suspended&&!url.isEmpty()&&!closed){suspended=false;preparingId=0;prepare(true);}}
    private void releaseCurrent(){main.removeCallbacks(ticker);preparingId=0;seekingId=0;if(session!=null){session.release();session=null;}if(player!=null){view.setPlayer(null);player.release();player=null;}view.setVisibility(View.GONE);}
    void close(){closed=true;releaseCurrent();url="";state="NONE";}
}
