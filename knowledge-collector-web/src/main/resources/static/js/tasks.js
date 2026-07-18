window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const rows = document.querySelector("#task-rows"); const $=id=>document.querySelector(`#${id}`); let page=0,totalPages=0;
    function dateValue(date){return date.toISOString().slice(0,10);}
    function resetDates(){const today=new Date();const from=new Date(today);from.setDate(today.getDate()-6);$("task-from").value=dateValue(from);$("task-to").value=dateValue(today);}
    async function loadOptions(){const data=await A.request("/api/v1/sources?page=0&size=100");$("task-source").innerHTML+=data.content.map(s=>`<option value="${s.id}">${A.escape(s.name)}</option>`).join("");}
    async function load(){
        const q=new URLSearchParams({page,size:20});[["keyword","task-keyword"],["sourceId","task-source"],["status","task-status"],["triggerType","task-trigger"],["from","task-from"],["to","task-to"]].forEach(([n,id])=>{if($(id).value)q.set(n,$(id).value);});
        const data = await A.request(`/api/v1/tasks?${q}`);totalPages=data.totalPages;
        rows.innerHTML = data.content.map(task => `<tr><td><a class="task-link" href="/tasks/${task.id}">${A.escape(task.taskNo)}</a></td>
            <td>${A.escape(task.sourceName)}</td><td><span class="status ${task.status === "SUCCESS" ? "on" : "off"}">${task.status}</span></td>
            <td>${A.escape(task.triggerType)}</td>
            <td>${new Date(task.createdAt).toLocaleString("zh-CN")}</td>
            <td><strong>${task.createdCount}</strong> / ${task.discoveredCount}</td><td>${task.duplicateCount}</td>
            <td>${task.durationMillis == null ? "—" : `${task.durationMillis} ms`}</td>
            <td><a class="button button-ghost button-small" href="/tasks/${task.id}">查看详情</a></td></tr>`).join("");
        document.querySelector("#task-empty").hidden = data.content.length > 0;
        document.querySelector("#task-count").textContent = `${data.totalElements} 个任务`;
        $("task-page").textContent=`第 ${page+1} / ${Math.max(totalPages,1)} 页`;$("task-prev").disabled=page===0;$("task-next").disabled=page+1>=totalPages;
    }
    $("task-search").onclick=()=>{page=0;load().catch(e=>A.message(e.message,true));};$("task-reset").onclick=()=>{$("task-filter").reset();resetDates();page=0;load();};
    $("task-prev").onclick=()=>{page--;load();};$("task-next").onclick=()=>{page++;load();};
    try {resetDates();await loadOptions();await load();} catch (error) { A.message(error.message, true); }
});
