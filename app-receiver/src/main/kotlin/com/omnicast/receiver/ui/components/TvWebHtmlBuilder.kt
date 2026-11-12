package com.omnicast.receiver.ui.components

import android.content.Context

/**
 * Generador de plantilla HTML5 / Hls.js para reproducción web nativa en TV Box.
 * Desacoplado para mantener TvWebStreamScreen por debajo del límite de 300 líneas.
 */
object TvWebHtmlBuilder {

    private var cachedHlsJs: String? = null

    fun getHlsJs(context: Context): String = cachedHlsJs ?: try {
        context.assets.open("hls.light.min.js").bufferedReader().use { it.readText() }.also { cachedHlsJs = it }
    } catch (_: Throwable) { "" }

    fun buildPlayerHtml(
        safeUrl: String,
        startSec: Double,
        hlsCode: String,
        headers: Map<String, String>
    ): String {
        val requireCreds = headers["X-With-Credentials"] == "true"
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
              * { margin:0; padding:0; box-sizing:border-box; }
              html, body { width:100vw; height:100vh; overflow:hidden; background:#000000; }
              video { width:100vw; height:100vh; object-fit:contain; background:#000000; border:none; outline:none; }
              video::-webkit-media-controls { display:none !important; }
            </style>
            <script>$hlsCode</script>
            </head>
            <body>
              <video id="vplayer" autoplay playsinline></video>
              <script>
                var vid = document.getElementById('vplayer');
                var streamUrl = "$safeUrl";
                var startSec = $startSec;
                var hasResumed = false;

                function reportStatus() {
                  if (window.AndroidBridge && vid) {
                    var cur = vid.currentTime || 0;
                    var dur = (vid.duration && isFinite(vid.duration) && !isNaN(vid.duration)) ? vid.duration : 0;
                    var playing = !vid.paused && !vid.ended && vid.readyState > 2;
                    window.AndroidBridge.onPlaybackStatus(cur, dur, playing);
                  }
                  if (hls && hls.bufferController && typeof hls.bufferController.cleanBackBuffer === 'function') {
                    try { hls.bufferController.cleanBackBuffer(); } catch(_) {}
                  }
                }

                function sendHeartbeat(ev) {
                  try {
                    if (window.AndroidBridge && window.AndroidBridge.onStreamHeartbeat) window.AndroidBridge.onStreamHeartbeat(ev);
                  } catch(_) {}
                }

                vid.addEventListener('play', reportStatus);
                vid.addEventListener('pause', reportStatus);
                vid.addEventListener('ended', reportStatus);
                vid.addEventListener('timeupdate', reportStatus);
                vid.addEventListener('durationchange', reportStatus);
                vid.addEventListener('progress', function() { sendHeartbeat('PROGRESS'); });
                vid.addEventListener('error', function() {
                  if (window.AndroidBridge && window.AndroidBridge.onFatalError) {
                    var code = vid.error ? vid.error.code : 0;
                    window.AndroidBridge.onFatalError('VIDEO_ERR_' + code);
                  }
                });

                function seekStart() {
                  if (startSec > 0 && !hasResumed && vid) {
                    hasResumed = true;
                    vid.currentTime = startSec;
                  }
                }
                vid.addEventListener('loadedmetadata', function() { seekStart(); reportStatus(); });
                vid.addEventListener('canplay', seekStart);

                var hls = null;
                function initHls() {
                  if (window.Hls && Hls.isSupported()) {
                    if (hls) { try { hls.destroy(); } catch(_) {} hls = null; }
                    var cfg = { enableWorker: true, lowLatencyMode: false, backBufferLength: 10, maxBufferLength: 20, maxMaxBufferLength: 30, maxBufferSize: 25 * 1024 * 1024, maxBufferHole: 0.5 };
                    if ($requireCreds) { cfg.xhrSetup = function(xhr) { try { xhr.withCredentials = true; } catch(_) {} }; }
                    hls = new Hls(cfg);
                    hls.loadSource(streamUrl);
                    hls.attachMedia(vid);
                    hls.on(Hls.Events.MANIFEST_PARSED, function() {
                      sendHeartbeat('MANIFEST_PARSED');
                      console.log('TvWebStreamJS [MANIFEST_PARSED]', streamUrl);
                      vid.play().catch(function(){});
                      reportMetadata();
                    });
                    hls.on(Hls.Events.LEVEL_LOADED, function() { sendHeartbeat('LEVEL_LOADED'); });
                    hls.on(Hls.Events.FRAG_LOADING, function() { sendHeartbeat('FRAG_LOADING'); });
                    hls.on(Hls.Events.FRAG_LOADED, function() { sendHeartbeat('FRAG_LOADED'); });
                    hls.on(Hls.Events.ERROR, function(ev, data) {
                      console.error('TvWebStreamJS [HLS_ERROR] ' + data.type + ' ' + data.details + ' code=' + (data.response ? data.response.code : 'none'));
                      if (data.fatal) {
                        if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
                          if (cfg.xhrSetup) {
                            console.warn('TvWebStreamJS [CORS_FALLBACK] Reintentando sin credenciales XHR...');
                            delete cfg.xhrSetup;
                            try { hls.destroy(); } catch(_) {}
                            hls = new Hls(cfg);
                            hls.loadSource(streamUrl);
                            hls.attachMedia(vid);
                          } else {
                            if (window.AndroidBridge && window.AndroidBridge.onFatalError) {
                              window.AndroidBridge.onFatalError(data.details || 'NETWORK_ERROR');
                            }
                          }
                        }
                        else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
                        else {
                          if (window.AndroidBridge && window.AndroidBridge.onFatalError) {
                            window.AndroidBridge.onFatalError(data.details || 'FATAL_ERROR');
                          }
                        }
                      }
                    });
                  } else { vid.src = streamUrl; vid.play().catch(function(){}); }
                }

                function reportMetadata() {
                  try {
                    if (window.AndroidBridge && hls && hls.levels) {
                      var qList = ['Auto'];
                      for (var i = 0; i < hls.levels.length; i++) {
                        var lvl = hls.levels[i];
                        qList.push(lvl.height ? (lvl.height + 'p') : ('Nivel ' + i));
                      }
                      var sList = ['Desactivados'];
                      if (hls.subtitleTracks) {
                        for (var j = 0; j < hls.subtitleTracks.length; j++) {
                          var sub = hls.subtitleTracks[j];
                          sList.push(sub.name || sub.lang || ('Pista ' + j));
                        }
                      }
                      window.AndroidBridge.onMetadataLoaded(qList.join(','), sList.join(','));
                    }
                  } catch(_) {}
                }

                if (streamUrl.indexOf('.m3u8') !== -1) initHls(); else { vid.src = streamUrl; vid.play().catch(function(){}); }

                function playStream() { if (vid) vid.play().catch(function(){}); }
                function pauseStream() { if (vid) vid.pause(); }
                function togglePlay() { if (vid) { if (vid.paused) vid.play().catch(function(){}); else vid.pause(); } }
                function seekTo(s) { if (vid) vid.currentTime = Math.max(0, s); }
                function seekRel(d) { if (vid) vid.currentTime = Math.max(0, (vid.currentTime || 0) + d); }
                function setVolume(v) { if (vid) vid.volume = Math.max(0, Math.min(1, v)); }
                function cycleQuality() {
                  try {
                    if (!hls || !hls.levels || !hls.levels.length) return 'Única';
                    var n = (hls.currentLevel + 2) % (hls.levels.length + 1) - 1;
                    hls.currentLevel = n;
                    if (n === -1) return 'Auto';
                    var l = hls.levels[n];
                    return l && l.height ? (l.height + 'p') : ('Nivel ' + n);
                  } catch(_) { return 'Auto'; }
                }
                function setQuality(label) {
                  try {
                    if (!hls || !hls.levels) return;
                    if (label === 'Auto') { hls.currentLevel = -1; return; }
                    for (var i = 0; i < hls.levels.length; i++) {
                      var h = hls.levels[i].height ? (hls.levels[i].height + 'p') : ('Nivel ' + i);
                      if (h === label) { hls.currentLevel = i; return; }
                    }
                  } catch(_) {}
                }
                function setSubtitle(label) {
                  try {
                    if (!hls || !hls.subtitleTracks) return;
                    if (label === 'Desactivados') { hls.subtitleTrack = -1; return; }
                    for (var i = 0; i < hls.subtitleTracks.length; i++) {
                      var n = hls.subtitleTracks[i].name || hls.subtitleTracks[i].lang || ('Pista ' + i);
                      if (n === label) { hls.subtitleTrack = i; return; }
                    }
                  } catch(_) {}
                }
                function toggleSubtitles() {
                  try {
                    if (hls && hls.subtitleTracks && hls.subtitleTracks.length) {
                      var n = (hls.subtitleTrack + 2) % (hls.subtitleTracks.length + 1) - 1;
                      hls.subtitleTrack = n;
                      if (n === -1) return 'Desactivados';
                      var t = hls.subtitleTracks[n];
                      return t.name || t.lang || ('Pista ' + n);
                    }
                    if (vid && vid.textTracks && vid.textTracks.length) {
                      for (var i = 0; i < vid.textTracks.length; i++) {
                        if (vid.textTracks[i].mode === 'showing') { vid.textTracks[i].mode = 'disabled'; return 'Desactivados'; }
                      }
                      vid.textTracks[0].mode = 'showing';
                      return vid.textTracks[0].label || vid.textTracks[0].language || 'Pista 1';
                    }
                    return 'Sin subtítulos';
                  } catch(_) { return 'Sin subtítulos'; }
                }
                function stopStream() {
                  if (hls) { try { hls.destroy(); hls = null; } catch(_) {} }
                  if (vid) { vid.pause(); vid.removeAttribute('src'); vid.load(); }
                }

                function setDisplayMode(mode, zoom, panY, panX) {
                  if (!vid) return;
                  if (mode === 'FILL' || mode === 'EXTEND_16_9') { vid.style.objectFit = 'fill'; vid.style.transform = 'scale(1.0)'; }
                  else if (mode === 'FIT') { vid.style.objectFit = 'contain'; vid.style.transform = 'scale(1.0)'; }
                  else {
                    var z = zoom || 1.0; var px = panX || 0; var py = panY || 0;
                    var tx = (Math.abs(px) <= 2.5 && px !== 0) ? (px * window.innerWidth) : px;
                    var ty = (Math.abs(py) <= 2.5 && py !== 0) ? (py * window.innerHeight) : py;
                    vid.style.objectFit = 'contain'; vid.style.transform = 'translate(' + tx + 'px, ' + ty + 'px) scale(' + z + ')';
                  }
                }

                setInterval(reportStatus, 1000);
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
