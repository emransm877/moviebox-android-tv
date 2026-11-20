package org.emransm.moviebox.tv;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Atomic app-private library, separate from APK files and service sessions. */
public final class Library {
    public final JSONObject data;
    private final AtomicFile file;
    private final ExecutorService writer=Executors.newSingleThreadExecutor();
    public Library(Context context) throws JSONException {
        file=new AtomicFile(new File(context.getFilesDir(),"library.json"));JSONObject loaded;
        try{loaded=new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8));}catch(Exception ignored){loaded=new JSONObject();}
        data=loaded;
        for(String key:new String[]{"list","history","settings"})if(data.optJSONObject(key)==null)data.put(key,new JSONObject());
        if(data.optString("identity").isEmpty())data.put("identity",UUID.randomUUID().toString().replace("-",""));
    }
    public JSONObject settings(){return data.optJSONObject("settings");}
    public void save(){final byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);writer.execute(()->{FileOutputStream stream=null;try{stream=file.startWrite();stream.write(bytes);file.finishWrite(stream);}catch(IOException error){if(stream!=null)file.failWrite(stream);}});}
    public void close(){writer.shutdown();}
    static JSONObject summary(JSONObject subject) throws JSONException {JSONObject out=new JSONObject();for(String key:new String[]{"subjectId","subjectType","title","cover","releaseDate","imdbRatingValue","countryName","genre"})if(subject.has(key))out.put(key,subject.get(key));return out;}
    public boolean contains(JSONObject subject){return data.optJSONObject("list").has(subject.optString("subjectId"));}
    public boolean toggle(JSONObject subject) throws JSONException {JSONObject list=data.getJSONObject("list");String id=subject.getString("subjectId");if(list.has(id))list.remove(id);else list.put(id,summary(subject));save();return list.has(id);}
    public List<JSONObject> listing(String name) throws JSONException {
        JSONObject values=data.getJSONObject(name);List<JSONObject> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        List<JSONObject> records=new ArrayList<>();Iterator<String> keys=values.keys();while(keys.hasNext()){JSONObject entry=values.optJSONObject(keys.next());if(entry!=null)records.add(entry);}
        Collections.sort(records,(a,b)->Long.compare(b.optLong("updated"),a.optLong("updated")));
        for(JSONObject entry:records){JSONObject subject=name.equals("history")?entry.optJSONObject("subject"):entry;if(subject!=null&&seen.add(subject.optString("subjectId")))out.add(subject);}return out;
    }
    public static String key(JSONObject subject,int season,int episode){return subject.optString("subjectId")+":"+season+":"+episode;}
    public long position(JSONObject subject,int season,int episode){JSONObject value=data.optJSONObject("history").optJSONObject(key(subject,season,episode));return value==null||value.optBoolean("completed")?0:value.optLong("position");}
    public void progress(JSONObject subject,int season,int episode,long position,long duration,boolean completed) throws JSONException {
        if(position<1000&&!completed)return;
        JSONObject history=data.getJSONObject("history");history.put(key(subject,season,episode),new JSONObject().put("subject",summary(subject)).put("season",season).put("episode",episode).put("position",position).put("duration",duration).put("completed",completed).put("updated",System.currentTimeMillis()));
        if(history.length()>300){String oldest=null;long at=Long.MAX_VALUE;Iterator<String> keys=history.keys();while(keys.hasNext()){String key=keys.next();long value=history.getJSONObject(key).optLong("updated");if(value<at){at=value;oldest=key;}}if(oldest!=null)history.remove(oldest);}
        save();
    }
    public void removeTitles(String name,Set<String> identities) throws JSONException {JSONObject section=data.getJSONObject(name);List<String> remove=new ArrayList<>();Iterator<String> keys=section.keys();while(keys.hasNext()){String key=keys.next();JSONObject record=section.getJSONObject(key);JSONObject subject=name.equals("history")?record.optJSONObject("subject"):record;if(subject!=null&&identities.contains(subject.optString("subjectId")))remove.add(key);}for(String key:remove)section.remove(key);save();}
}
