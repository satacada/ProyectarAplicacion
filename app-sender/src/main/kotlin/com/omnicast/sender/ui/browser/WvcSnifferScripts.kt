package com.omnicast.sender.ui.browser

/**
 * Scripts JavaScript inyectables optimizados para detección de streams HTML5,
 * XHR/Fetch hooks y detección de reproductores embebidos (Pelismart, Vidhide, Embed69, ok.ru).
 */
object WvcSnifferScripts {

    fun buildFallbackHooks(consolePrefix: String): String = """
        (function() {
            try {
                function isMediaStr(u) {
                    if (!u || typeof u !== 'string') return false;
                    var l = u.toLowerCase();
                    if (l.indexOf('.jpg') !== -1 || l.indexOf('.png') !== -1 || l.indexOf('.webp') !== -1 ||
                        l.indexOf('.gif') !== -1 || l.indexOf('.css') !== -1 || l.indexOf('.js') !== -1 ||
                        l.indexOf('i.mycdn.me') !== -1 || l.indexOf('st.mycdn.me') !== -1) return false;
                    if (l.indexOf('/embed/') !== -1 || l.indexOf('/embed69') !== -1 || l.indexOf('/vidurl/') !== -1) return false;
                    if (l.indexOf('init.mp4') !== -1 || l.indexOf('/init-') !== -1 || l.indexOf('.m4s') !== -1 || l.indexOf('.ts') !== -1) return false;
                    return l.indexOf('.m3u8') !== -1 || l.indexOf('.mp4') !== -1 || l.indexOf('.mpd') !== -1 ||
                           l.indexOf('/hls') !== -1 || l.indexOf('/stream/') !== -1 || l.indexOf('/manifest') !== -1 ||
                           l.indexOf('chunklist') !== -1 || l.indexOf('vd.mycdn') !== -1 ||
                           l.indexOf('p.php?url=') !== -1 || (l.indexOf('/p.php') !== -1 && l.indexOf('sig=') !== -1) ||
                           (l.indexOf('master.txt') !== -1 && l.indexOf('/hls') !== -1);
                }
                function isShortCommercial(v) {
                    if (!v) return false;
                    var d = v.duration;
                    if (typeof d === 'number' && !isNaN(d) && d > 0 && d < 60) return true;
                    var s = (v.currentSrc || v.src || '').toLowerCase();
                    if (s.indexOf('preroll') !== -1 || s.indexOf('ad-delivery') !== -1 || s.indexOf('/ads/') !== -1 || s.indexOf('imasdk') !== -1 || s.indexOf('cat-bolt') !== -1 || s.indexOf('ad.mp4') !== -1 || s.indexOf('vast') !== -1 || s.indexOf('commercial') !== -1) return true;
                    return false;
                }
                function reportVid(v, title) {
                    if (!v || isShortCommercial(v)) return;
                    var d = v.duration || 0;
                    if (d > 0 && d < 60) return;
                    var s = v.currentSrc || v.src;
                    if (!s || s.indexOf('blob:') === 0 || s.indexOf('data:') === 0) return;
                    if (!isMediaStr(s)) return;
                    var t = title || document.title || 'Video Web';
                    console.log('$consolePrefix' + JSON.stringify({type:'videoPlaying', video:s, title:t, duration:d, frameUrl:window.location.href || ''}));
                }
                function attachVid(v) {
                    if (!v || v.__omni) return;
                    v.__omni = true;
                    v.addEventListener('loadedmetadata', function() { reportVid(v); }, true);
                    v.addEventListener('durationchange', function() { reportVid(v); }, true);
                    v.addEventListener('play', function() { reportVid(v); }, true);
                    v.addEventListener('playing', function() { reportVid(v); }, true);
                    if (v.readyState >= 1) { reportVid(v); }
                }
                var vids = document.getElementsByTagName('video');
                for (var vi = 0; vi < vids.length; vi++) { attachVid(vids[vi]); }
                document.addEventListener('play', function(e) { if (e.target && e.target.tagName === 'VIDEO') attachVid(e.target); }, true);
                document.addEventListener('playing', function(e) { if (e.target && e.target.tagName === 'VIDEO') attachVid(e.target); }, true);

                window.__omniCapturedMedia = window.__omniCapturedMedia || [];
                var origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = function() {
                        var a = arguments[0];
                        var u = (typeof a === 'string') ? a : (a && a.url ? a.url : '');
                        if (isMediaStr(u)) {
                            window.__omniCapturedMedia = window.__omniCapturedMedia || [];
                            if (window.__omniCapturedMedia.indexOf(u) === -1) window.__omniCapturedMedia.push(u);
                            console.log('$consolePrefix' + JSON.stringify({type:'video', src:u, title:document.title, frameUrl:window.location.href || ''}));
                        }
                        return origFetch.apply(this, arguments);
                    };
                }
                var origOpen = XMLHttpRequest.prototype.open;
                if (origOpen) {
                    XMLHttpRequest.prototype.open = function(m, u) {
                        if (isMediaStr(u)) {
                            window.__omniCapturedMedia = window.__omniCapturedMedia || [];
                            if (window.__omniCapturedMedia.indexOf(u) === -1) window.__omniCapturedMedia.push(u);
                            console.log('$consolePrefix' + JSON.stringify({type:'video', src:u, title:document.title, frameUrl:window.location.href || ''}));
                        }
                        return origOpen.apply(this, arguments);
                    };
                }
                var origSrcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                if (origSrcDesc && origSrcDesc.set) {
                    var origSet = origSrcDesc.set;
                    Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                        set: function(val) {
                            var res = origSet.call(this, val);
                            if (typeof val === 'string' && val.indexOf('blob:') !== 0 && val.indexOf('data:') !== 0 && isMediaStr(val)) {
                                attachVid(this);
                            }
                            return res;
                        }
                    });
                }
                function sniffEmbeddedPlayers() {
                    try {
                        if (typeof window.jwplayer === 'function') {
                            var pl = window.jwplayer().getPlaylist ? window.jwplayer().getPlaylist() : null;
                            if (pl && pl.length) {
                                for (var pi = 0; pi < pl.length; pi++) {
                                    var f = pl[pi].file || (pl[pi].sources && pl[pi].sources[0] && pl[pi].sources[0].file);
                                    if (f && isMediaStr(f)) {
                                        console.log('$consolePrefix' + JSON.stringify({type:'video', src:f, title:pl[pi].title || document.title, frameUrl:window.location.href || ''}));
                                    }
                                }
                            }
                        }
                        var ifrs = document.querySelectorAll('iframe');
                        for (var fi = 0; fi < ifrs.length; fi++) {
                            var isrc = ifrs[fi].src || ifrs[fi].getAttribute('data-src') || '';
                            if (isrc && isMediaStr(isrc)) {
                                console.log('$consolePrefix' + JSON.stringify({type:'video', src:isrc, title:document.title, frameUrl:window.location.href || ''}));
                            }
                        }
                    } catch(_) {}
                }
                sniffEmbeddedPlayers();
                setTimeout(sniffEmbeddedPlayers, 1200);
                setTimeout(sniffEmbeddedPlayers, 3000);
            } catch(_) {}
        })();
    """.trimIndent()

    fun buildAggressiveRescanScript(consolePrefix: String): String = """
        (function() {
            try {
                var fUrl = window.location.href || '';
                if (typeof window.__omniResetAndRescan === 'function') {
                    window.__omniResetAndRescan();
                }
                if (window.__omniCapturedMedia && window.__omniCapturedMedia.length > 0) {
                    for (var m = 0; m < window.__omniCapturedMedia.length; m++) {
                        var cu = window.__omniCapturedMedia[m];
                        console.log('$consolePrefix' + JSON.stringify({type: 'video', video: cu, src: cu, title: document.title, frameUrl: fUrl}));
                    }
                }
                try {
                    if (window.performance && typeof window.performance.getEntriesByType === 'function') {
                        var res = window.performance.getEntriesByType('resource');
                        for (var r = 0; r < res.length; r++) {
                            var ru = res[r].name || '';
                            var lru = ru.toLowerCase();
                            if (lru.indexOf('.m3u8') !== -1 || lru.indexOf('.mp4') !== -1 || lru.indexOf('.mpd') !== -1 || lru.indexOf('/hls') !== -1 || lru.indexOf('/stream/') !== -1 || lru.indexOf('p.php?url=') !== -1) {
                                console.log('$consolePrefix' + JSON.stringify({type: 'video', video: ru, src: ru, title: document.title, frameUrl: fUrl}));
                            }
                        }
                    }
                } catch(_) {}
                var vids = document.querySelectorAll('video');
                for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    v.__omniMonitored = false;
                    v.__omni = false;
                    var d = v.duration || 0;
                    if (d > 0 && d < 60) continue;
                    var s = v.currentSrc || v.src;
                    if (s && s.indexOf('blob:') !== 0 && s.indexOf('data:') !== 0) {
                        console.log('$consolePrefix' + JSON.stringify({type: (!v.paused && i === 0) ? 'videoPlaying' : 'video', video: s, src: s, title: document.title, duration: d, frameUrl: fUrl}));
                    }
                    var chSrcs = v.querySelectorAll('source');
                    for (var cs = 0; cs < chSrcs.length; cs++) {
                        var cUrl = chSrcs[cs].src;
                        if (cUrl && cUrl.indexOf('blob:') !== 0 && cUrl.indexOf('data:') !== 0) {
                            console.log('$consolePrefix' + JSON.stringify({type: 'video', video: cUrl, src: cUrl, title: document.title, duration: d, frameUrl: fUrl}));
                        }
                    }
                    var hlsObj = (v.__hls && v.__hls.url) || (v._hls && v._hls.url) || (window.hls && window.hls.url);
                    if (hlsObj && typeof hlsObj === 'string' && hlsObj.indexOf('blob:') !== 0) {
                        console.log('$consolePrefix' + JSON.stringify({type: 'videoPlaying', video: hlsObj, src: hlsObj, title: document.title, duration: d, frameUrl: fUrl}));
                    }
                }
                if (typeof window.jwplayer === 'function') {
                    var pl = window.jwplayer().getPlaylist ? window.jwplayer().getPlaylist() : null;
                    if (pl && pl.length) {
                        for (var pi = 0; pi < pl.length; pi++) {
                            var f = pl[pi].file || (pl[pi].sources && pl[pi].sources[0] && pl[pi].sources[0].file);
                            if (f && typeof f === 'string') {
                                console.log('$consolePrefix' + JSON.stringify({type: 'video', src: f, title: pl[pi].title || document.title, frameUrl: fUrl}));
                            }
                        }
                    }
                }
                var ifrs = document.querySelectorAll('iframe');
                for (var j = 0; j < ifrs.length; j++) {
                    var isrc = ifrs[j].src || ifrs[j].getAttribute('data-src') || '';
                    if (!isrc || isrc.indexOf('http') !== 0) continue;
                    var lsrc = isrc.toLowerCase();
                    if (lsrc.indexOf('google') !== -1 || lsrc.indexOf('recaptcha') !== -1 || lsrc.indexOf('facebook') !== -1) continue;
                    if (lsrc.indexOf('.m3u8') !== -1 || lsrc.indexOf('.mp4') !== -1 || lsrc.indexOf('/stream/') !== -1 ||
                        lsrc.indexOf('fastream') !== -1 || lsrc.indexOf('ok.ru') !== -1 || lsrc.indexOf('streamwish') !== -1 ||
                        lsrc.indexOf('filemoon') !== -1 || lsrc.indexOf('vidhide') !== -1 || lsrc.indexOf('pelismart') !== -1 ||
                        lsrc.indexOf('pelisserieshoy') !== -1 || lsrc.indexOf('embed69') !== -1) {
                        console.log('$consolePrefix' + JSON.stringify({type: 'video', src: isrc, title: document.title, frameUrl: fUrl}));
                    }
                }
            } catch(_) {}
        })();
    """.trimIndent()
}
