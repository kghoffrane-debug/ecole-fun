(function () {
  function confetti() {
    var icons = ['🎉', '⭐', '🎈', '✨', '🌈', '🎊'];
    for (var i = 0; i < 36; i++) {
      var s = document.createElement('span');
      s.className = 'confetti';
      s.textContent = icons[Math.floor(Math.random() * icons.length)];
      s.style.left = Math.random() * 100 + 'vw';
      s.style.fontSize = 16 + Math.random() * 18 + 'px';
      s.style.animationDuration = 2 + Math.random() * 2.5 + 's';
      s.style.animationDelay = Math.random() * 0.8 + 's';
      document.body.appendChild(s);
      setTimeout(function (el) { el.remove(); }.bind(null, s), 5500);
    }
  }

  if (document.querySelector('.flash.ok')) confetti();
  document.querySelectorAll('.mascot').forEach(function (m) {
    m.addEventListener('click', confetti);
  });

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(function () {});
  }
})();
