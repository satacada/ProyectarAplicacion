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
                    return l.indexOf('.m3u8') !== -1 || l.indexOf('.mp4') !== -1 || l.indexOf('.mpd') !== -1 ||
                           l.indexOf('/hls') !== -1 || l.indexOf('/stream/') !== -1 || l.indexOf('/manifest') !== -1 ||
                           l.indexOf('chunklist') !== -1 || l.indexOf('cmd=video') !== -1 || l.indexOf('vd.mycdn') !== -1 ||
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
                    console.log('$consolePrefix' + JSON.stringify({type:'videoPlaying', video:s, title:t, duration:d}));
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

                var origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = function() {
                        var a = arguments[0];
                        var u = (typeof a === 'string') ? a : (a && a.url ? a.url : '');
                        if (isMediaStr(u)) {
                            console.log('$consolePrefix' + JSON.stringify({type:'video', src:u, title:document.title}));
                        }
                        return origFetch.apply(this, arguments);
                    };
                }
                var origOpen = XMLHttpRequest.prototype.open;
                if (origOpen) {
                    XMLHttpRequest.prototype.open = function(m, u) {
                        if (isMediaStr(u)) {
                            console.log('$consolePrefix' + JSON.stringify({type:'video', src:u, title:document.title}));
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
                                        console.log('$consolePrefix' + JSON.stringify({type:'video', src:f, title:pl[pi].title || document.title}));
                                    }
                                }
                            }
                        }
                        var ifrs = document.querySelectorAll('iframe');
                        for (var fi = 0; fi < ifrs.length; fi++) {
                            var isrc = ifrs[fi].src || ifrs[fi].getAttribute('data-src') || '';
                            if (isrc && isMediaStr(isrc)) {
                                console.log('$consolePrefix' + JSON.stringify({type:'video', src:isrc, title:document.title}));
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
                var vids = document.querySelectorAll('video');
                for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    var d = v.duration || 0;
                    if (d > 0 && d < 60) continue;
                    var s = v.currentSrc || v.src;
                    if (s && s.indexOf('blob:') !== 0 && s.indexOf('data:') !== 0) {
                        console.log('$consolePrefix' + JSON.stringify({type:'videoPlaying', video:s, title:document.title, duration:d}));
                    }
                }
                if (typeof window.jwplayer === 'function') {
                    var pl = window.jwplayer().getPlaylist ? window.jwplayer().getPlaylist() : null;
                    if (pl && pl.length) {
                        for (var pi = 0; pi < pl.length; pi++) {
                            var f = pl[pi].file || (pl[pi].sources && pl[pi].sources[0] && pl[pi].sources[0].file);
                            if (f && typeof f === 'string') {
                                console.log('$consolePrefix' + JSON.stringify({type:'video', src:f, title:pl[pi].title || document.title}));
                            }
                        }
                    }
                }
                var ifrs = document.querySelectorAll('iframe');
                for (var j = 0; j < ifrs.length; j++) {
                    var isrc = ifrs[j].src || ifrs[j].getAttribute('data-src') || '';
                    if (isrc && (isrc.indexOf('.m3u8') !== -1 || isrc.indexOf('.mp4') !== -1 || isrc.indexOf('/stream/') !== -1)) {
                        console.log('$consolePrefix' + JSON.stringify({type:'video', src:isrc, title:document.title}));
                    }
                }
            } catch(_) {}
        })();
    """.trimIndent()
}
