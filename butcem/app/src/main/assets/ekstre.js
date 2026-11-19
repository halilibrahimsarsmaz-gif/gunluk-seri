/* Garanti kredi kartı ekstresi (PDF) ayrıştırıcı. Girdi: sayfa başına {str,x,y,w} listesi. */
(function(g){
const AY={ocak:1,"şubat":2,mart:3,nisan:4,"mayıs":5,haziran:6,temmuz:7,"ağustos":8,"eylül":9,ekim:10,"kasım":11,"aralık":12};
const DATE_RE=/^(\d{1,2}) (Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık) (\d{4})$/;
const NUM_RE=/^\d{1,3}(?:\.\d{3})*,\d{2}[+-]?$/;
const TAKSIT_RE=/x\s*(\d+)\s*=.*?(\d+)\s*\.\s*Taksit/i;
const ATLA_RE=/ÖDEMENİZ İÇİN|TEŞEKKÜR|BONUS KAMPANYA|DEVİR|NAKİT AVANS FAİZİ ÖDEME/i;
const UCRET_RE=/^(DÖNEM FAİZİ|KKDF|BSMV|FAİZ|ÜCRET|AİDAT|KART ÜCRETİ)/i;
const iso=m=>m[3]+"-"+String(AY[m[2].toLocaleLowerCase("tr")]).padStart(2,"0")+"-"+String(m[1]).padStart(2,"0");
const num=s=>parseFloat(String(s).replace(/[+-]$/,"").replace(/\./g,"").replace(",","."));
function ekstreParse(pages){
  let maxRight=0;
  for(const p of pages) for(const i of p) if(NUM_RE.test(i.str.trim())) maxRight=Math.max(maxRight,i.x+i.w);
  const minAmt=maxRight-14; let kesim=null, section="", out=[];
  for(const p of pages){
    const its=p.filter(i=>i.str.trim()&&!/^bos(l|luk)?$/i.test(i.str.trim())).map(i=>({s:i.str.trim(),x:i.x,y:i.y,r:i.x+i.w})).sort((a,b)=>b.y-a.y||a.x-b.x);
    const rows=[]; for(const i of its){ const last=rows[rows.length-1]; if(last&&Math.abs(last.y-i.y)<=2.5) last.it.push(i); else rows.push({y:i.y,it:[i]}); }
    for(const row of rows){
      const it=row.it.sort((a,b)=>a.x-b.x); const text=it.map(i=>i.s).join(" ");
      if(/Hesap Kesim Tarihi/i.test(text)){ const d=it.map(i=>DATE_RE.exec(i.s)).find(Boolean); if(d) kesim=iso(d); continue; }
      const dm=it.length&&it[0].x<150?DATE_RE.exec(it[0].s):null;
      if(!dm){
        if(it.length===1&&!/\d/.test(it[0].s)&&it[0].x<150){ section=it[0].s; continue; }
        const am=[...it].reverse().find(i=>NUM_RE.test(i.s)&&i.r>=minAmt);
        const desc=it.filter(i=>!NUM_RE.test(i.s)).map(i=>i.s).join(" ");
        if(am&&UCRET_RE.test(desc)&&kesim) out.push({date:kesim,desc,amount:num(am.s),credit:false,taksit:"",section:"ÜCRET"});
        continue;
      }
      const am=[...it].reverse().find(i=>NUM_RE.test(i.s)&&i.r>=minAmt); if(!am) continue;
      const parts=it.slice(1).filter(i=>i!==am&&!NUM_RE.test(i.s)&&!/^[\d.,]+\s*TL$/i.test(i.s));
      let taksit=""; const tk=parts.find(i=>TAKSIT_RE.test(i.s)); if(tk){ const t=TAKSIT_RE.exec(tk.s); taksit=t[2]+"/"+t[1]; }
      const desc=parts.filter(i=>i!==tk).map(i=>i.s).join(" ").replace(/\s+/g," ").trim();
      if(ATLA_RE.test(desc)) continue;
      const d0=iso(dm);
      out.push({date:taksit&&kesim?kesim:d0,desc:desc||"(açıklama yok)",amount:num(am.s),credit:/\+$/.test(am.s),taksit,section});
    }
  }
  return {kesim,rows:out.filter(r=>r.amount>0)};
}
g.ekstreParse=ekstreParse;
if(typeof module!=="undefined") module.exports={ekstreParse};
})(typeof window!=="undefined"?window:globalThis);
