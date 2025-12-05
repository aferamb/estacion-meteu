// Shared JS helpers for UI pages
const strictTsRegex = /^\d{4}-\d{2}-\d{2}\d{2}:\d{2}:\d{2}$/; // yyyy-MM-ddHH:mm:ss

function showJsonTable(container, data){
  const wrap = document.getElementById(container);
  if(!wrap) return;
  wrap.innerHTML = '';
  if(!data || data.length===0){ wrap.innerHTML = '<div class="small muted">No data</div>'; return; }
  const keys = Object.keys(data[0]);
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const trh = document.createElement('tr');
  keys.forEach(k=>{ const th=document.createElement('th'); th.textContent=k; trh.appendChild(th); });
  thead.appendChild(trh); table.appendChild(thead);
  const tbody = document.createElement('tbody');
  data.forEach(row=>{
    const tr=document.createElement('tr');
    keys.forEach(k=>{ const td=document.createElement('td'); let v=row[k]; if(v===null||v===undefined) v=''; td.textContent = v; tr.appendChild(td); });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  const div = document.createElement('div'); div.className='table-wrap'; div.appendChild(table);
  wrap.appendChild(div);
}

async function fetchJson(url, opts){
  const r = await fetch(url, opts);
  if(!r.ok){ const txt = await r.text(); throw new Error(txt||r.statusText); }
  return r.json();
}

function validateStrictTs(s){ if(!s) return true; return strictTsRegex.test(s); }

// Health polling helper: fetches `/Health` and updates element with id `healthStatus`.
async function fetchHealthStatus() {
  try {
    const resp = await fetch('Health');
    if (!resp.ok) throw new Error('HTTP ' + resp.status);
    const txt = await resp.text();
    return txt.trim();
  } catch (e) {
    return 'unreachable';
  }
}

function startHealthPolling(intervalSeconds) {
  const el = document.getElementById('healthStatus');
  if (!el) return;
  async function tick() {
    const s = await fetchHealthStatus();
    el.textContent = s;
    el.className = 'health-' + (s === 'OK' ? 'ok' : s === 'DEGRADED' ? 'warn' : 'bad');
  }
  tick();
  return setInterval(tick, Math.max(5, intervalSeconds|0) * 1000);
}
