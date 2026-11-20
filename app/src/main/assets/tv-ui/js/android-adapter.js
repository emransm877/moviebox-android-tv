(function(global){
  'use strict';
  if(!global.MovieBoxAndroidEnvironment)return;
  var API=global.MovieBoxAPI.prototype;
  API.request=function(url,body,headers,nativeApi){var self=this;return global.MovieBoxAndroidRequest({mode:'api',url:url,body:body,headers:headers||{}}).then(function(response){
    if(response.status<200||response.status>=300)throw new Error('Service HTTP '+response.status);
    var user=response.user;if(user){var token=JSON.parse(user).token;if(nativeApi)self.nativeToken=token;else self.webToken=token;}
    var result=JSON.parse(response.body);if(result.code!=null&&Number(result.code)!==0)throw new Error(result.message||'Service refused this request');
    var data=result.data==null?result:result.data;return global.MovieBoxArtwork?MovieBoxArtwork.prepare(data,self.bannerIndex):data;
  });};
  API.text=function(url){return global.MovieBoxAndroidRequest({mode:'subtitle',url:url}).then(function(response){if(response.status!==200)throw new Error('Subtitle download unavailable');return response.body;});};
})(window);
