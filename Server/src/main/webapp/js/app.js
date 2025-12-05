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

// Live feed: poll /admin/live and update stations and messages
function startLiveFeed(intervalSeconds) {
  const stationsEl = document.getElementById('liveStations');
  const messagesEl = document.getElementById('liveMessages');
  if (!stationsEl || !messagesEl) return;
  function renderStations(list) {
    stationsEl.innerHTML = '';
    if (!list || list.length === 0) { stationsEl.innerHTML = '<div class="small muted">No hay estaciones conectadas</div>'; return; }
    const ul = document.createElement('ul');
    list.forEach(s => { const li = document.createElement('li'); li.textContent = (s.sensor_id||'desconocido') + (s.last_seen?(' — '+s.last_seen):''); ul.appendChild(li); });
    stationsEl.appendChild(ul);
  }
  function renderMessages(list) {
    messagesEl.innerHTML = '';
    if (!list || list.length === 0) { messagesEl.innerHTML = '<div class="small muted">Sin mensajes recientes</div>'; return; }
    const wrap = document.createElement('div'); wrap.className = 'table-wrap';
    const table = document.createElement('table');
    const thead = document.createElement('thead'); thead.innerHTML = '<tr><th>Sensor</th><th>Hora</th><th>Temp</th><th>Humid</th><th>AQI</th><th>Lux</th></tr>'; table.appendChild(thead);
    const tbody = document.createElement('tbody');
    list.forEach(m => {
      const tr = document.createElement('tr');
      tr.innerHTML = '<td>'+(m.sensor_id||'')+'</td><td>'+(m.recorded_at||'')+'</td><td>'+(m.temp==null?'':m.temp)+'</td><td>'+(m.humid==null?'':m.humid)+'</td><td>'+(m.aqi==null?'':m.aqi)+'</td><td>'+(m.lux==null?'':m.lux)+'</td>';
      tbody.appendChild(tr);
    });
    table.appendChild(tbody); wrap.appendChild(table); messagesEl.appendChild(wrap);
  }
  async function tick() {
    try {
      const data = await fetchJson('/admin/live');
      if (data && data.stations) renderStations(data.stations);
      if (data && data.messages) renderMessages(data.messages);
    } catch (e) {
      console.warn('live feed error', e);
    }
  }
  tick();
  return setInterval(tick, Math.max(1, intervalSeconds|0) * 1000);
}
