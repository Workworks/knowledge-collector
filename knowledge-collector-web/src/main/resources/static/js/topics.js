window.addEventListener("DOMContentLoaded", () => {
    const A = window.AdminCommon; let page = 0, totalPages = 0;
    const $ = id => document.querySelector(`#${id}`);
    async function load() {
        try {
            const query = new URLSearchParams({page, size: 10});
            if ($("topic-keyword").value.trim()) query.set("keyword", $("topic-keyword").value.trim());
            if ($("topic-enabled-filter").value) query.set("enabled", $("topic-enabled-filter").value);
            const data = await A.request(`/api/v1/topics?${query}`);
            totalPages = data.totalPages;
            $("topic-rows").innerHTML = data.content.map(topic => `<tr>
                <td><strong>${A.escape(topic.name)}</strong><small>${A.escape(topic.code)}</small></td>
                <td>${A.escape(topic.keywords.join("、") || "—")}</td><td>${A.escape(topic.language)}</td>
                <td>${topic.sortOrder}</td><td><span class="status ${topic.enabled ? "on" : "off"}">${topic.enabled ? "启用" : "停用"}</span></td>
                <td class="row-actions"><button data-edit="${topic.id}">编辑</button>
                <button data-toggle="${topic.id}" data-enabled="${!topic.enabled}">${topic.enabled ? "停用" : "启用"}</button>
                <button class="danger-link" data-delete="${topic.id}">删除</button></td></tr>`).join("");
            $("topic-empty").hidden = data.content.length > 0;
            $("topic-page").textContent = `第 ${page + 1} 页 / 共 ${Math.max(totalPages, 1)} 页`;
            $("topic-prev").disabled = page === 0; $("topic-next").disabled = page + 1 >= totalPages;
        } catch (e) { A.message(e.message, true); }
    }
    function edit(topic = {}) {
        $("topic-editor").hidden = false; $("topic-editor-title").textContent = topic.id ? "编辑主题" : "新建主题";
        $("topic-id").value = topic.id || ""; $("topic-code").value = topic.code || "";
        $("topic-name").value = topic.name || ""; $("topic-description").value = topic.description || "";
        $("topic-keywords").value = (topic.keywords || []).join("\n"); $("topic-excluded").value = (topic.excludedKeywords || []).join("\n");
        $("topic-color").value = topic.color || "#2563EB"; $("topic-icon").value = topic.icon || "";
        $("topic-language").value = topic.language || "zh-CN"; $("topic-sort-order").value = topic.sortOrder || 0;
        $("topic-enabled").checked = topic.enabled ?? true; $("topic-editor").scrollIntoView({behavior: "smooth"});
    }
    $("topic-form").addEventListener("submit", async event => {
        event.preventDefault(); const id = $("topic-id").value;
        const body = {code:$("topic-code").value,name:$("topic-name").value,description:$("topic-description").value,
            keywords:$("topic-keywords").value,excludedKeywords:$("topic-excluded").value,color:$("topic-color").value,
            icon:$("topic-icon").value,language:$("topic-language").value,sortOrder:Number($("topic-sort-order").value),
            enabled:$("topic-enabled").checked};
        try { await A.request(`/api/v1/topics${id ? `/${id}` : ""}`, {method:id ? "PUT":"POST",body:JSON.stringify(body)});
            A.message(id ? "主题已更新" : "主题已创建"); $("topic-editor").hidden=true; await load(); } catch(e){A.message(e.message,true);}
    });
    $("topic-rows").addEventListener("click", async event => {
        const button=event.target.closest("button"); if(!button)return;
        try {
            if(button.dataset.edit) edit(await A.request(`/api/v1/topics/${button.dataset.edit}`));
            if(button.dataset.toggle){await A.request(`/api/v1/topics/${button.dataset.toggle}/enabled`,
                {method:"PATCH",body:JSON.stringify({enabled:button.dataset.enabled==="true"})}); await load();}
            if(button.dataset.delete && confirm("确定删除该主题？")){await A.request(`/api/v1/topics/${button.dataset.delete}`,{method:"DELETE"});await load();}
        } catch(e){A.message(e.message,true);}
    });
    $("topic-search").onclick=()=>{page=0;load();}; $("topic-new").onclick=()=>edit();
    $("topic-cancel").onclick=()=>{$("topic-editor").hidden=true;}; $("topic-prev").onclick=()=>{page--;load();};
    $("topic-next").onclick=()=>{page++;load();}; load();
});
