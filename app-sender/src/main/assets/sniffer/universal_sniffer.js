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
        window.open = function(u, t, f) {
            console.warn('OMNICAST_AD_POPUP_BLOCKED: ' + u);
            return null;
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
            s.indexOf('quickcore') !== -1 || s.indexOf('holdbitter') !== -1 || s.indexOf('/gambling/') !== -1 || s.indexOf('bonus-') !== -1) {
            return true;
        }
        // 2. Para cualquier video con duración finita menor a 60 segundos es un comercial (prerolls de 15s/18s/30s)
        var d = v.duration;
        if (typeof d === 'number' && !isNaN(d) && d > 0 && d < 60) return true;
        return false;
    }

    function reportMedia(src, title, duration, isPlaying, label) {
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
            label: label || '',
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
            try {
                var sources = v.getElementsByTagName('source');
                for (var si = 0; si < sources.length; si++) {
                    if (sources[si].src) reportMedia(sources[si].src, document.title, v.duration || 0, false);
                }
                var dSrc = v.getAttribute('data-src');
                if (dSrc) reportMedia(dSrc, document.title, v.duration || 0, false);
            } catch(_) {}
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
                try {
                    this.addEventListener('load', function() {
                        try {
                            if (this.responseText) {
                                inspectStreamingPatterns(this.responseText, document.title);
                            }
                        } catch(_) {}
                    });
                } catch(_) {}
                return origOpen.apply(this, arguments);
            };
        }
    } catch(e) {}

    // Detección pura por comportamiento y estado del reproductor (Cero nombres de dominio en duro)
    function hasActiveDirectStream() {
        return (window.__omniCapturedMedia && window.__omniCapturedMedia.length > 0) ||
               (window.__omniActiveLiveStream && (Date.now() - window.__omniActiveLiveStream.timestamp < 180000));
    }

    function hasBlobOrMseVideo() {
        try {
            var vids = document.getElementsByTagName('video');
            for (var i = 0; i < vids.length; i++) {
                var s = vids[i].currentSrc || vids[i].src || '';
                if (s.indexOf('blob:') === 0) return true;
            }
            if (typeof window.MediaSource !== 'undefined') {
                if (window.OneVideoPlayer || window.Clappr || window.jwplayer) return true;
            }
        } catch(_) {}
        return false;
    }

    function inspectStreamingPatterns(responseText, defaultTitle) {
        if (!responseText || typeof responseText !== 'string' || responseText.length < 15) return;
        
        // Regla 1: Si ya existe un flujo directo capturado por red (PelisPlus, PelisGO, Reypelis), NO interferir.
        if (hasActiveDirectStream()) return;

        // Regla 2: Solo inspeccionar payload si el reproductor está operando con MSE / Blob en memoria
        if (!hasBlobOrMseVideo()) return;

        try {
            // A. Manifiesto HLS dentro de respuestas JSON o de configuración
            if (responseText.indexOf('.m3u8') !== -1) {
                var matchHls = responseText.match(/https?:\/\/[^"'\s\\><]+\.m3u8[^"'\s\\><]*/i);
                if (matchHls) {
                    var cleanHls = matchHls[0].replace(/\\/g, '');
                    // Excluir pistas secundarias de idioma/subtítulos
                    if (cleanHls.indexOf('/lang/') === -1 && cleanHls.indexOf('/subs/') === -1 && cleanHls.indexOf('.vtt') === -1) {
                        if (isMediaUrl(cleanHls)) {
                            reportMedia(cleanHls, defaultTitle || document.title, 0, false, "Auto (Adaptativo)");
                            return;
                        }
                    }
                }
            }

            // B. Metadatos estructurados de reproductor (hlsManifestUrl / flashvars / videos)
            if (responseText.indexOf('hlsManifestUrl') !== -1 || (responseText.indexOf('flashvars') !== -1 && responseText.indexOf('metadata') !== -1)) {
                var pj = JSON.parse(responseText);
                var meta = (pj.flashvars && pj.flashvars.metadata) ? pj.flashvars.metadata : (pj.metadata || pj.video || pj);
                parseOkRuMetadata(meta, defaultTitle);
                return;
            }

            // C. Estructura estándar de Vimeo / reproductores basados en configuración JSON
            if (responseText.indexOf('request') !== -1 && responseText.indexOf('files') !== -1 && responseText.indexOf('hls') !== -1) {
                var vj = JSON.parse(responseText);
                var hlsCdn = vj.request && vj.request.files && vj.request.files.hls && vj.request.files.hls.cdns;
                if (hlsCdn) {
                    for (var c in hlsCdn) {
                        if (hlsCdn[c] && hlsCdn[c].url && isMediaUrl(hlsCdn[c].url)) {
                            reportMedia(hlsCdn[c].url, (vj.video && vj.video.title) || defaultTitle || document.title, 0, false, "Auto (HLS)");
                            return;
                        }
                    }
                }
            }
        } catch(_) {}
    }

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

    // 6. Extractor específico para reproductores MSE (OneVideoPlayer, DOM data-options, scripts inline y calidades)
    function mapOkRuQuality(name) {
        if (!name) return 'Calidad Original';
        var n = String(name).toLowerCase().trim();
        if (n === 'ultra' || n === '2160' || n === '4k') return '2160p 4K';
        if (n === 'quad' || n === '1440' || n === '2k') return '1440p 2K';
        if (n === 'full' || n === '1080' || n === '1080p') return '1080p FHD';
        if (n === 'hd' || n === '720' || n === '720p') return '720p HD';
        if (n === 'sd' || n === '480' || n === '480p') return '480p SD';
        if (n === 'low' || n === '360' || n === '360p') return '360p';
        if (n === 'lowest' || n === '240' || n === '240p') return '240p';
        if (n === 'mobile' || n === '144' || n === '144p') return '144p Móvil';
        return n;
    }

    window.__reportedOkRuStreams = window.__reportedOkRuStreams || {};
    function parseOkRuMetadata(meta, defaultTitle) {
        if (!meta) return;
        if (typeof meta === 'string') {
            try { meta = JSON.parse(meta); } catch(e) { return; }
        }
        var mId = (meta.movie && meta.movie.id) ? String(meta.movie.id) : (document.title || 'msevid');
        var t = (meta.movie && meta.movie.title) ? meta.movie.title : (defaultTitle || document.title);
        var dur = (meta.movie && meta.movie.duration) ? meta.movie.duration : 0;
        var hlsKey = mId + '_hls';
        if (meta.hlsManifestUrl && !window.__reportedOkRuStreams[hlsKey]) {
            window.__reportedOkRuStreams[hlsKey] = true;
            reportMedia(meta.hlsManifestUrl, t, dur, false, 'Auto (Adaptativo)');
        }
        if (meta.videos && meta.videos.length > 0) {
            for (var v = 0; v < meta.videos.length; v++) {
                var vidItem = meta.videos[v];
                var qKey = mId + '_' + (vidItem.name || v);
                if (vidItem && vidItem.url && !window.__reportedOkRuStreams[qKey]) {
                    window.__reportedOkRuStreams[qKey] = true;
                    reportMedia(vidItem.url, t, dur, false, mapOkRuQuality(vidItem.name));
                }
            }
        }
    }

    function sniffMsePlayerDom() {
        try {
            // A. Instancia OneVideoPlayer u objetos globales de reproductor
            if (typeof OneVideoPlayer === 'object' && OneVideoPlayer.getInstance) {
                try {
                    var ov = OneVideoPlayer.getInstance();
                    if (ov && ov.options && ov.options.flashvars && ov.options.flashvars.metadata) {
                        parseOkRuMetadata(ov.options.flashvars.metadata, ov.options.title);
                    }
                } catch(_) {}
            }
            // B. Elementos del DOM con metadata
            var elms = document.querySelectorAll('[data-options*="metadata"], [data-module="OKVideo"], div.vp-layer, div[data-video], [data-options*="video"], [data-player]');
            for (var i = 0; i < elms.length; i++) {
                var raw = elms[i].getAttribute('data-options') || elms[i].getAttribute('data-video');
                if (!raw) continue;
                try {
                    var p = JSON.parse(raw);
                    var meta = (p.flashvars && p.flashvars.metadata) ? p.flashvars.metadata : (p.metadata || p.video || null);
                    parseOkRuMetadata(meta, p.title);
                } catch(e) {}
            }
            // C. Globales de OKVideo
            if (window.OKVideo && window.OKVideo.metadata) {
                parseOkRuMetadata(window.OKVideo.metadata);
            }
            // D. Scripts inline cuando el video usa blob:
            if (hasBlobOrMseVideo()) {
                var scrs = document.querySelectorAll('script');
                for (var si = 0; si < scrs.length; si++) {
                    var txt = scrs[si].textContent || '';
                    if (txt.indexOf('hlsManifestUrl') !== -1 && txt.indexOf('videos') !== -1) {
                        var match = txt.match(/\{[\s\S]*?"hlsManifestUrl"[\s\S]*?\}/);
                        if (match) {
                            try { parseOkRuMetadata(JSON.parse(match[0])); } catch(_) {}
                        }
                    }
                }
            }
        } catch(e) {}
    }
    sniffMsePlayerDom();
    setTimeout(sniffMsePlayerDom, 1200);
    setTimeout(sniffMsePlayerDom, 3000);
    document.addEventListener('click', function() {
        if (hasBlobOrMseVideo()) {
            setTimeout(sniffMsePlayerDom, 800);
            setTimeout(sniffMsePlayerDom, 2000);
        }
    }, true);

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
        sniffMsePlayerDom();
        sniffPlayerApis();
    }, 3000);

    // 7b. Observación reactiva limpia de Cloudflare Turnstile (Cero clicks sintéticos que activen bot heuristics)
    function checkTurnstileCompleted() {
        try {
            var cfInp = document.querySelector('input[name="cf-turnstile-response"]');
            if (cfInp && cfInp.value && !window.__omniTurnstileSubmitted) {
                window.__omniTurnstileSubmitted = true;
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
            sniffMsePlayerDom();
        } catch(e) {}
    };

})();
