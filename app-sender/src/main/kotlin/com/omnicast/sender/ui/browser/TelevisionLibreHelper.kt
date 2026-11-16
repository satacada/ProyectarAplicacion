package com.omnicast.sender.ui.browser

import android.webkit.WebView

/**
 * Satélite desacoplado para Television Libre (television-libre.st).
 * Principio "Poner y Sacar": Cero código invasivo en BrowserWebView.
 */
object TelevisionLibreHelper {

    private const val SCRIPT = """
        (function() {
            if (window.location.hostname.indexOf('television-libre.st') === -1) return;

            function fixAgenda() {
                var ifr = document.getElementById('agenda');
                if (!ifr) return;
                ifr.style.backgroundColor = '#ffffff';
                try {
                    var doc = ifr.contentDocument || (ifr.contentWindow ? ifr.contentWindow.document : null);
                    if (doc && doc.body) {
                        doc.body.style.backgroundColor = '#ffffff';
                        var links = doc.querySelectorAll('a');
                        for (var i = 0; i < links.length; i++) {
                            var h = links[i].getAttribute('href');
                            if (h && h.indexOf('/en-vivo/') !== -1) {
                                links[i].setAttribute('target', '_top');
                                links[i].onclick = function(e) {
                                    e.preventDefault();
                                    window.top.location.href = this.href;
                                };
                            }
                        }
                    }
                } catch(_) {}
            }

            function fixChannel() {
                var ifr = document.getElementById('iframe');
                if (!ifr) return;
                var src = (ifr.getAttribute('src') || '').toLowerCase();
                if (src.indexOf('/html/fl/') !== -1) {
                    var serverLinks = document.querySelectorAll('.server-links a, nav a');
                    for (var i = 0; i < serverLinks.length; i++) {
                        var txt = (serverLinks[i].textContent || '').toLowerCase();
                        if (txt.indexOf('opción 2') !== -1 || txt.indexOf('opcion 2') !== -1) {
                            serverLinks[i].click();
                            break;
                        }
                    }
                }
            }

            fixAgenda();
            fixChannel();
            var ifr = document.getElementById('agenda');
            if (ifr) ifr.addEventListener('load', fixAgenda);
            setTimeout(fixAgenda, 500);
            setTimeout(fixAgenda, 1500);
            setTimeout(fixAgenda, 3000);
            setTimeout(fixChannel, 1000);
        })();
    """

    fun injectIfMatches(webView: WebView?, url: String?) {
        if (webView == null || url == null) return
        if (url.contains("television-libre.st", ignoreCase = true)) {
            webView.evaluateJavascript(SCRIPT, null)
        }
    }
}
