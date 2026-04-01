document.addEventListener('DOMContentLoaded', () => {

    const pci = document.getElementById('process-count');
    const rci = document.getElementById('resource-count');
    const bg = document.getElementById('btn-generate');
    const mi = document.getElementById('matrix-inputs');
    const br = document.getElementById('btn-run');
    const tl = document.getElementById('trace-log');
    const es = document.getElementById('edges-svg');
    const nc = document.getElementById('nodes-container');
    const pc = document.getElementById('pool-counts');
    const rp = document.getElementById('resource-pool');
    const so = document.getElementById('status-overlay');
    const st = document.getElementById('status-title');
    const sd = document.getElementById('status-desc');

    let p = 5, r = 3, ir = false, np = [];
    const dm = 1200, au = 'http://127.0.0.1:8080/api/bankers';

    function iS() {
        es.innerHTML = `<defs><marker id="arrow-allocating" markerWidth="6" markerHeight="6" refX="25" refY="3" orient="auto"><polygon points="0 0, 6 3, 0 6" fill="var(--info)" /></marker><marker id="arrow-releasing" markerWidth="6" markerHeight="6" refX="25" refY="3" orient="auto"><polygon points="6 0, 0 3, 6 6" fill="var(--success)" /></marker></defs>`;
    }

    function iG() {
        if (ir) return;
        
        p = parseInt(pci.value) || 5;
        r = parseInt(rci.value) || 3;
        
        so.classList.add('hidden');
        so.className = 'status-overlay hidden';
        
        gMI();
        dG();
        cL();
        lM(`System initialized for ${p} Processes and ${r} Resources.`);
    }

    function dG() {
        const rt = document.querySelector('.graph-area').getBoundingClientRect();
        const cw = rt.width, ch = rt.height;
        
        iS();
        nc.innerHTML = '';
        pc.innerHTML = '';
        
        for (let j = 0; j < r; j++) {
            pc.innerHTML += `<div class="pool-item" id="pool-res-${j}">0<span>R${j}</span></div>`;
        }
        
        const cx = cw / 2, cy = ch / 2, rd = Math.min(cx, cy) * 0.65;
        np = [];
        
        for (let i = 0; i < p; i++) {
            const ag = (Math.PI * 2 * i) / p - Math.PI / 2;
            const x = cx + rd * Math.cos(ag), y = cy + rd * Math.sin(ag);
            
            np.push({ id: i, x, y });
            
            const ne = document.createElement('div');
            ne.className = 'node';
            ne.id = `node-${i}`;
            ne.style.left = `${x}px`;
            ne.style.top = `${y}px`;
            ne.innerText = `P${i}`;
            
            nc.appendChild(ne);
        }
    }

    function dE(fc, pi, ty) {
        const pn = np[pi];
        const rt = document.querySelector('.graph-area').getBoundingClientRect();
        
        const cx = rt.width / 2, cy = rt.height / 2;
        const sx = fc ? cx : pn.x, sy = fc ? cy : pn.y;
        const ex = fc ? pn.x : cx, ey = fc ? pn.y : cy;
        
        const pt = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        pt.setAttribute('d', `M ${sx} ${sy} L ${ex} ${ey}`);
        pt.setAttribute('class', `edge ${ty}`);
        pt.setAttribute('marker-end', `url(#arrow-${ty})`);
        pt.setAttribute('data-process', pi);
        
        es.appendChild(pt);
    }

    function cE() {
        const ps = es.querySelectorAll('path');
        ps.forEach(x => x.remove());
    }

    function gMI() {
        let h = ``;
        h += `<div class="section-title">Available Resources (Initial)</div>`;
        h += `<table class="matrix-table" style="margin-bottom: 12px;"><tr>`;
        
        for (let j = 0; j < r; j++) h += `<th>R${j}</th>`;
        h += `</tr><tr>`;
        
        for (let j = 0; j < r; j++) {
            let df = j === 0 ? 3 : (j === 1 ? 3 : 2);
            h += `<td><input type="number" id="avail-${j}" min="0" value="${df}"></td>`;
        }
        
        h += `</tr></table>`;
        h += `<div class="matrix-grid" style="grid-template-columns: 1fr 1fr">`;
        h += `<div><div class="section-title">Max Need</div><table class="matrix-table"><tr><th></th>`;
        
        for (let j = 0; j < r; j++) h += `<th>R${j}</th>`;
        h += `</tr>`;
        
        for (let i = 0; i < p; i++) {
            h += `<tr><th>P${i}</th>`;
            for (let j = 0; j < r; j++) {
                let df = Math.floor(Math.random() * 5) + 2;
                h += `<td><input type="number" id="max-${i}-${j}" min="0" value="${df}"></td>`;
            }
            h += `</tr>`;
        }
        
        h += `</table></div>`;
        h += `<div><div class="section-title">Current Allocation</div><table class="matrix-table"><tr><th></th>`;
        
        for (let j = 0; j < r; j++) h += `<th>R${j}</th>`;
        h += `</tr>`;
        
        for (let i = 0; i < p; i++) {
            h += `<tr><th>P${i}</th>`;
            for (let j = 0; j < r; j++) {
                h += `<td><input type="number" id="alloc-${i}-${j}" min="0" value="${Math.floor(Math.random() * 2)}"></td>`;
            }
            h += `</tr>`;
        }
        
        h += `</table></div></div>`;
        mi.innerHTML = h;
        uPV();
    }

    function gP() {
        let mx = [], al = [], av = [];
        
        for (let j = 0; j < r; j++) {
            av.push(parseInt(document.getElementById(`avail-${j}`).value) || 0);
        }
        
        for (let i = 0; i < p; i++) {
            let mr = [], ar = [];
            for (let j = 0; j < r; j++) {
                mr.push(parseInt(document.getElementById(`max-${i}-${j}`).value) || 0);
                ar.push(parseInt(document.getElementById(`alloc-${i}-${j}`).value) || 0);
            }
            mx.push(mr);
            al.push(ar);
        }
        
        return { processes: p, resources: r, available: av, max: mx, allocation: al };
    }

    function uPV(aa) {
        if (!aa) {
            aa = [];
            for (let j = 0; j < r; j++) {
                let vl = document.getElementById(`avail-${j}`);
                aa.push(vl ? parseInt(vl.value) || 0 : 0);
            }
        }
        
        for (let j = 0; j < r; j++) {
            let el = document.getElementById(`pool-res-${j}`);
            if (el) el.innerHTML = `${aa[j]}<span>R${j}</span>`;
        }
    }

    function lM(mg, ty = 'info') {
        if (!mg) return;
        
        const en = document.createElement('div');
        en.className = `log-entry ${ty}`;
        en.innerText = mg;
        
        tl.appendChild(en);
        tl.scrollTo({ top: tl.scrollHeight, behavior: 'smooth' });
    }

    function cL() {
        tl.innerHTML = '';
    }

    function hN(id, st) {
        const el = document.getElementById(`node-${id}`);
        if (el) el.className = `node ${st}`;
    }

    function rV() {
        document.querySelectorAll('.node').forEach(x => x.className = 'node');
        cE();
        rp.className = 'resource-pool';
        so.classList.add('hidden');
    }

    const slp = ms => new Promise(rs => setTimeout(rs, ms));

    async function rA() {
        if (ir) return;
        
        cL();
        rV();
        lM("Sending matrix data to Java API for Banker's evaluation...", "info");
        
        ir = true;
        br.disabled = true;
        bg.disabled = true;
        const py = gP();
        
        try {
            const rs = await fetch(au, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(py)
            });
            
            if (!rs.ok) throw new Error(`HTTP Error! Status: ${rs.status}`);
            
            const dt = await rs.json();
            await aT(dt.trace, dt.isSafe);
            
        } catch (er) {
            lM(`Error contacting backend: ${er.message}`, "deadlockMsg");
            lM("Make sure the backend is running on port 8080.", "info");
            
            document.getElementById('server-status').className = 'server-status';
            document.getElementById('server-status').innerHTML = '<span class="pulse-indicator" style="background:var(--danger)"></span> Offline';
            
        } finally {
            ir = false;
            br.disabled = false;
            bg.disabled = false;
        }
    }

    async function aT(ts, is) {
        for (const ev of ts) {
            if (ev.message) lM(ev.message, ev.type);
            
            switch (ev.type) {
                case 'init': 
                    if (ev.available) uPV(ev.available); 
                    break;
                case 'check': 
                    hN(ev.node, 'checking'); 
                    rp.classList.add('active'); 
                    await slp(dm * 0.5); 
                    break;
                case 'allocate': 
                    hN(ev.node, 'checking'); 
                    dE(true, ev.node, 'allocating'); 
                    await slp(dm); 
                    break;
                case 'release': 
                    cE(); 
                    dE(false, ev.node, 'releasing'); 
                    hN(ev.node, 'safe'); 
                    await slp(dm); 
                    cE(); 
                    rp.classList.remove('active'); 
                    if (ev.available) uPV(ev.available); 
                    await slp(dm * 0.5); 
                    break;
                case 'wait': 
                    hN(ev.node, 'deadlock'); 
                    rp.classList.remove('active'); 
                    await slp(dm * 0.7); 
                    hN(ev.node, ''); 
                    break;
            }
        }
        
        setTimeout(() => {
            so.classList.remove('hidden');
            
            if (!is) {
                so.className = 'status-overlay';
                st.innerText = "UNSAFE STATE DETECTED";
                sd.innerText = "The Java Banker's algorithm determined a deadlock could occur.";
            } else {
                so.className = 'status-overlay safe';
                st.innerText = "SYSTEM SAFE";
                sd.innerText = "Valid execution sequence found. No deadlock will occur.";
            }
            
            setTimeout(() => {
                so.classList.add('hidden');
            }, 3000);
            
        }, 800);
    }

    bg.addEventListener('click', iG);
    br.addEventListener('click', rA);
    
    mi.addEventListener('input', e => {
        if (e.target.id && e.target.id.startsWith('avail-')) uPV();
    });
    
    let rzT;
    window.addEventListener('resize', () => {
        if (!ir) {
            clearTimeout(rzT);
            rzT = setTimeout(() => dG(), 100);
        }
    });
    
    setTimeout(iG, 100);
});
