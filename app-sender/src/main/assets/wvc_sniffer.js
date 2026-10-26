
//delete console;
var ibConsole = ibConsole ||  window.console;
var ibConsoleLog = ibConsoleLog || window.console.log;
//window.console = siteConsole;
var fakeIframeName = 'ibFakeIframe';
/* try {
    var fakeIframe = document.createElement('iframe');
    fakeIframe.style.display = 'none';
    fakeIframe.id=fakeIframeName;``
    document.body.appendChild(i);
    ibConsole = i.contentWindow.console;
} catch (e) {

} */

var metaTitle = null;
var metaImageURL = null;
var metaInfoNotSet = true;
var sentTopTitle = false;
if (ibIsDocumentSomewhatReady(document)) {
    ibAddInitFunctions();
    ibSetMetaInfo();

}
function isES6() {
    try {
        eval('"use strict"; class foo {}');
        return true;
    }
    catch (e) {
        return false;
    }
}



function DomMonitor(document) {
    this.monitors = {};
}



function ibSetMetaInfo() {

    if (metaInfoNotSet) {
        var tags = document.getElementsByTagName('meta');
        for (var i = 0; i < tags.length; i++) {
            metaInfoNotSet = false;
            var name = tags[i].getAttribute('name');
            if (!name) {
                name = tags[i].getAttribute('property');
            }
            var content = tags[i].getAttribute('content');
            if (name != null && content != null) {
                if (name == 'og:title') {
                    metaTitle = content;
                } else if (name == 'og:image' && !metaImageURL) {
                    metaImageURL = content;
                } else if (name == "twitter:title" && metaTitle == null) {
                    metaTitle = content;
                } else if (name == 'twitter:image' && !metaImageURL) {
                    metaImageURL = content;
                } else if (name == 'twitter:image:src' && !metaImageURL) {
                    metaImageURL = content;
                }
            }

        }

        ibSendTopTitle();
    } else {
        ibSendTopTitle();
    }
}
function ibSendTopTitle() {
    if (!sentTopTitle && !ibInIframe()) {
        if (!metaInfoNotSet) {
            sentTopTitle = true;
        }
        if (metaTitle || metaImageURL) {
            var videoInfo = new Object();
            videoInfo.type = "pageInfo";
            videoInfo.title = metaTitle;
            videoInfo.poster = metaImageURL;
            videoInfo.from = self.document.location.href;
            ibSend(videoInfo);
        }
    }
}
function ibInIframe() {
    try {
        return window.self !== window.top;
    } catch (e) {
        //ibConsole.log(e);
        return true;
    }
}
function ibIsDocumentSomewhatReady(doc) {
    if (document.readyState === "complete" || document.readyState === "interactive") {
        return true;
    } else {
        return false;
    }
}

ibAddEventListener(self.document, "DOMContentLoaded", ibAddInitFunctions, false);


function ibAddInitFunctions() {
    ibAddMessagePostHandlers();
    ibAddClickHandlerForAll();
    ibFindAllVideos();
    //this might be happening too early
    setTimeout(ibFindAllVideos,2000)
}

function ibAddEventListener(obj, ev, func, when) {
    obj.removeEventListener(ev, func, when);
    obj.addEventListener(ev, func, when);
}

function ibAddClickHandlerForAll() {
    // ibFindAllDocuments();

    ibAddClickHandler(self.document);
}
function ibAddClickHandler(doc) {
    if (isES6) {


        var domMonitor = new DomMonitor(doc);
        domMonitor.monitors['video'] = function (newVideos) {
            ibFindAllVideos();
        };
        domMonitor.monitors['track'] = function (newTracks) {
            ibFindAllVideos();
        };
    } else {
        ibAddEventListener(doc, "click", ibClicked, true);

    }
}
function ibClicked() {
    ibFindAllVideos();
}
function ibFindAllDocuments() {
    var docs = new Array();
    docs.push(self.document);
    ibFindAllIFrames(document, docs);
    return docs;
}
function ibCanAccessIFrame(iframe) {
    var html = null;
    try {
        // deal with older browsers
        var doc = iframe.contentDocument || iframe.contentWindow.document;
        if (doc != null && doc.body != null) {
            html = doc.body.innerHTML;
        }
    } catch (err) {
        // do nothing
    }

    return (html !== null);
}
function ibFindAllIFrames(d, docs) {
    var ibFrames = d.getElementsByTagName('iframe');
    for (var i = 0; i < ibFrames.length; i++) {
        try {

            var ibFrame = ibFrames[i];
            if (ibFrame.id != fakeIframeName) {
                if (ibCanAccessIFrame(ibFrame)) {
                    var doc = ibFrame.contentDocument;
                    if (ibIsDocumentSomewhatReady(doc)) {
                        ibAddSelf(doc);
                    } else {
                        ibAddIframeLoadEvent(ibFrame);
                    }
                    docs.push(doc);
                    ibFindAllIFrames(doc, docs);
                } else {
                    //    ibConsole.log("Unable to access iframe " + ibFrame.src );


                }
            }

        } catch (e) {
            //must be cross domain
        }
    }
}
function ibAddIframeLoadEvent(iframe) {
    ibAddEventListener(iframe, "load", ibIframeLoaded, false);

}
function ibIframeLoaded() {
    try {
        var doc = this.contentDocument;
        ibAddSelf(doc);

    } catch (e) {
        //must be cross domain
    }
}
var ibScriptIDTag = "ibScript"
function ibAddSelf(doc) {

    if (!doc.getElementById(ibScriptIDTag)) {
        var script = doc.createElement('script');
        script.src = '/ibjslib/wai2iku6/ibfunctions.js';
        script.id = ibScriptIDTag;
        script.async = false;

        doc.documentElement.appendChild(script);
        console.log("added self");
    }
}
var alreadySearchedForm = false
function ibFindAllVideos() {
    var docs = ibFindAllDocuments();
    var foundVideos = new Array();
    for (var d = 0; d < docs.length; d++) {
        try {
            var videos = docs[d].getElementsByTagName('video');
            for (var i = 0; i < videos.length; i++) {
                var video = videos[i];
                foundVideos.push(video);
                ibAddEventListener(video, "play", ibVideoPlaying, true);
            }

        } catch (e) {
            // ibLog("Error searching videos " + e);
        }
    }
    if (!alreadySearchedForm && (foundVideos == null || foundVideos.length == 0)){
        alreadySearchedForm = true;
        if (location.href.includes("wootly.ch")){
            var elements = document.getElementsByTagName("form");
            if (elements && elements.length == 1){
                elements[0].submit();
            }
        }
    }
    ibSendListOfVideos(foundVideos);
    var findVideos = new Object();
    findVideos.type = "findVideos";
    ibPostMessageToIFrame(findVideos);
    try {
        var video = document.getElementsByTagName('video');
        if (video != null && video.length > 0) {
            var dataset = video[0].dataset;
            if (dataset != null) {

                var payload = dataset.payload;
                if (payload != null) {
                    var unpack = jsonpack["unpack"](atob(payload))
                    if (unpack != null) {
                        var videoInfo = new Object();
                        videoInfo.type = "m3u8Segments";
                        videoInfo.from = self.document.location.href;
                        videoInfo.segments = unpack;
                        ibSend(videoInfo);
                    }
                }
            }
        }
    } catch (e) {
    }
    try{
        if (window.playerConfig) {
            var vimeoConfig = new Object();
            vimeoConfig.type = "vimeoConfig";
            vimeoConfig.playerConfig = window.playerConfig;
            ibSend(vimeoConfig);
        }
    }catch(e){
        
    }


}
function ibPauseAllVideos() {
    var videos = self.document.getElementsByTagName('video');
    for (var i = 0; i < videos.length; i++) {
        var video = videos[i];
        video.pause();
    }
    var pause = new Object();
    pause.type = "pause";
    ibPostMessageToIFrame(pause);
}
function ibSkipAds() {
    var videos = self.document.getElementsByTagName('video');
    for (var i = 0; i < videos.length; i++) {
        var video = videos[i];
        //video.pause();
        if (video.currentTime < video.duration - 0.1) {
            video.currentTime = video.duration - 0.1;
            video.play();
        }
    }
    var pause = new Object();
    pause.type = "pause";
    ibPostMessageToIFrame(pause);
}
function ibVideoPlaying() {
    if (this.currentSrc && (this.currentSrc.indexOf('stub.mp4') !== -1 || this.currentSrc.indexOf('blank.mp4') !== -1 || this.currentSrc.indexOf('dummy.mp4') !== -1)) {
        return;
    }
    var ev = new Object();
    ev.type = "videoPlaying";
    ev.video = this.currentSrc;
    ev.from = self.document.location.href;
    ev.currentTime = this.currentTime;
    ev.duration = this.duration;
    if (this.poster) {
        ev.poster = this.poster;
    }
    if (this.crossOrigin){
        ev.crossOrigin = this.crossOrigin;
    }
    addTextTracks(this, ev);
    ibSetMetaInfo();

    if (metaTitle) {
        ev.title = metaTitle;
    }
    if (!ev.poster && metaImageURL) {

        ev.poster = metaImageURL;
    }
    try {
        if (this.classList && this.classList.contains('fp-engine')) {

            var flowplayer = getFlowPlayer();

            if (flowplayer && flowplayer.video) {
                if (!ev.textTracks) {
                    ev.textTracks = new Array();
                }

                if (flowplayer.video.subtitles) {
                    for (i = 0; i < flowplayer.video.subtitles.length; i++) {
                        
                        var trackInfo = new Object()
                        trackInfo.url = flowplayer.video.subtitles[i].src;
                        trackInfo.lang = flowplayer.video.subtitles[i].label;
                        ev.textTracks.push(trackInfo)

                    }
                }

            }


        }
    } catch (e) {
        ibLog(e);
    }
    if (lastVideoPlaying != null){
        if (ev.video == lastVideoPlaying.video ){
            var tracks = ev.textTracks;
            var savedTracks = lastVideoPlaying.textTracks;
            if (tracks != null && savedTracks != null){
                if (compareTextTrackArrays(tracks,savedTracks)){
                    //skip
                    return;
                }
            }   
            if (tracks == null && savedTracks == null){
                //skip
                return;
            
            }
        }
    }
    lastVideoPlaying = ev
    ibSend(ev);


}
var lastVideoPlaying = null
function compareTextTrackArrays(array1, array2) {
    if (array1.length !== array2.length) {
      return false;
    }
  
    return array1.every((value, index) => value.url == array2[index].url);
  }
function addTextTracks(video, v) {
    try {
        var tracks = video.getElementsByTagName('track');
        var videoTracks = new Array();
        v.textTracks = videoTracks;
        if (tracks.length > 0) {
            for (var t = 0; t < tracks.length; t++) {
                var trackInfo = new Object()
                var trackSrc = tracks[t].src;
                trackInfo.url = trackSrc;
                trackInfo.lang = tracks[t].srclang;
                if (!trackInfo.lang){
                    trackInfo.lang = tracks[t].label;
                }
                videoTracks[t] = trackInfo;
            }
        }
    } catch (e) {
        //  ibLog(video.outerHTML+" : "+e);
    }
}
function ibSendListOfVideos(videos) {
    var listHolder = new Object();
    listHolder.type = "videoList";
    listHolder.from = self.document.location.href;

    var videoList = new Array();
    listHolder.videoList = videoList;
    ibSetMetaInfo();
    if (metaTitle) {
        listHolder.title = metaTitle;
    }
    ibAddTCPlayerVideos(videoList);
    //might make more sense to run this on all documents
    if (typeof jwplayer == 'function') {
        try {
            var playlists = jwplayer().getPlaylist();
            var lengthOfPlaylist = playlists.length;
            for (var p = 0; p < lengthOfPlaylist; p++) {
                var playlist = playlists[p];
                var v = new Object();
                    
                    if (playlist.tracks && playlist.tracks.length > 0) {
                        var videoTracks = new Array();
                        v.textTracks = videoTracks;
                        for (var t = 0; t < playlist.tracks.length; t++) {
                            var trackSrc = playlist.tracks[t].file;
                            var trackInfo = new Object()
                            trackInfo.url = trackSrc;
                            trackInfo.lang = playlist.tracks[t].label;
                            videoTracks[t] = trackInfo;

                        }
                    }
                    if (metaImageURL) {
                        v.poster = metaImageURL;
                    }
                    if (playlist.title){
                        v.jwTitle = playlist.title;
                    }
                    videoList.push(v);
                var jSources = playlist.sources;
                for (var i = 0; i < jSources.length; i++) {
                    var jSource = jSources[i];
                    
                    var sources = new Array();
                    
                    v.sources = sources;
                    
                    var current = new Object();

                    current.source = jSource.file;
                    current.label = jSource.label;
                    //mp4 is often misconfigured
                    if (jSource.type && jSource.type != 'mp4') {
                        current.videoType = jSource.type;
                    }
                    current.jwplayer = true;
                    if (jSource.tracks && jSource.tracks.length > 0) {
                        var videoTracks = new Array();
                        v.textTracks = videoTracks;
                        var trackCount = 0;
                        for (var t = 0; t < jSource.tracks.length; t++) {

                            var trackSrc = jSource.tracks[t].file;
                            var kind = jSource.tracks[t].kind;
                            if (kind == "thumbnails") {

                            } else {
                                
                                var trackSrc = playlist.tracks[t].file;
                                var trackInfo = new Object()
                                trackInfo.url = trackSrc;
                                trackInfo.lang = jSource.tracks[t].label;
                                videoTracks[trackCount++] = trackInfo;

                            }

                        }
                    }
                    sources.push(current);

                }
            }
        } catch (jwe) {
            //  ibLog("Error with jw player "+jwe);
        }
    }
    var phVideoElement = document.querySelector('div #videoShow[data-default]');
    try {
        if (phVideoElement) {
            ibAddVideoToList(videoList, phVideoElement.getAttribute('data-default'), null, null);
        }
    } catch (e) {
        ibLog(e);
    }
    try {
        var flowplayer = getFlowPlayer();

        if (flowplayer && flowplayer.video) {
            var videoSrc = flowplayer.video.src;
            var videoTracks = new Array();
            if (flowplayer.video.subtitles) {
                for (i = 0; i < flowplayer.video.subtitles.length; i++) {
                    var trackInfo = new Object()
                    trackInfo.url = flowplayer.video.subtitles[i].src;
                    trackInfo.lang = flowplayer.video.subtitles[i].label;
                    videoTracks[i] = trackInfo
                }
            }
            ibAddVideoToList(videoList, videoSrc, videoTracks);
        }

    } catch (e) {
        ibLog(e);
    }

    try {

        if (typeof Clappr !== 'undefined') {
            if (Clappr.PlayerInfo._players) {
                var players = Clappr.PlayerInfo._players;
                for (var playerKey in players) {
                    var player = players[playerKey];
                    var url = player.options.source;
                    var mime = player.options.mimeType;
                    ibAddVideoToList(videoList, url, null, mime);
                }


            }
        }

    } catch (e) {
        ibLog(e);
    }
    try {
        if (document.location.host.toLowerCase().endsWith('facebook.com')) {

            var stores = document.querySelectorAll('[data-store]');
            for (i = 0; i < stores.length; i++) {
                var store = stores[i];
                var dataset = store.dataset;
                if (dataset) {
                    var json = JSON.parse(dataset.store);
                    if (json.videoID && json.src) {
                        var videoURL = json.src;

                        var video = new Object();
                        if (metaImageURL) {
                            video.poster = metaImageURL;
                        }
                        var sources = new Array();
                        videoList.push(video);
                        video.sources = sources;

                        var current = new Object();

                        current.source = json.src;
                        current.jwplayer = false;
                        sources.push(current);
                    }
                }
            }

        }
    } catch (e) {
        ibLog(e);
    }
    try{
        //ok.ru and vk sites
        if (typeof OneVideoPlayer == 'object') {
            var oneVideo = OneVideoPlayer.getInstance();
            if (oneVideo && oneVideo.options && oneVideo.options.flashvars && oneVideo.options.flashvars.metadata) {
                var meta = oneVideo.options.flashvars.metadata;
                var okTitle = (meta.movie && meta.movie.title) ? meta.movie.title : null;
                var poster = oneVideo.options.poster || (meta.movie ? meta.movie.poster : null);
                if (okTitle && !metaTitle) {
                    metaTitle = okTitle;
                }
                var m3u8 = meta.hlsManifestUrl;
                if (m3u8) {
                    ibAddVideoToList(videoList, m3u8, null, "application/vnd.apple.mpegurl", poster, "HLS Adaptativo", okTitle);
                }
                var videosArrayList = meta.videos;
                if (videosArrayList != null && videosArrayList.length > 0){
                    for (var okIdx = 0; okIdx < videosArrayList.length; okIdx++){
                        var okItem = videosArrayList[okIdx];
                        var mp4URL = okItem.url;
                        var qName = okItem.name ? (okItem.name + "p") : "Directo";
                        if (mp4URL != null){
                            ibAddVideoToList(videoList, mp4URL, null, "video/mp4", poster, qName, okTitle);
                        }
                    }
                }
            }
        }
    }catch (e) {
        ibLog(e);
    }
    for (var i = 0; i < videos.length; i++) {
        try {
            var video = videos[i];
            var v = new Object();
            addTextTracks(video, v);
            if (video.poster) {
                v.poster = video.poster;
            }
            if (video.crossOrigin){
                v.crossOrigin = video.crossOrigin;
            }
            var sources = new Array();

            v.sources = sources;
            if (video.currentSrc && video.currentSrc.indexOf('stub.mp4') === -1 && video.currentSrc.indexOf('dummy.mp4') === -1) {
                var current = new Object();
                current.source = video.currentSrc;
                current.currentTime = video.currentTime;
                current.duration = video.duration;
                sources.push(current);
            }
            var src = video.getElementsByTagName('source');
            if (src.length > 0) {
                for (var j = 0; j < src.length; j++) {
                    var sSrc = src[j].src || '';
                    var sLower = sSrc.toLowerCase();
                    if (sLower.indexOf('stub.mp4') !== -1 || sLower.indexOf('dummy.mp4') !== -1 || sLower.indexOf('blank.mp4') !== -1) {
                        continue;
                    }
                    var current = new Object();
                    current.source = sSrc;
                    current.currentTime = src[j].currentTime;
                    current.duration = src[j].duration;
                    sources.push(current);
                }
            }
            var dataSrc = video.getAttribute('data-src');
            if (dataSrc) {
                var current = new Object();
                current.source = dataSrc;
                sources.push(current);
            }
            if (v.sources.length > 0) {
                videoList.push(v);
            }
            var hrefs = video.getElementsByTagName('a');
            if (hrefs.length > 0) {
                for (var j = 0; j < hrefs.length; j++) {
                    var current = new Object();
                    current.source = hrefs[j].href;
                    if ('https://videojs.com/html5-video-support/' != current.source) {
                        sources.push(current);
                    }
                }

            }

            if (!v.poster && metaImageURL) {
                v.poster = metaImageURL;
            }

        } catch (e) {
            // ibLog(video.outerHTML + " : " + e);
        }

    }
    //rai
    var metaMP4Videos = self.document.getElementsByName('videourl_mp4');
    ibAddMetaVideosToList(metaMP4Videos, videoList);
    var metaH264Videos = self.document.getElementsByName('videourl_h264');
    ibAddMetaVideosToList(metaH264Videos, videoList);
    //from some page that converts this variable to a blob
    try {
        if (typeof urlVideo !== 'undefined') {
            ibAddVideoToList(videoList, urlVideo, null, null);
        }

    } catch (e) {
        ibLog(e);
    }
    if (listHolder.videoList.length > 0) {
        ibSend(listHolder);
    }
}
function ibAddVideoToList(videoList, source, videoTracks, mimeType = null, poster = null, label = null, videoTitle = null) {
    if (!source || typeof source !== 'string') return;
    var sLower = source.toLowerCase();
    if (sLower.indexOf('stub.mp4') !== -1 || sLower.indexOf('dummy.mp4') !== -1 || sLower.indexOf('blank.mp4') !== -1) {
        return;
    }
    var v = new Object();
    if (poster != null){
        v.poster = poster;
    }else if (metaImageURL ) {
        v.poster = metaImageURL;
    }
    if (videoTitle) {
        v.title = videoTitle;
    }
    var sources = new Array();
    videoList.push(v);
    v.sources = sources;
    var current = new Object();
    current.source = source;
    current.jwplayer = false;
    current.mimeType = mimeType;
    if (label) {
        current.label = label;
    }
    v.textTracks = videoTracks;
    sources.push(current);
}

function ibAddTCPlayerVideos(videoList) {
    if (typeof TCPlayer === 'undefined' || typeof TCPlayer.getPlayers !== 'function') return;
    try {
        var players = TCPlayer.getPlayers();
        var seen = Object.create(null);
        Object.keys(players || {}).forEach(function (id) {
            try {
                var player = players[id];
                if (!player || (player.isDisposed && player.isDisposed())) return;
                var options = typeof player.options === 'function' ? player.options() : {};
                var sources = typeof player.currentSources === 'function' ? player.currentSources() : null;
                if (!sources || !sources.length) sources = options.sources || [];
                if (!Array.isArray(sources)) sources = [sources];
                sources.forEach(function (source) {
                    var src = typeof source === 'string' ? source : source && source.src;
                    if (typeof src !== 'string' || !/^webrtc:\/\//i.test(src)) return;
                    var fallback = options.webrtcConfig && options.webrtcConfig.fallbackUrl;
                    var url;
                    if (typeof fallback === 'string' && fallback) {
                        url = new URL(fallback, document.baseURI);
                    } else {
                        url = new URL(src.replace(/^webrtc:\/\//i, 'https://'));
                        if (Array.from(url.searchParams.keys()).some(function (key) { return key.indexOf('tabr_') === 0; })) return;
                        url.pathname = /\.sdp$/.test(url.pathname)
                            ? url.pathname.replace(/\.sdp$/, '.m3u8') : url.pathname + '.m3u8';
                    }
                    if (!/^https?:$/.test(url.protocol) || seen[url.href]) return;
                    seen[url.href] = true;
                    ibAddVideoToList(videoList, url.href, null, 'application/vnd.apple.mpegurl',
                        typeof player.poster === 'function' ? player.poster() : null);
                    if (videoList.length > 0 && videoList[videoList.length - 1].sources && videoList[videoList.length - 1].sources[0]) {
                        videoList[videoList.length - 1].sources[0].validateHls = true;
                    }
                });
            } catch (e) { ibLog(e); }
        });
    } catch (e) { ibLog(e); }
}

function getFlowPlayer() {
    if (window.flowplayer && typeof window.flowplayer === 'function') {
        return window.flowplayer();
    }
}

function ibAddMetaVideosToList(metaMP4Videos, videoList) {
    for (var i = 0; i < metaMP4Videos.length; i++) {
        var mp4 = metaMP4Videos[i];

        var v = new Object();
        if (metaImageURL) {
            v.poster = metaImageURL;
        }
        var sources = new Array();
        videoList.push(v);
        v.sources = sources;
        if (mp4.content) {
            var current = new Object();
            current.source = mp4.content;
            sources.push(current);
        }

    }
}

function ibStringify(obj) {
    if (!JSON.stringify) {
        var ibFunctionsScript = document.createElement('script');
        ibFunctionsScript.type = 'text/javascript';
        ibFunctionsScript.src = 'https://ibjslib/json2.js';
        ibFunctionsScript.async = false;
        self.document.documentElement.appendChild(ibFunctionsScript);
    }
    return JSON.stringify(obj);

};

function ibAddMessagePostHandlers() {
    ibAddEventListener(window, "message", ibMessageReceived, true);
}
var ibMessageToIFrameTAG = "ibMessageToIFrame";
var ibMessageToParentTAG = "ibMessageToParent";
function ibMessageReceived(e, undefined) {
    try {

        var message = e.data;

        var fromIFrame = message.indexOf(ibMessageToParentTAG) >= 0;
        var fromParent = message.indexOf(ibMessageToIFrameTAG) >= 0;
        if (fromIFrame || fromParent) {
            if (fromParent) {
                message = message.substring(ibMessageToIFrameTAG.length);

            } else {
                message = message.substring(ibMessageToParentTAG.length);
            }

            var json = JSON.parse(message);
            if (self.document.location.href == json.from) {
                ibLog("got message from same doc, ignoring ");
                return;
            }
            var t = json.type;
            if (fromParent) {
                switch (t) {
                    case "findVideos":
                        // ibFindAllDocuments();
                        ibFindAllVideos();

                        break;
                    case "pause":

                        ibPauseAllVideos();

                        break;
                    default:
                    // ibLog("Unexpected messsage from parent to iframe " + message);
                }

            } else {
                try {

                    switch (t) {


                        default:
                        // ibLog("Unexpected messsage from iframe to parent " + message);
                    }
                } catch (ex) {

                }
            }
        }
    } catch (ge) {

    }
}


function ibPostMessageToIFrame(message) {
    message.from = self.document.location.href;
    try {
        var ibFrames = self.document.getElementsByTagName('iframe');

        for (var i = 0; i < ibFrames.length; i++) {
            try {
                if (ibFrames[i] != self) {

                    if (!ibCanAccessIFrame(ibFrames[i])) {
                        //  ibConsole.log("Got iframe " + ibFrames[i].src +" from " + self.location);
                        ibFrames[i].contentWindow.postMessage(ibMessageToIFrameTAG + ibStringify(message), '*');
                    }


                }
            } catch (e) {
                // ibLog("Exception sending message to iframe " + e + " from " + ibFrames[i]);

            }
        }


    } catch (e) {
        //  ibLog("Exception sending message to iframes " + e);
    }

}
function ibLog(log) {
    var jsonLog = new Object();
    jsonLog.message = log;
    jsonLog.type = "log";
    ibSend(jsonLog);

}
function ibSend(obj) {
    obj.ibMessage = true;
    var message = ibStringify(obj);
    ibConsoleLog(message);
    //ibConsole.log(message);


}
function ibPostMessageToParent(message) {
    window.parent.postMessage(ibMessageToParentTAG + ibStringify(message), '*');

}

(function () {
    if (self == top) {
        var send = new Object();
        send.type = 'functionsLoaded';


        try {
            ibSend(send);
        } catch (ex) {
        }

    }
    var iframes = self.document.getElementsByClassName('ibWorkaroundIframe');
    if (iframes.length > 0) {
        for (i = 0; i < iframes.length; i++) {
            var iframe = iframes[i];
            iframe.parentNode.removeChild(iframe);

        }
    }


})();

(function () {
    try {
        if (typeof window.MediaMetadata === 'undefined') {
            window.MediaMetadata = function (init) { Object.assign(this, init || {}); };
        }
        if (!('mediaSession' in navigator)) {
            Object.defineProperty(navigator, 'mediaSession', {
                value: {
                    metadata: null,
                    playbackState: 'none',
                    setActionHandler: function () {},
                    setPositionState: function () {}
                },
                configurable: true
            });
        }
    } catch (e) {}
})();