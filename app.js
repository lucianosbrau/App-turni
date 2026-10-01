let rows=[],specials=[],ferie=[],result=[];
const $=x=>document.getElementById(x);

$("file").onchange=async e=>{
 const f=e.target.files[0]; if(!f)return;
 const b=await f.arrayBuffer();
 const w=XLSX.read(b,{type:"array",cellDates:true});
 rows=XLSX.utils.sheet_to_json(w.Sheets[w.SheetNames[0]],{header:1,defval:""});
 $("info").textContent="Caricato: "+f.name;
 $("run").disabled=false;
};

function tag(container,text,fn){
 const x=document.createElement("span"); x.className="tag"; x.textContent=text;
 const b=document.createElement("button"); b.textContent="×";
 b.onclick=()=>{fn();x.remove()};
 x.appendChild(b); $(container).appendChild(x);
}

$("addSp").onclick=()=>{
 const p=$("spPerson").value,d=$("spDate").value,q=$("spReq").value;
 if(!d||!q)return;
 specials.push({p,d,q});
 tag("spList",p+" · "+d+" · "+q,()=>specials=specials.filter(x=>!(x.p==p&&x.d==d&&x.q==q)));
};

$("addFerie").onclick=()=>{
 const d=$("ferie").value;
 if(!d||ferie.includes(d))return;
 ferie.push(d);
 tag("ferieList",d,()=>ferie=ferie.filter(x=>x!=d));
};

function D(v){
 if(v instanceof Date)return new Date(v.getFullYear(),v.getMonth(),v.getDate());
 if(typeof v=="number"){let x=XLSX.SSF.parse_date_code(v);return new Date(x.y,x.m-1,x.d)}
 let x=new Date(v); return isNaN(x)?null:new Date(x.getFullYear(),x.getMonth(),x.getDate());
}
const iso=d=>d.toISOString().slice(0,10);
const fmt=d=>d.toLocaleDateString("it-IT");

function cam(v){
 const s=String(v??"").trim().toLowerCase();
 return s=="camacho"||/(^|[\/\s(])cam(?:acho)?($|[\/\s(])/.test(s);
}

function L(r,d){
 let m=[]; r.forEach((v,i)=>{if(cam(v))m.push(i)});
 if(!m.length)return"";
 if(m.some(i=>i==14||i==15))return"assenza";
 if(m.includes(1)&&m.includes(2))return"Guardia San Vito";
 if(m.includes(1))return"mattina San Vito";
 if(m.includes(2))return"pomeriggio San Vito";
 if(m.includes(4)){
   let s=String(r[4]).toLowerCase();
   if(s.includes("(n)"))return"notte PN";
   if(s.includes("(end)"))return"mattina PN";
   if(s.includes("(d)"))return"guardia diurna PN";
 }
 if(m.includes(5))return"8-15.30 spilimbergo";
 if(m.some(i=>[6,7,8].includes(i)))return"mattina San Vito";
 if(m.includes(10))return d.getDay()==1?"mattina San Vito":d.getDay()==4?"pomeriggio San Vito":"";
 return"";
}

function C(d){
 const n=Math.round((d-new Date(2026,8,1))/864e5);
 const cycle=Math.floor(n/4);
 const pos=((n%4)+4)%4;
 const team=((3+cycle-1)%8)+1;
 if(team==5)return"";
 return["GIORNO","NOTTE","SMONTO NOTTE",""][pos];
}

function H(l){
 return {
 "mattina San Vito":6,
 "pomeriggio San Vito":6,
 "Guardia San Vito":12,
 "guardia diurna PN":12,
 "8-15.30 spilimbergo":6.5,
 "notte PN":12,
 "mattina PN":6
 }[l]||0;
}

function N(d,l,c){
 const w=d.getDay()==0||d.getDay()==6;
 const sv=l.includes("San Vito");
 const pn=["guardia diurna PN","mattina PN"].includes(l);
 const sp=l.includes("spilimbergo");
 const g=["Guardia San Vito","guardia diurna PN"].includes(l);
 if(g&&c=="NOTTE")return"19.15";
 if(l=="notte PN"&&c=="GIORNO")return"18.45";
 if(l=="Smonto notte"&&c=="GIORNO")return"7.15";
 if(sv&&c=="SMONTO NOTTE")return"7.15";
 if((pn||sp)&&c=="SMONTO NOTTE")return"7.00";
 if(l=="Guardia San Vito"&&c=="GIORNO")return"7.30 + 16.00";
 if(l=="guardia diurna PN"&&c=="GIORNO")return w?"7.15 e tutto il giorno":"7.15 +16.00";
 if(w&&(sv||pn)&&c=="GIORNO")return"7.15 e tutto il giorno";
 if(sv&&c=="GIORNO")return"7.30";
 if((pn||sp)&&c=="GIORNO")return"7.15";
 return"";
}

$("run").onclick=()=>{
 let a=[];
 for(let r of rows){let d=D(r[0]);if(d)a.push({d,l:L(r,d)})}
 for(let i=1;i<a.length;i++)if(a[i-1].l=="notte PN")a[i].l="Smonto notte";

 result=a.map(x=>{
   const sd=iso(x.d);
   let l=x.l,c=C(x.d);
   const sl=specials.find(s=>s.d==sd&&s.p=="Ludovica");
   const sc=specials.find(s=>s.d==sd&&s.p=="Luciano");
   if(sl)l=sl.q;
   if(sc)c=sc.q;
   if(ferie.includes(sd))c="";
   return{d:x.d,l,c,n:N(x.d,l,c),h:H(l)};
 });

 for(let i=0;i<result.length-1;i++){
   if(ferie.includes(iso(result[i].d))&&C(result[i].d)=="NOTTE"&&!specials.some(s=>s.p=="Luciano"&&s.d==iso(result[i+1].d)))
     result[i+1].c="";
 }
 render();
};

function render(){
 const body=$("body"); body.innerHTML="";
 let week=null,total=0;
 result.forEach((r,i)=>{
   let monday=new Date(r.d);
   monday.setDate(monday.getDate()-((monday.getDay()+6)%7));
   let key=iso(monday);
   if(week!==null&&key!=week){
     let tr=document.createElement("tr");tr.className="week-total";
     tr.innerHTML="<td colspan='4'>Totale settimana</td><td>"+total+"</td>";
     body.appendChild(tr); total=0;
   }
   week=key; total+=r.h;
   let tr=document.createElement("tr");
   if([0,6].includes(r.d.getDay()))tr.className="weekend";
   [fmt(r.d),r.l,r.c,r.n,r.h].forEach(v=>{
     let td=document.createElement("td");td.textContent=v;tr.appendChild(td);
   });
   body.appendChild(tr);
   if(i==result.length-1){
     let tr2=document.createElement("tr");tr2.className="week-total";
     tr2.innerHTML="<td colspan='4'>Totale settimana</td><td>"+total+"</td>";
     body.appendChild(tr2);
   }
 });
 $("out").hidden=false;$("save").disabled=false;
}

$("save").onclick=()=>{
 const dataRows=[["Data","Ludovica","Luciano","Necessità","Ore Ludovica"],
   ...result.map(r=>[fmt(r.d),r.l,r.c,r.n,r.h])];
 const w=XLSX.utils.aoa_to_sheet(dataRows),b=XLSX.utils.book_new();
 XLSX.utils.book_append_sheet(b,w,"Elaborazione");
 w["!cols"]=[{wch:14},{wch:24},{wch:20},{wch:28},{wch:16}];

 const headerStyle={
   font:{bold:true,color:{rgb:"FFFFFF"}},
   fill:{patternType:"solid",fgColor:{rgb:"172033"}},
   alignment:{horizontal:"center",vertical:"center",wrapText:true}
 };
 const weekendStyle={
   fill:{patternType:"solid",fgColor:{rgb:"FFF200"}}
 };
 const borderStyle={style:"thin",color:{rgb:"D9D9D9"}};
 const baseStyle={
   alignment:{vertical:"center",wrapText:true},
   border:{bottom:borderStyle}
 };

 // Intestazioni.
 for(let c=0;c<5;c++){
   const cell=w[XLSX.utils.encode_cell({r:0,c})];
   if(cell)cell.s=headerStyle;
 }
 // Stesso giallo della preview per tutti i sabati e le domeniche.
 result.forEach((r,i)=>{
   const rr=i+1;
   const weekend=[0,6].includes(r.d.getDay());
   for(let c=0;c<5;c++){
     const cell=w[XLSX.utils.encode_cell({r:rr,c})];
     if(!cell)continue;
     cell.s=weekend ? {...baseStyle,fill:{patternType:"solid",fgColor:{rgb:"FFF200"}}} : baseStyle;
   }
 });

 const out=XLSX.write(b,{bookType:"xlsx",type:"array"});
 const blob=new Blob([out],{type:"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
 const url=URL.createObjectURL(blob);
 const a=document.createElement("a");
 a.href=url;
 a.download="elaborazione_turni.xlsx";
 a.rel="noopener";
 a.style.display="none";
 document.body.appendChild(a);
 a.click();
 a.remove();
 setTimeout(()=>URL.revokeObjectURL(url),1000);
};
