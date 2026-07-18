window.addEventListener("DOMContentLoaded", async () => {
    const A=window.AdminCommon; let page=0,totalPages=0,topicMap=new Map(); const $=id=>document.querySelector(`#${id}`);
    async function loadTopics(){const topics=await A.request("/api/v1/topics/options");topicMap=new Map(topics.map(t=>[t.id,t.name]));
        $("source-topics").innerHTML=topics.map(t=>`<option value="${t.id}">${A.escape(t.name)}</option>`).join("");}
    async function load(){try{const q=new URLSearchParams({page,size:10});if($("source-keyword").value.trim())q.set("keyword",$("source-keyword").value.trim());
        if($("source-type-filter").value)q.set("type",$("source-type-filter").value);if($("source-enabled-filter").value)q.set("enabled",$("source-enabled-filter").value);
        const data=await A.request(`/api/v1/sources?${q}`);totalPages=data.totalPages;$("source-rows").innerHTML=data.content.map(s=>`<tr>
        <td><strong>${A.escape(s.name)}</strong><small>${A.escape(s.code)}</small></td><td>${s.type}</td>
        <td>${[...s.topicIds].map(id=>A.escape(topicMap.get(id)||id)).join("、")||"—"}</td>
        <td>${s.timeoutSeconds}s / ${s.maxRetries} 次 / ${s.requestIntervalMillis}ms</td>
        <td><span class="status ${s.healthStatus==="VERIFIED"?"on":"off"}">${{VERIFIED:"正常 · 已验证",UNHEALTHY:"异常",UNKNOWN:"未检查"}[s.healthStatus]||s.healthStatus}</span><small>${A.escape(s.lastHealthMessage||"")}</small></td>
        <td>${s.lastHealthCheckedAt?new Date(s.lastHealthCheckedAt).toLocaleString("zh-CN"):"—"}<small>${s.enabled?"启用":"停用"}</small></td><td class="row-actions">
        <button data-edit="${s.id}">编辑</button><button data-health="${s.id}">刷新</button>${s.type==="HTML_LIST"?`<button data-rule="${s.id}">规则</button>`:""}<button data-crawl="${s.id}">立即采集</button>
        <button data-toggle="${s.id}" data-enabled="${!s.enabled}">${s.enabled?"停用":"启用"}</button>
        <button class="danger-link" data-delete="${s.id}">删除</button></td></tr>`).join("");
        $("source-empty").hidden=data.content.length>0;$("source-page").textContent=`第 ${page+1} 页 / 共 ${Math.max(totalPages,1)} 页`;
        $("source-prev").disabled=page===0;$("source-next").disabled=page+1>=totalPages;}catch(e){A.message(e.message,true);}}
    function edit(s={}){$("source-editor").hidden=false;$("source-editor-title").textContent=s.id?"编辑采集源":"新建采集源";
        $("source-id").value=s.id||"";$("source-code").value=s.code||"";$("source-name").value=s.name||"";$("source-type").value=s.type||"RSS";
        $("source-home-url").value=s.homeUrl||"";$("source-feed-url").value=s.feedUrl||"";$("source-language").value=s.language||"zh-CN";
        $("source-charset").value=s.charset||"UTF-8";$("source-user-agent").value=s.userAgent||"KnowledgeCollector/1.0 (+local-admin)";
        $("source-timeout").value=s.timeoutSeconds||15;$("source-retries").value=s.maxRetries??2;$("source-interval").value=s.requestIntervalMillis??2000;
        $("source-notes").value=s.notes||"";$("source-obey-robots").checked=s.obeyRobots??true;$("source-full-content").checked=s.fetchFullContent??true;
        $("source-summary-only").checked=s.summaryOnly??false;$("source-snapshot").checked=s.saveSnapshot??false;$("source-enabled").checked=s.enabled??true;
        [...$("source-topics").options].forEach(o=>o.selected=(s.topicIds||[]).includes(Number(o.value)));$("source-editor").scrollIntoView({behavior:"smooth"});}
    $("source-form").addEventListener("submit",async e=>{e.preventDefault();const id=$("source-id").value;const body={code:$("source-code").value,name:$("source-name").value,
        type:$("source-type").value,homeUrl:$("source-home-url").value,feedUrl:$("source-feed-url").value,language:$("source-language").value,
        charset:$("source-charset").value,userAgent:$("source-user-agent").value,timeoutSeconds:Number($("source-timeout").value),
        maxRetries:Number($("source-retries").value),requestIntervalMillis:Number($("source-interval").value),obeyRobots:$("source-obey-robots").checked,
        fetchFullContent:$("source-full-content").checked,summaryOnly:$("source-summary-only").checked,saveSnapshot:$("source-snapshot").checked,
        enabled:$("source-enabled").checked,notes:$("source-notes").value,topicIds:[...$("source-topics").selectedOptions].map(o=>Number(o.value))};
        try{await A.request(`/api/v1/sources${id?`/${id}`:""}`,{method:id?"PUT":"POST",body:JSON.stringify(body)});A.message(id?"采集源已更新":"采集源已创建");
            $("source-editor").hidden=true;await load();}catch(err){A.message(err.message,true);}});
    $("source-rows").addEventListener("click",async e=>{const b=e.target.closest("button");if(!b)return;try{
        if(b.dataset.edit)edit(await A.request(`/api/v1/sources/${b.dataset.edit}`));
        if(b.dataset.test)await A.request(`/api/v1/sources/${b.dataset.test}/test`,{method:"POST"});
        if(b.dataset.health){await A.request(`/api/v1/sources/${b.dataset.health}/health`,{method:"POST"});await load();}
        if(b.dataset.rule)location.href=`/sources/${b.dataset.rule}/rules`;
        if(b.dataset.crawl){const task=await A.request(`/api/v1/sources/${b.dataset.crawl}/crawl`,{method:"POST"});location.href=`/tasks/${task.id}`;}
        if(b.dataset.toggle){await A.request(`/api/v1/sources/${b.dataset.toggle}/enabled`,{method:"PATCH",body:JSON.stringify({enabled:b.dataset.enabled==="true"})});await load();}
        if(b.dataset.delete&&confirm("确定删除该采集源？")){await A.request(`/api/v1/sources/${b.dataset.delete}`,{method:"DELETE"});await load();}
    }catch(err){A.message(err.message,true);}});
    $("source-search").onclick=()=>{page=0;load();};$("source-new").onclick=()=>edit();$("source-cancel").onclick=()=>{$("source-editor").hidden=true;};
    $("source-refresh-all").onclick=async()=>{try{A.message("正在刷新全部启用采集源…");await A.request("/api/v1/sources/health/refresh",{method:"POST"});A.message("采集源状态已刷新");await load();}catch(e){A.message(e.message,true);}};
    $("source-discovery-form").addEventListener("submit",async e=>{e.preventDefault();const button=$("source-discover");button.disabled=true;button.textContent="AI 正在发现并验证…";
        try{const result=await A.request("/api/v1/sources/discover",{method:"POST",body:JSON.stringify({topic:$("discovery-topic").value,language:$("discovery-language").value,count:Number($("discovery-count").value),quality:$("discovery-quality").value})});
            $("discovery-result").hidden=false;$("discovery-result").innerHTML=`<strong>已收纳 ${result.imported} / ${result.requested} 个有效采集源</strong><span>AI 建议 ${result.suggested} 个，过滤 ${result.rejected.length} 个无效或低质量来源。</span>`;
            A.message(`AI 已新增 ${result.imported} 个已验证采集源`);await load();}catch(err){A.message(err.message,true);}finally{button.disabled=false;button.textContent="AI 发现并收纳";}});
    $("source-prev").onclick=()=>{page--;load();};$("source-next").onclick=()=>{page++;load();};
    try{await loadTopics();await load();}catch(e){A.message(e.message,true);}
});
