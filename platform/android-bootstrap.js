(function(global){
  'use strict';
  if(global.MovieBoxAndroidUnsupported)return;
  var bridge=global.MovieBoxAndroidBridge;if(!bridge)return;
  global.MovieBoxAndroidEnvironment=JSON.parse(bridge.environment());
  try{
    var saved=bridge.cachedState();if(saved){var nativeState=JSON.parse(saved);if(nativeState.saved&&nativeState.history&&nativeState.settings){localStorage.setItem('moviebox-tv-library',saved);localStorage.setItem('moviebox-tv-library-backup',saved);}}
    localStorage.setItem('moviebox-tv-theme',global.MovieBoxAndroidEnvironment.theme||'obsidian');
  }catch(_){}
  var requests=new Map(),callbacks=new Map(),serial=0,epoch=0,listener={},snapshot={state:'NONE',position:0,duration:0,tracks:[],selected:[]};
  function command(action,args,success,failure){var id=++serial;if(success||failure)callbacks.set(id,{success:success,failure:failure});bridge.command(JSON.stringify({action:action,args:args||{},id:id,epoch:epoch}));return id;}
  global.MovieBoxAndroidRequest=function(options){return new Promise(function(resolve,reject){var id=++serial;requests.set(id,{resolve:resolve,reject:reject});bridge.request(id,JSON.stringify(options));});};
  global.MovieBoxAndroidResolve=function(id,result,error){var entry=requests.get(id);if(!entry)return;requests.delete(id);if(error)entry.reject(new Error(error));else entry.resolve(result);};
  global.MovieBoxAndroidEvent=function(event){
    if(event.epoch!==epoch)return;
    if(event.snapshot)snapshot=Object.assign(snapshot,event.snapshot);
    if(event.type==='prepared'||event.type==='seeked'||event.type==='failure'){
      var callback=callbacks.get(event.id);callbacks.delete(event.id);
      if(callback){if(event.type==='failure'){if(callback.failure)callback.failure({name:event.error||'ANDROID_PLAYER_ERROR'});}else if(callback.success)callback.success();}
    }
    if(event.type==='buffering'&&listener.onbufferingstart)listener.onbufferingstart();
    if(event.type==='ready'&&listener.onbufferingcomplete)listener.onbufferingcomplete();
    if(event.type==='progress'&&listener.oncurrentplaytime)listener.oncurrentplaytime(snapshot.position);
    if(event.type==='ended'&&listener.onstreamcompleted)listener.onstreamcompleted();
    if(event.type==='error'&&listener.onerror)listener.onerror(event.error||'PLAYER_ERROR_CONNECTION_FAILED');
    if(event.type==='state'&&global.MovieBoxTV&&MovieBoxTV.player.active){MovieBoxTV.player.paused=snapshot.state==='PAUSED';MovieBoxTV.player.updatePlayButton();}
  };
  global.MovieBoxAndroidAVPlayer={
    open:function(url){epoch++;callbacks.clear();snapshot={state:'IDLE',position:0,duration:0,tracks:[],selected:[]};command('open',{url:url});},
    close:function(){command('close');epoch++;callbacks.clear();snapshot.state='NONE';},
    setListener:function(value){listener=value||{};},
    setStreamingProperty:function(key,value){command('property',{key:key,value:value});},
    setBufferingParam:function(){},setDisplayRect:function(){},
    setDisplayMethod:function(mode){command('display',{mode:mode});},
    prepareAsync:function(success,failure){command('prepare',{},success,failure);},
    play:function(){snapshot.state='PLAYING';command('play');},
    pause:function(){snapshot.state='PAUSED';command('pause');},
    seekTo:function(position,success,failure){command('seek',{position:position},success,failure);},
    setSelectTrack:function(type,index){if(type!=='AUDIO')throw new Error('Unsupported track type');command('audio',{index:Number(index)});},
    getState:function(){return snapshot.state;},getDuration:function(){return snapshot.duration;},getCurrentTime:function(){return snapshot.position;},
    getTotalTrackInfo:function(){return snapshot.tracks;},getCurrentStreamInfo:function(){return snapshot.selected;}
  };
  var volumeListener=null;
  global.MovieBoxAndroidAudio={getVolume:function(){return bridge.volume();},isMute:function(){return bridge.muted();},setVolume:function(value){command('volume',{value:value});},setMute:function(value){command('mute',{value:value});},setVolumeChangeListener:function(callback){volumeListener=callback;}};
  global.MovieBoxAndroidVolumeChanged=function(){if(volumeListener)volumeListener();};
  global.MovieBoxAndroidKey=function(key,down,repeat){
    var active=document.activeElement;
    var event=new KeyboardEvent(down?'keydown':'keyup',{key:key,bubbles:true,cancelable:true,repeat:!!repeat});
    (active||document).dispatchEvent(event);
    // Synthetic Android key events have no browser default action for ranges.
    // Keep explicit player seek handling above authoritative when it prevents default.
    if(down&&!event.defaultPrevented&&active&&!active.disabled&&active.tagName==='INPUT'&&active.type==='range'&&(key==='ArrowLeft'||key==='ArrowRight')){
      if(active.id==='volume-slider'){
        var volumeStep=Number(global.MovieBoxAndroidEnvironment.volumeStep)||1;
        active.value=Math.max(0,Math.min(100,Number(active.value)+(key==='ArrowLeft'?-volumeStep:volumeStep)));
      }else if(key==='ArrowLeft')active.stepDown();else active.stepUp();
      active.dispatchEvent(new Event('input',{bubbles:true}));
      active.dispatchEvent(new Event('change',{bubbles:true}));
    }
    if(down&&key==='Enter'&&!event.defaultPrevented&&active&&active.tagName==='BUTTON')active.click();
  };
  document.addEventListener('focusin',function(){var active=document.activeElement;bridge.textFocus(!!(active&&(active.tagName==='TEXTAREA'||active.tagName==='INPUT'&&!/^(range|checkbox|radio|button|submit)$/i.test(active.type))));});
  document.addEventListener('DOMContentLoaded',function(){
    var page=document.getElementById('player-page');
    new MutationObserver(function(){var open=!page.hidden;document.documentElement.classList.toggle('android-video-open',open);document.documentElement.style.setProperty('background-color',open?'transparent':(global.MovieBoxThemes?global.MovieBoxThemes.record(document.documentElement.dataset.theme).palette.base:'#090e12'),'important');bridge.videoVisible(open);}).observe(page,{attributes:true,attributeFilter:['hidden']});
  });
})(window);
