package org.emransm.moviebox.tv;

import org.json.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Optional release metadata checks. No installation or silent APK downloads. */
final class Updates {
    static final String REPOSITORY="emransm877/moviebox-android-tv",VERSION="0.1.0";
    static JSONObject check() throws Exception {
        JSONObject data=new JSONObject(new String(Api.download("https://api.github.com/repos/"+REPOSITORY+"/releases/latest",512*1024),StandardCharsets.UTF_8));
        String tag=data.optString("tag_name");
        if(data.optBoolean("draft",true)||data.optBoolean("prerelease",true)||!Policies.newer(tag,VERSION))return null;
        String release="https://github.com/"+REPOSITORY+"/releases/tag/"+tag;
        if(!release.equals(data.optString("html_url")))return null;
        String name="MovieBox-Android-TV-"+tag.replaceFirst("^v","")+".apk";
        String download="https://github.com/"+REPOSITORY+"/releases/download/"+tag+"/"+name;
        JSONArray assets=data.optJSONArray("assets");if(assets==null)return null;
        for(int i=0;i<assets.length();i++){JSONObject asset=assets.getJSONObject(i);if(name.equals(asset.optString("name"))&&"uploaded".equals(asset.optString("state"))&&download.equals(asset.optString("browser_download_url"))&&asset.optLong("size")>0&&asset.optLong("size")<1024L*1024*1024)return new JSONObject().put("tag",tag).put("url",release);}
        return null;
    }
}
