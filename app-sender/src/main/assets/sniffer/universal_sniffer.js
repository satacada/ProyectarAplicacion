(function() {
    'use strict';
    if (window.__omniSnifferInjected) return;
    window.__omniSnifferInjected = true;

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
    // 1b. Intercepción segura de window.open contra popups publicitarios
    try {
        var origOpen = window.open;
        window.open = function(u, t, f) {
            if (!u || u === 'about:blank') return null;
            var l = (typeof u === 'string') ? u.toLowerCase() : '';
            if (l.indexOf('bet') !== -1 || l.indexOf('click') !== -1 || l.indexOf('track') !== -1 ||
                l.indexOf('pop') !== -1 || l.indexOf('ad') !== -1 || l.indexOf('redirect') !== -1 ||
                l.indexOf('traffic') !== -1 || l.indexOf('syndication') !== -1) {
                console.warn('OMNICAST_AD_POPUP_BLOCKED: ' + u);
                return null;
            }
            return origOpen ? origOpen.apply(window, arguments) : null;
        };
    } catch(e) {}

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
               l.indexOf('/live') !== -1 || l.indexOf('chunklist') !== -1 ||
               l.indexOf('p.php?url=') !== -1 || (l.indexOf('/p.php') !== -1 && l.indexOf('sig=') !== -1) ||
               ((l.indexOf('mycdn.me') !== -1 || l.indexOf('okcdn.ru') !== -1) && (l.indexOf('vd') !== -1 || l.indexOf('video') !== -1 || l.indexOf('cmd=video') !== -1 || l.indexOf('/cmd/') !== -1 || l.indexOf('sig=') !== -1)) ||
               (l.indexOf('master.txt') !== -1 && l.indexOf('/hls') !== -1);
    }

    function isCommercial(v) {
        if (!v) return false;
        var s = (v.currentSrc || v.src || '').toLowerCase();
        // 1. Descarte inmediato si la URL contiene patrones explícitos de anuncios
        if (s.indexOf('preroll') !== -1 || s.indexOf('ad-delivery') !== -1 ||
            s.indexOf('/ads/') !== -1 || s.indexOf('imasdk') !== -1 ||
            s.indexOf('cat-bolt') !== -1 || s.indexOf('ad.mp4') !== -1 || s.indexOf('vast') !== -1 ||
            s.indexOf('commercial') !== -1 || s.indexOf('spotx') !== -1 || s.indexOf('doubleclick') !== -1 ||
            s.indexOf('holdbitter') !== -1 || s.indexOf('/gambling/') !== -1 || s.indexOf('bonus-') !== -1) {
            return true;
        }
        // 2. Para cualquier video con duración finita menor a 60 segundos es un comercial (prerolls de 15s/18s/30s)
        var d = v.duration;
        if (typeof d === 'number' && !isNaN(d) && d > 0 && d < 60) return true;
        return false;
    }

    function reportMedia(src, title, duration, isPlaying) {
        if (!src || !isMediaUrl(src)) return;
        var dur = (typeof duration === 'number' && !isNaN(duration)) ? duration : 0;
        if (dur > 0 && dur < 60) return;
        var t = title || document.title || 'Transmisión Web';
        if (dur <= 0) {
            try {
                var vids = document.getElementsByTagName('video');
                for (var k = 0; k < vids.length; k++) {
                    if (vids[k].duration && !isNaN(vids[k].duration) && vids[k].duration > 0) {
                        if (vids[k].duration < 60) return;
                        dur = vids[k].duration;
                        break;
                    }
                }
            } catch(e) {}
        }
        console.log(PREFIX + JSON.stringify({
            type: isPlaying ? 'videoPlaying' : 'video',
            video: src,
            src: src,
            title: t,
            duration: dur,
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
            var s = v.currentSrc || v.src;
            if (s) reportMedia(s, document.title, v.duration || 0, playing);
        }

        v.addEventListener('loadedmetadata', function() { skipIfCommercial(); checkAndReport(false); }, true);
        v.addEventListener('durationchange', function() { skipIfCommercial(); checkAndReport(false); }, true);
        v.addEventListener('play', function() { skipIfCommercial(); checkAndReport(true); }, true);
        v.addEventListener('playing', function() { skipIfCommercial(); checkAndReport(true); }, true);
        v.addEventListener('timeupdate', function() { skipIfCommercial(); }, true);
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
    window.__omniActiveLiveStream = window.__omniActiveLiveStream || null;
    window.__omniCapturedMedia = window.__omniCapturedMedia || [];
    function recordCapturedMedia(u) {
        if (!u || typeof u !== 'string') return;
        if (!isMediaUrl(u)) return;
        if (window.__omniCapturedMedia.indexOf(u) === -1) {
            window.__omniCapturedMedia.unshift(u);
            if (window.__omniCapturedMedia.length > 25) window.__omniCapturedMedia.pop();
        }
    }
    function updateLiveStreamIfFresher(u) {
        if (!u || typeof u !== 'string') return;
        recordCapturedMedia(u);
        if (u.indexOf('.m3u8') !== -1 || u.indexOf('/hls') !== -1 || u.indexOf('.mpd') !== -1 || u.indexOf('p.php?url=') !== -1) {
            window.__omniActiveLiveStream = { url: u, timestamp: Date.now() };
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

    // Función global invocable antes de transmitir para obtener el stream activo con token vivo (0s de retraso)
    window.__omniGetLiveStream = function() {
        try {
            var vids = document.getElementsByTagName('video');
            for (var vi = 0; vi < vids.length; vi++) {
                var v = vids[vi];
                if (v && v.currentSrc && isMediaUrl(v.currentSrc)) {
                    return v.currentSrc;
                }
            }
            if (window.__omniActiveLiveStream && (Date.now() - window.__omniActiveLiveStream.timestamp < 180000)) {
                return window.__omniActiveLiveStream.url;
            }
            if (window.__omniCapturedMedia && window.__omniCapturedMedia.length > 0) {
                return window.__omniCapturedMedia[0];
            }
        } catch(e) {}
        return null;
    };

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

    // 7. Rescaneo periódico ligero de video
    setInterval(function() {
        var allV = document.getElementsByTagName('video');
        for (var j = 0; j < allV.length; j++) { monitorVideo(allV[j]); }
        sniffOkRuDom();
    }, 3000);

    // 8. Función global de reset y rescaneo profundo para el botón del trueno
    window.__omniResetAndRescan = function() {
        try {
            var vids = document.getElementsByTagName('video');
            for (var k = 0; k < vids.length; k++) {
                var v = vids[k];
                v.__omniMonitored = false;
                v.__omni = false;
                monitorVideo(v);
                var s = v.currentSrc || v.src;
                if (s && isMediaUrl(s)) {
                    reportMedia(s, document.title, v.duration || 0, !v.paused);
                }
            }
            if (window.__omniCapturedMedia && window.__omniCapturedMedia.length > 0) {
                for (var ci = 0; ci < window.__omniCapturedMedia.length; ci++) {
                    reportMedia(window.__omniCapturedMedia[ci], document.title, 0, false);
                }
            }
            if (typeof window.ibFindAllVideos === 'function') {
                window.ibFindAllVideos();
            }
            sniffOkRuDom();
        } catch(e) {}
    };

})();
