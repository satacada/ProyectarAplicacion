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
                    return l.indexOf('.m3u8') !== -1 || l.indexOf('.mp4') !== -1 || l.indexOf('.mpd') !== -1 ||
                           l.indexOf('/hls/') !== -1 || l.indexOf('/stream/') !== -1 || l.indexOf('/manifest') !== -1 ||
                           l.indexOf('chunklist') !== -1 || l.indexOf('cmd=video') !== -1 || l.indexOf('vd.mycdn') !== -1 ||
                           l.indexOf('morencius.com') !== -1 || l.indexOf('swdyu.com') !== -1;
                }
                function isShortCommercial(v) {
                    if (!v) return false;
                    if (v.duration && v.duration > 0 && v.duration < 16) return true;
                    var s = (v.currentSrc || v.src || '').toLowerCase();
                    if (s.indexOf('preroll') !== -1 || s.indexOf('ad-delivery') !== -1 || s.indexOf('/ads/') !== -1 || s.indexOf('imasdk') !== -1 || s.indexOf('cat-bolt') !== -1) return true;
                    return false;
                }
                function reportVid(v, title) {
                    if (!v || isShortCommercial(v)) return;
                    var s = v.currentSrc || v.src;
                    var t = title || document.title || 'Video Web';
                    if (s && s.indexOf('blob:') !== 0 && s.indexOf('data:') !== 0) {
                        console.log('$consolePrefix' + JSON.stringify({type:'videoPlaying', video:s, title:t}));
                    }
                }
                var vids = document.getElementsByTagName('video');
                for (var vi = 0; vi < vids.length; vi++) { reportVid(vids[vi]); }
                document.addEventListener('play', function(e) { if (e.target && e.target.tagName === 'VIDEO') reportVid(e.target); }, true);
                document.addEventListener('playing', function(e) { if (e.target && e.target.tagName === 'VIDEO') reportVid(e.target); }, true);

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
                            if (typeof val === 'string' && val.indexOf('blob:') !== 0 && val.indexOf('data:') !== 0 && isMediaStr(val)) {
                                console.log('$consolePrefix' + JSON.stringify({type:'videoPlaying', video:val, title:document.title}));
                            }
                            return origSet.call(this, val);
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
}
