(function() {
    'use strict';
    var _sniffKey = typeof Symbol !== 'undefined' ? Symbol.for('_media_sensor_') : null;
    if (_sniffKey) {
        if (window[_sniffKey]) return;
        try {
            Object.defineProperty(window, _sniffKey, { value: true, enumerable: false, configurable: false, writable: false });
        } catch(_) { window[_sniffKey] = true; }
    }

    var PREFIX = 'OMNICAST_STREAM:';
    window.__blobStore = window.__blobStore || {};

    // 1. Preservación de Blobs (Hook window.URL.revokeObjectURL de Seekee)
    try {
        var origRevoke = window.URL.revokeObjectURL;
        if (origRevoke) {
            window.URL.revokeObjectURL = function(blobUrl) {
                if (typeof blobUrl === 'string' && blobUrl.indexOf('blob:') === 0) {
                    window.__blobStore[blobUrl] = { url: blobUrl, time: Date.now() };
                    setTimeout(function() {
                        try { origRevoke.call(window.URL, blobUrl); } catch(e) {}
                    }, 15000);
                } else {
                    origRevoke.call(window.URL, blobUrl);
                }
            };
        }
    } catch(e) {}

    // 1b. Gestión de window.open delegada canónicamente en WvcShieldSecurityEngine.kt (HU-007/ADR-006)

    // 2. Filtro de URLs de medios
    function isMediaUrl(u) {
        if (!u || typeof u !== 'string') return false;
        var l = u.toLowerCase();
        if (l.indexOf('.jpg') !== -1 || l.indexOf('.png') !== -1 || l.indexOf('.webp') !== -1 ||
            l.indexOf('.gif') !== -1 || l.indexOf('.css') !== -1 || l.indexOf('.js') !== -1 ||
            l.indexOf('i.mycdn.me') !== -1 || l.indexOf('st.mycdn.me') !== -1) return false;
        if (l.indexOf('stub.mp4') !== -1 || l.indexOf('dummy.mp4') !== -1 || l.indexOf('blank.mp4') !== -1) return false;
        if (l.indexOf('/embed/') !== -1 || l.indexOf('/embed69') !== -1 || l.indexOf('/vidurl/') !== -1) return false;
        if (l.indexOf('init.mp4') !== -1 || l.indexOf('/init-') !== -1 || l.indexOf('.m4s') !== -1) return false;
        if (l.indexOf('.ts') !== -1 && l.indexOf('.m3u8') === -1) return false;
        if (l.indexOf('holdbitter') !== -1 || l.indexOf('/gambling/') !== -1 || l.indexOf('bonus-') !== -1 || l.indexOf('bonus_') !== -1) return false;
        return l.indexOf('.m3u8') !== -1 || l.indexOf('.mp4') !== -1 || l.indexOf('.mpd') !== -1 ||
               l.indexOf('/hls') !== -1 || l.indexOf('/stream/') !== -1 || l.indexOf('/manifest') !== -1 ||
               l.indexOf('doodcdn') !== -1 || l.indexOf('doopstream') !== -1 ||
               l.indexOf('doodstream') !== -1 || l.indexOf('/pass/') !== -1 ||
               l.indexOf('/live') !== -1 || l.indexOf('chunklist') !== -1 ||
               l.indexOf('.php?url=') !== -1 || l.indexOf('p.php?url=') !== -1 || (l.indexOf('/p.php') !== -1 && l.indexOf('sig=') !== -1) ||
               ((l.indexOf('mycdn.me') !== -1 || l.indexOf('okcdn.ru') !== -1) && (l.indexOf('vd') !== -1 || l.indexOf('video') !== -1 || l.indexOf('cmd=video') !== -1 || l.indexOf('/cmd/') !== -1 || l.indexOf('sig=') !== -1)) ||
               l.indexOf('master.txt') !== -1 || l.indexOf('.txt?hls') !== -1 || (l.indexOf('.txt') !== -1 && l.indexOf('hls') !== -1);
    }

    function isCommercial(v) {
        if (!v) return false;
        var s = (v.currentSrc || v.src || '').toLowerCase();
        // 1. Descarte inmediato si la URL contiene patrones explícitos de anuncios
        if (s.indexOf('preroll') !== -1 || s.indexOf('ad-delivery') !== -1 ||
            s.indexOf('/ads/') !== -1 || s.indexOf('imasdk') !== -1 ||
            s.indexOf('cat-bolt') !== -1 || s.indexOf('ad.mp4') !== -1 || s.indexOf('vast') !== -1 ||
            s.indexOf('commercial') !== -1 || s.indexOf('spotx') !== -1 || s.indexOf('doubleclick') !== -1 ||
            s.indexOf('quickcore') !== -1 || s.indexOf('holdbitter') !== -1 || s.indexOf('/gambling/') !== -1 || s.indexOf('bonus-') !== -1) {
            return true;
        }
        // 2. Para cualquier video con duración finita menor a 60 segundos es un comercial (prerolls de 15s/18s/30s)
        var d = v.duration;
        if (typeof d === 'number' && !isNaN(d) && d > 0 && d < 60) return true;
        return false;
    }

    function reportMedia(v, explicitUrl, duration, isPlaying, label) {
        var src = '';
        var t = '';
        var dur = 0;
        var playing = false;
        var lbl = '';

        if (v && typeof v === 'object' && (v.nodeName === 'VIDEO' || v.tagName === 'VIDEO' || typeof v.duration !== 'undefined')) {
            src = explicitUrl || v.currentSrc || v.src || '';
            dur = v.duration;
            t = (typeof duration === 'string' && duration) ? duration : (document.title || 'Transmisión Web');
            playing = (typeof isPlaying === 'boolean') ? isPlaying : !v.paused;
            lbl = label || '';
        } else {
            src = (typeof v === 'string') ? v : (explicitUrl || '');
            t = (typeof explicitUrl === 'string' && explicitUrl && typeof v === 'string') ? explicitUrl : (document.title || 'Transmisión Web');
            dur = duration;
            playing = !!isPlaying;
            lbl = label || '';
        }

        if (!src || !isMediaUrl(src)) return;

        var uLower = src.toLowerCase();
        var isLiveStream = uLower.indexOf('.m3u8') !== -1 ||
                           uLower.indexOf('/live') !== -1 ||
                           uLower.indexOf('chunklist') !== -1 ||
                           uLower.indexOf('master') !== -1 ||
                           uLower.indexOf('hls') !== -1;

        if (dur === 0 || isNaN(dur) || dur === Infinity || !isFinite(dur)) {
            if (isLiveStream) {
                dur = 0;
            }
        }

        if (typeof dur === 'number' && !isNaN(dur) && dur > 0 && dur < 60 && !isLiveStream) {
            return;
        }

        var finalDur = (typeof dur === 'number' && !isNaN(dur) && isFinite(dur) && dur > 0) ? dur : 0;

        console.log(PREFIX + JSON.stringify({
            type: playing ? 'videoPlaying' : 'video',
            video: src,
            src: src,
            title: t,
            label: lbl,
            duration: finalDur,
            isLive: isLiveStream,
            frameUrl: window.location.href || ''
        }));
    }

    // 3. Captura Universal de etiquetas <video>
    function monitorVideo(v) {
        if (!v || v.__omniMonitored) return;
        v.__omniMonitored = true;

        function skipIfCommercial() {
            if (isCommercial(v)) {
                try {
                    v.muted = true;
                    if (v.duration && !isNaN(v.duration) && v.duration > 0 && v.duration < 60) {
                        v.currentTime = v.duration;
                    }
                } catch(_) {}
            }
        }

        function checkAndReport(playing) {
            if (isCommercial(v)) { skipIfCommercial(); return; }
            var isYouTube = (window.location.hostname || '').indexOf('youtube.com') !== -1;
            if (isYouTube) return;
            var s = v.currentSrc || v.src;
            if (s && s.indexOf('blob:') !== 0 && s.indexOf('data:') !== 0) {
                reportMedia(v, s, document.title, playing);
            }
            try {
                var sources = v.getElementsByTagName('source');
                for (var si = 0; si < sources.length; si++) {
                    if (sources[si].src) reportMedia(v, sources[si].src, document.title, false);
                }
                var dSrc = v.getAttribute('data-src');
                if (dSrc) reportMedia(v, dSrc, document.title, false);
            } catch(_) {}
        }

        v.addEventListener('loadedmetadata', function() { skipIfCommercial(); checkAndReport(false); }, true);
        v.addEventListener('durationchange', function() { skipIfCommercial(); checkAndReport(false); }, true);
        v.addEventListener('play', function() { skipIfCommercial(); checkAndReport(true); }, true);
        v.addEventListener('playing', function() { skipIfCommercial(); checkAndReport(true); }, true);
        v.addEventListener('timeupdate', function() { skipIfCommercial(); }, true);
        v.addEventListener('error', function() {
            var err = v.error;
            console.warn('OMNI_DOM_VIDEO_ERROR:' + JSON.stringify({
                code: err ? err.code : 0,
                message: err ? (err.message || '') : '',
                src: v.currentSrc || v.src || '',
                page: window.location.href || ''
            }));
        }, true);
        v.addEventListener('stalled', function() {
            console.warn('OMNI_DOM_VIDEO_STALLED:' + JSON.stringify({
                src: v.currentSrc || v.src || '',
                page: window.location.href || ''
            }));
        }, true);
        if (v.readyState >= 1) { skipIfCommercial(); checkAndReport(!v.paused); }
    }

    var vids = document.getElementsByTagName('video');
    for (var i = 0; i < vids.length; i++) { monitorVideo(vids[i]); }
    document.addEventListener('play', function(e) { if (e.target && e.target.tagName === 'VIDEO') monitorVideo(e.target); }, true);
    document.addEventListener('playing', function(e) { if (e.target && e.target.tagName === 'VIDEO') monitorVideo(e.target); }, true);

    // 4. Intercepción de HTMLMediaElement.src
    try {
        var origSrc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
        if (origSrc && origSrc.set) {
            Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                set: function(val) {
                    var res = origSrc.set.call(this, val);
                    if (typeof val === 'string' && isMediaUrl(val)) monitorVideo(this);
                    return res;
                }
            });
        }
    } catch(e) {}

    // 5. Intercepción de Fetch y XHR con registro de Token Fresco en Tiempo Real
    var activeLiveStream = null;
    var capturedMedia = [];
    function recordCapturedMedia(u) {
        if (!u || typeof u !== 'string') return;
        if (!isMediaUrl(u)) return;
        if (capturedMedia.indexOf(u) === -1) {
            capturedMedia.unshift(u);
            if (capturedMedia.length > 25) capturedMedia.pop();
        }
    }
    function updateLiveStreamIfFresher(u) {
        if (!u || typeof u !== 'string') return;
        recordCapturedMedia(u);
        if (u.indexOf('.m3u8') !== -1 || u.indexOf('/hls') !== -1 || u.indexOf('.mpd') !== -1 || u.indexOf('p.php?url=') !== -1) {
            activeLiveStream = { url: u, timestamp: Date.now() };
        }
    }

    try {
        var origFetch = window.fetch;
        if (origFetch) {
            window.fetch = function() {
                var a = arguments[0];
                var u = (typeof a === 'string') ? a : (a && a.url ? a.url : '');
                if (isMediaUrl(u)) {
                    updateLiveStreamIfFresher(u);
                    reportMedia(u, document.title, 0, false);
                }
                return origFetch.apply(this, arguments);
            };
        }
    } catch(e) {}

    try {
        var origOpen = XMLHttpRequest.prototype.open;
        if (origOpen) {
            XMLHttpRequest.prototype.open = function(m, u) {
                if (isMediaUrl(u)) {
                    updateLiveStreamIfFresher(u);
                    reportMedia(u, document.title, 0, false);
                }
                return origOpen.apply(this, arguments);
            };
        }
    } catch(e) {}

    // 6. Extractor DOM específico para OK.ru (Metadata JSON en elemento reproductor)
    function sniffOkRuDom() {
        try {
            var elms = document.querySelectorAll('[data-options*="metadata"], [data-module="OKVideo"], div.vp-layer');
            for (var i = 0; i < elms.length; i++) {
                var raw = elms[i].getAttribute('data-options');
                if (!raw) continue;
                try {
                    var p = JSON.parse(raw);
                    var meta = (p.flashvars && p.flashvars.metadata) ? p.flashvars.metadata : null;
                    if (typeof meta === 'string') { try { meta = JSON.parse(meta); } catch(e) {} }
                    if (meta) {
                        var t = (meta.movie && meta.movie.title) ? meta.movie.title : document.title;
                        if (meta.hlsManifestUrl) {
                            reportMedia(meta.hlsManifestUrl, t, 0, false);
                        }
                        if (meta.videos && meta.videos.length > 0) {
                            for (var v = 0; v < meta.videos.length; v++) {
                                if (meta.videos[v].url) {
                                    reportMedia(meta.videos[v].url, t, 0, false);
                                }
                            }
                        }
                    }
                } catch(e) {}
            }
        } catch(e) {}
    }
    sniffOkRuDom();
    setTimeout(sniffOkRuDom, 1500);
    setTimeout(sniffOkRuDom, 3500);

    // 6b. Extractor DOM para APIs de reproductores web (JWPlayer, Clappr, Flowplayer) estilo WVC
    function sniffPlayerApis() {
        try {
            if (typeof window.jwplayer === 'function') {
                var pls = window.jwplayer().getPlaylist();
                if (pls && pls.length > 0) {
                    for (var p = 0; p < pls.length; p++) {
                        var item = pls[p];
                        var itemTitle = item.title || document.title;
                        var srcs = item.sources || [];
                        for (var s = 0; s < srcs.length; s++) {
                            if (srcs[s].file && isMediaUrl(srcs[s].file)) {
                                reportMedia(srcs[s].file, itemTitle, 0, false, srcs[s].label || ('Opción ' + (s + 1)));
                            }
                        }
                        if (item.file && isMediaUrl(item.file)) {
                            reportMedia(item.file, itemTitle, 0, false, 'Master');
                        }
                    }
                }
            }
            if (typeof window.Clappr !== 'undefined' && window.Clappr.PlayerInfo && window.Clappr.PlayerInfo._players) {
                var cp = window.Clappr.PlayerInfo._players;
                for (var k in cp) {
                    if (cp[k] && cp[k].options && cp[k].options.source && isMediaUrl(cp[k].options.source)) {
                        reportMedia(cp[k].options.source, document.title, 0, false);
                    }
                }
            }
        } catch(_) {}
    }
    sniffPlayerApis();
    setTimeout(sniffPlayerApis, 2000);

    // 7. Rescaneo periódico ligero de video
    setInterval(function() {
        var allV = document.getElementsByTagName('video');
        for (var j = 0; j < allV.length; j++) { monitorVideo(allV[j]); }
        sniffOkRuDom();
        sniffPlayerApis();
    }, 3000);

    // 7b. Observación reactiva limpia de Cloudflare Turnstile (Cero clicks sintéticos que activen bot heuristics)
    var turnstileSubmitted = false;
    function checkTurnstileCompleted() {
        try {
            var cfInp = document.querySelector('input[name="cf-turnstile-response"]');
            if (cfInp && cfInp.value && !turnstileSubmitted) {
                turnstileSubmitted = true;
                if (typeof window.imNotARobot === 'function') {
                    window.imNotARobot();
                } else if (typeof window.onSubmit === 'function') {
                    window.onSubmit(cfInp.value);
                } else {
                    var f = document.getElementById('F1') || cfInp.closest('form');
                    if (f && typeof f.submit === 'function') f.submit();
                }
            }
        } catch(_) {}
    }
    document.addEventListener('DOMContentLoaded', checkTurnstileCompleted);
    window.addEventListener('load', checkTurnstileCompleted);
    setInterval(checkTurnstileCompleted, 1000);

    // 8. Intercepción reactiva de navegación SPA (YouTube Polymer, pushState, popstate, yt-navigate-finish)
    function checkSpaNavigation() {
        try {
            var href = window.location.href;
            if (href.indexOf('watch?v=') !== -1 || href.indexOf('/live/') !== -1 || href.indexOf('youtu.be/') !== -1) {
                console.log(PREFIX + JSON.stringify({
                    type: 'videoPlaying',
                    video: href,
                    src: href,
                    title: document.title || 'Video YouTube',
                    duration: 0,
                    frameUrl: href
                }));
            }
        } catch(_) {}
    }
    try {
        var origPushState = window.history.pushState;
        if (origPushState) {
            window.history.pushState = function() {
                var res = origPushState.apply(this, arguments);
                setTimeout(checkSpaNavigation, 150);
                return res;
            };
        }
        var origReplaceState = window.history.replaceState;
        if (origReplaceState) {
            window.history.replaceState = function() {
                var res = origReplaceState.apply(this, arguments);
                setTimeout(checkSpaNavigation, 150);
                return res;
            };
        }
        window.addEventListener('popstate', checkSpaNavigation);
        document.addEventListener('yt-navigate-finish', checkSpaNavigation);
        checkSpaNavigation();
    } catch(_) {}

})();
