/* ES5 deliberately: outdated WebViews must show useful guidance, not a blank UI. */
(function (global) {
  'use strict';
  if (!global.MovieBoxAndroidBridge) return;
  var supported = !!global.Promise && !!global.Map && !!Object.assign &&
    !!Array.from && !!String.prototype.padStart && !!global.fetch &&
    !!global.CSS && global.CSS.supports('color', 'var(--moviebox-check)');
  try { if (supported) new Function('return async function () { return 1; };'); }
  catch (_) { supported = false; }
  if (supported) return;
  global.MovieBoxAndroidUnsupported = true;
  global.MovieBoxAndroidKey = function (key, down) {
    if (down && (key === 'Enter' || key === 'Back' || key === 'Escape')) global.MovieBoxAndroidBridge.exit();
  };
  document.addEventListener('DOMContentLoaded', function () {
    while (document.body.firstChild) document.body.removeChild(document.body.firstChild);
    document.body.style.cssText = 'margin:0;background:#0b1016;color:#eef7f3;font-family:Arial,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh';
    var panel = document.createElement('main');
    panel.style.cssText = 'width:900px;max-width:80%;padding:52px;border:1px solid #30443f;border-radius:24px;background:#13231f';
    var title = document.createElement('h1'); title.textContent = 'One update before you watch';
    title.style.cssText = 'font-size:42px;margin:0 0 24px';
    var help = document.createElement('p');
    help.textContent = 'Update Android System WebView from Google Play on your TV, then reopen MovieBox. Your saved titles and watching progress stay on this device.';
    help.style.cssText = 'font-size:26px;line-height:1.6;margin:0 0 32px';
    var close = document.createElement('button'); close.textContent = 'Close MovieBox';
    close.style.cssText = 'padding:18px 28px;border:2px solid #79e9bd;border-radius:12px;background:#79e9bd;color:#09251a;font-size:24px';
    close.onclick = function () { global.MovieBoxAndroidBridge.exit(); };
    panel.appendChild(title);panel.appendChild(help);panel.appendChild(close);document.body.appendChild(panel);close.focus();
    document.addEventListener('keydown', function (event) {
      if (event.keyCode === 13 || event.keyCode === 23) { event.preventDefault();close.click(); }
    });
  });
})(window);
