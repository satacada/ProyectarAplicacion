package com.omnicast.receiver.ui.components

/**
 * Generador HTML / JavaScript para el reproductor web de la TV Box.
 * Soporta control de reproducción, zoom TV, cambio de resolución HLS y alternancia de subtítulos.
 */
object TvWebPlayerHtmlBuilder {

    fun build(hlsCode: String, streamUrl: String, startPositionMs: Long, subtitles: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()): String {
        val safeUrl = streamUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        val startSec = (startPositionMs / 1000.0).coerceAtLeast(0.0)
        val tracksHtml = subtitles.joinToString("") { sub ->
            val lang = (sub.lang ?: "es").replace("\"", "&quot;")
            val safeSubUrl = sub.url.replace("\"", "&quot;")
            """<track src="$safeSubUrl" label="$lang" kind="subtitles" srclang="$lang">"""
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
              * { margin:0; padding:0; box-sizing:border-box; }
              html, body { width:100vw; height:100vh; overflow:hidden; background:#000; position:relative; }
              video { width:100vw; height:100vh; object-fit:contain; background:#000; border:none; outline:none; transition:transform 0.2s ease, object-fit 0.2s ease; }
              video::-webkit-media-controls-overlay-play-button { display:none !important; opacity:0 !important; pointer-events:none !important; width:0 !important; height:0 !important; }
              video::-webkit-media-controls-start-playback-button { display:none !important; opacity:0 !important; pointer-events:none !important; width:0 !important; height:0 !important; }
              video::-webkit-media-controls-overlay-enclosure { display:none !important; }
            </style>
            <script>$hlsCode</script>
            </head>
            <body>
              <video id="vplayer" autoplay playsinline controls>$tracksHtml</video>
              <script>
                var vid = document.getElementById('vplayer');
                var streamUrl = "$safeUrl", startSec = $startSec, hasResumed = false;
                var hlsInstance = null, watchdogTimer = null;

                function clearWatchdog() { if (watchdogTimer) { clearTimeout(watchdogTimer); watchdogTimer = null; } }
                function resetWatchdog(ms) {
                  clearWatchdog();
                  watchdogTimer = setTimeout(function() {
                    if (vid && (vid.currentTime === 0 || vid.paused) && vid.readyState < 3) {
                      tryPlay();
                    }
                  }, ms || 15000);
                }
                resetWatchdog(15000);

                function hideSpin() {}
                function showSpin() {}
                function hideCurtain() {}
                function tryPlay() {
                  if (!vid) return;
                  var p = vid.play();
                  if (p !== undefined) {
                    p.then(function() { clearWatchdog(); hideSpin(); hideCurtain(); }).catch(function() {
                      vid.muted = true;
                      vid.play().then(function() { setTimeout(function() { if (vid) vid.muted = false; clearWatchdog(); hideSpin(); hideCurtain(); }, 250); }).catch(function(){});
                    });
                  }
                }
                function reportStatus() {
                  if (vid && vid.currentTime > 0 && !vid.paused) clearWatchdog();
                  if (window.AndroidBridge && vid) {
                    window.AndroidBridge.onPlaybackStatus(vid.currentTime || 0, vid.duration || 0, !vid.paused && !vid.ended && vid.readyState > 2);
                  }
                }
                vid.addEventListener('timeupdate', reportStatus); vid.addEventListener('play', function() { hideSpin(); hideCurtain(); reportStatus(); });
                vid.addEventListener('playing', function() { clearWatchdog(); hideSpin(); hideCurtain(); }); vid.addEventListener('waiting', showSpin); vid.addEventListener('seeking', showSpin); vid.addEventListener('seeked', hideSpin);
                vid.addEventListener('pause', reportStatus); vid.addEventListener('ended', reportStatus);
                function seekStart() { if (startSec > 0 && !hasResumed && vid) { hasResumed = true; vid.currentTime = startSec; } }
                vid.addEventListener('loadedmetadata', function() { seekStart(); tryPlay(); }); vid.addEventListener('canplay', function() { seekStart(); tryPlay(); });
                var hasTriedHls = false;
                vid.addEventListener('error', function() {
                  hideCurtain();
                  if (!hasTriedHls && window.Hls && Hls.isSupported() && streamUrl.indexOf('.mp4') === -1) {
                    initHls(); return;
                  }
                  clearWatchdog();
                  var c = vid.error ? vid.error.code : 0;
                  if (window.AndroidBridge && window.AndroidBridge.onPlaybackFailed) window.AndroidBridge.onPlaybackFailed('VID_ERR_' + c);
                });

                function initHls() {
                  hasTriedHls = true;
                  if (window.Hls && Hls.isSupported()) {
                    var hls = new Hls({ enableWorker: true, lowLatencyMode: false, backBufferLength: 30, maxBufferLength: 30, maxMaxBufferLength: 60, maxBufferSize: 30 * 1000 * 1000, startFragPrefetch: true });
                    hlsInstance = hls;
                    hls.loadSource(streamUrl);
                    hls.attachMedia(vid);
                    hls.on(Hls.Events.MANIFEST_PARSED, function() { resetWatchdog(12000); tryPlay(); });
                    hls.on(Hls.Events.LEVEL_LOADED, function() { resetWatchdog(12000); tryPlay(); });
                    hls.on(Hls.Events.FRAG_LOADING, function() { resetWatchdog(15000); });
                    hls.on(Hls.Events.FRAG_LOADED, function() { resetWatchdog(12000); });
                    hls.on(Hls.Events.FRAG_PARSED, function() { tryPlay(); });
                    var hlsNetRetries = 0;
                    hls.on(Hls.Events.ERROR, function(ev, data) {
                      if (data.fatal) {
                        hideCurtain();
                        if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
                          if (++hlsNetRetries <= 1) { resetWatchdog(10000); hls.startLoad(); }
                          else {
                            try { hls.destroy(); } catch(_) {}
                            vid.src = streamUrl;
                            tryPlay();
                          }
                        } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
                        else {
                          try { hls.destroy(); } catch(_) {}
                          vid.src = streamUrl;
                          tryPlay();
                        }
                      }
                    });
                  } else {
                    vid.src = streamUrl; tryPlay();
                  }
                }

                var isOkMp4 = (streamUrl.indexOf('mycdn.me') !== -1 || streamUrl.indexOf('okcdn.ru') !== -1) && streamUrl.indexOf('/hls') === -1 && streamUrl.indexOf('m3u8') === -1;
                var isDirectVideo = streamUrl.indexOf('.mp4') !== -1 || streamUrl.indexOf('.webm') !== -1 || isOkMp4;
                if (!isDirectVideo && window.Hls && Hls.isSupported()) initHls(); else { vid.src = streamUrl; vid.play().catch(function(){}); }

                function playStream() { if (vid) vid.play().catch(function(){}); }
                function pauseStream() { if (vid) vid.pause(); }
                function togglePlay() { if (vid) { if (vid.paused) vid.play().catch(function(){}); else vid.pause(); } }
                function seekTo(s) { if (vid) vid.currentTime = Math.max(0, s); }; function seekRel(d) { if (vid) vid.currentTime = Math.max(0, (vid.currentTime || 0) + d); }
                function setVolume(v) { if (vid) vid.volume = Math.max(0, Math.min(1, v)); }; function stopStream() { if (vid) { vid.pause(); vid.removeAttribute('src'); vid.load(); } }

                function setDisplayMode(mode, zoom, panY) {
                  if (!vid) return;
                  if (mode === 'FILL' || mode === 'EXTEND_16_9') { vid.style.objectFit = 'fill'; vid.style.transform = 'scale(1.0)'; }
                  else if (mode === 'FIT') { vid.style.objectFit = 'contain'; vid.style.transform = 'scale(1.0)'; }
                  else { vid.style.objectFit = 'contain'; vid.style.transform = 'scale(' + (zoom || 1.0) + ') translateY(' + (panY || 0) + 'px)'; }
                }

                var zoomLevels = ['FIT', '1.15', '1.30', '0.85', '0.75', 'FILL'];
                var zoomIdx = 0;
                function cycleZoom() {
                  zoomIdx = (zoomIdx + 1) % zoomLevels.length;
                  var z = zoomLevels[zoomIdx];
                  if (z === 'FIT') setDisplayMode('FIT', 1.0, 0);
                  else if (z === 'FILL') setDisplayMode('FILL', 1.0, 0);
                  else setDisplayMode('ZOOM', parseFloat(z), 0);
                }

                function toggleSubtitles() {
                  if (hlsInstance && hlsInstance.subtitleTracks && hlsInstance.subtitleTracks.length > 0) {
                    var cur = hlsInstance.subtitleTrack;
                    var next = (cur === -1) ? 0 : -1;
                    hlsInstance.subtitleTrack = next;
                    var name = (next === -1) ? 'Desactivados' : (hlsInstance.subtitleTracks[next].name || 'Español');
                    if (window.AndroidBridge && window.AndroidBridge.onToast) window.AndroidBridge.onToast('💬 Subtítulos: ' + name);
                  } else if (vid && vid.textTracks && vid.textTracks.length > 0) {
                    var tr = vid.textTracks[0];
                    tr.mode = (tr.mode === 'showing') ? 'hidden' : 'showing';
                    var name = (tr.mode === 'showing') ? (tr.label || 'Español') : 'Desactivados';
                    if (window.AndroidBridge && window.AndroidBridge.onToast) window.AndroidBridge.onToast('💬 Subtítulos: ' + name);
                  } else {
                    if (window.AndroidBridge && window.AndroidBridge.onToast) window.AndroidBridge.onToast('💬 Sin pistas de subtítulos');
                  }
                }

                function cycleQuality() {
                  if (hlsInstance && hlsInstance.levels && hlsInstance.levels.length > 1) {
                    var cur = hlsInstance.currentLevel;
                    var next = (cur + 1 >= hlsInstance.levels.length) ? -1 : cur + 1;
                    hlsInstance.currentLevel = next;
                    var qName = (next === -1) ? 'Automática' : (hlsInstance.levels[next].height + 'p');
                    if (window.AndroidBridge && window.AndroidBridge.onToast) window.AndroidBridge.onToast('📺 Calidad: ' + qName);
                  } else {
                    if (window.AndroidBridge && window.AndroidBridge.onToast) window.AndroidBridge.onToast('📺 Calidad: 1080p Full HD (Máxima)');
                  }
                }

                setInterval(reportStatus, 1000);
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
