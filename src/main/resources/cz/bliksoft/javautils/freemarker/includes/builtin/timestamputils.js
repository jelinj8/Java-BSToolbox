function formatDurationSec(ms) {
  const seconds = Math.floor(Math.abs(ms / 1000));
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.round(seconds % 60);
  const t = [h, m > 9 ? m : h ? '0' + m : m || '0', s > 9 ? s : '0' + s]
    .filter(Boolean)
    .join(':');
  return ms < 0 && seconds ? `-${t}` : `+${t}`;
}

function formatDuration(ms) {
  const seconds = Math.abs(ms / 1000);
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const t = [h, m > 9 ? m : h ? '0' + m : m || '0', s >= 10 ? s.toFixed(3) : '0' + s.toFixed(3)]
    .filter(Boolean)
    .join(':');
  return ms < 0 && seconds ? `-${t}` : `+${t}`;
}