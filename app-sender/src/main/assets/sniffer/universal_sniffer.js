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
    } catch(e) {}

    // 2. Filtro de URLs de medios
    function isMediaUrl(u) {
        if (!u || typeof u !== 'string') return false;
        var l = u.toLowerCase();
        if (l.indexOf('.jpg') !== -1 || l.indexOf('.png') !== -1 || l.indexOf('.webp') !== -1 ||
            l.indexOf('.gif') !== -1 || l.indexOf('.css') !== -1 || l.indexOf('.js') !== -1 ||
            l.indexOf('i.mycdn.me') !== -1 || l.indexOf('st.mycdn.me') !== -1) return false;
        if (l.indexOf('/embed/') !== -1 || l.indexOf('/embed69') !== -1 || l.indexOf('/vidurl/') !== -1) return false;
        if (l.indexOf('init.mp4') !== -1 || l.indexOf('/init-') !== -1 || l.indexOf('.m4s') !== -1) return false;
        if (l.indexOf('.ts') !== -1 && l.indexOf('.m3u8') === -1) return false;
        return l.indexOf('.m3u8') !== -1 || l.indexOf('.mp4') !== -1 || l.indexOf('.mpd') !== -1 ||
               l.indexOf('/hls') !== -1 || l.indexOf('/stream/') !== -1 || l.indexOf('/manifest') !== -1 ||
               l.indexOf('/live') !== -1 || l.indexOf('chunklist') !== -1 ||
               (l.indexOf('mycdn.me') !== -1 && (l.indexOf('vd') !== -1 || l.indexOf('video') !== -1 || l.indexOf('cmd=video') !== -1 || l.indexOf('/cmd/') !== -1 || l.indexOf('sig=') !== -1)) ||
               (l.indexOf('master.txt') !== -1 && l.indexOf('/hls') !== -1);
    }

    function isCommercial(v) {
        if (!v) return false;
        var s = (v.currentSrc || v.src || '').toLowerCase();
        // 1. Descarte inmediato si la URL contiene patrones explícitos de anuncios
        if (s.indexOf('preroll') !== -1 || s.indexOf('ad-delivery') !== -1 ||
            s.indexOf('/ads/') !== -1 || s.indexOf('imasdk') !== -1 ||
            s.indexOf('cat-bolt') !== -1 || s.indexOf('ad.mp4') !== -1 || s.indexOf('vast') !== -1) {
            return true;
        }
        // 2. Transmisiones en vivo o adaptativas (.m3u8, .mpd, chunklist, live) NUNCA son anuncios cortos
        if (s.indexOf('.m3u8') !== -1 || s.indexOf('.mpd') !== -1 || s.indexOf('chunklist') !== -1 || s.indexOf('/live') !== -1) {
            return false;
        }
        // 3. Para archivos MP4 o clips simples: si dura menos de 60 segundos es comercial
        var d = v.duration;
        if (typeof d === 'number' && !isNaN(d) && d > 0 && d < 60) return true;
        return false;
    }

    function reportMedia(src, title, duration, isPlaying) {
        if (!src || !isMediaUrl(src)) return;
        var t = title || document.title || 'Transmisión Web';
        var dur = duration || 0;
        console.log(PREFIX + JSON.stringify({
            type: isPlaying ? 'videoPlaying' : 'video',
            video: src,
            src: src,
            title: t,
            duration: dur
        }));
    }

    // 3. Captura Universal de etiquetas <video>
    function monitorVideo(v) {
        if (!v || v.__omniMonitored) return;
        v.__omniMonitored = true;

        function checkAndReport(playing) {
            if (isCommercial(v)) return;
            var s = v.currentSrc || v.src;
            if (s) reportMedia(s, document.title, v.duration || 0, playing);
        }

        v.addEventListener('loadedmetadata', function() { checkAndReport(false); }, true);
        v.addEventListener('durationchange', function() { checkAndReport(false); }, true);
        v.addEventListener('play', function() { checkAndReport(true); }, true);
        v.addEventListener('playing', function() { checkAndReport(true); }, true);
        if (v.readyState >= 1) checkAndReport(!v.paused);
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

    // 5. Intercepción de Fetch y XHR
    try {
        var origFetch = window.fetch;
        if (origFetch) {
            window.fetch = function() {
                var a = arguments[0];
                var u = (typeof a === 'string') ? a : (a && a.url ? a.url : '');
                if (isMediaUrl(u)) reportMedia(u, document.title, 0, false);
                return origFetch.apply(this, arguments);
            };
        }
    } catch(e) {}

    try {
        var origOpen = XMLHttpRequest.prototype.open;
        if (origOpen) {
            XMLHttpRequest.prototype.open = function(m, u) {
                if (isMediaUrl(u)) reportMedia(u, document.title, 0, false);
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

    // 7. Rescaneo periódico ligero de video
    setInterval(function() {
        var allV = document.getElementsByTagName('video');
        for (var j = 0; j < allV.length; j++) { monitorVideo(allV[j]); }
        sniffOkRuDom();
    }, 3000);

})();
