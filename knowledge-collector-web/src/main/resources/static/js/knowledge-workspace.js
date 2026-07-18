window.addEventListener("DOMContentLoaded", () => {
    const A = window.AdminCommon;
    const labels = {cards:"知识卡片",claims:"观点",entities:"实体",events:"事件",topicPages:"专题",
        projects:"研究项目",syntheses:"综合归纳",drafts:"写作草稿",openGaps:"开放缺口",dueReviews:"到期复习"};
    const endpoints = {cards:"cards",claims:"claims",entities:"entities",events:"events",topics:"topics",
        projects:"projects",syntheses:"syntheses",drafts:"drafts",gaps:"gaps?status=OPEN",reviews:"reviews/due"};
    const title = item => item.title || item.canonicalName || item.statement || item.description || item.cardTitle || `#${item.id}`;
    const detail = item => item.content || item.objective || item.summary || item.knowledgeSummary || item.outline ||
        item.excerpt || item.cardContent || item.introduction || item.status || "暂无补充说明";

    async function stats() {
        const data = await A.request("/api/v1/knowledge/statistics");
        document.querySelector("#knowledge-stats").innerHTML = Object.entries(data).map(([key,value]) =>
            `<article><span>${A.escape(labels[key] || key)}</span><strong>${value}</strong></article>`).join("");
    }
    async function load(panel) {
        const list = document.querySelector(`[data-list="${panel}"]`);
        list.innerHTML = '<p class="muted">正在读取…</p>';
        try {
            const data = await A.request(`/api/v1/knowledge/${endpoints[panel]}`);
            list.innerHTML = data.length ? data.map(item => `<article class="knowledge-item">
                <div><span class="knowledge-id">#${item.id}</span><strong>${A.escape(title(item))}</strong></div>
                <p>${A.escape(String(detail(item))).slice(0,600)}</p>
                <small>${A.escape(item.cardType || item.entityType || item.synthesisType || item.gapType || item.status || "KNOWLEDGE")}</small>
                ${panel === "reviews" ? `<button class="button button-small button-secondary" data-review="${item.id}">完成复习</button>` : ""}
            </article>`).join("") : '<div class="empty-card"><strong>尚无记录</strong><span>使用上方表单创建第一条知识资产。</span></div>';
            list.querySelectorAll("[data-review]").forEach(button => button.onclick = async () => {
                await A.request(`/api/v1/knowledge/reviews/${button.dataset.review}/complete`, {method:"POST",body:JSON.stringify({nextReviewDays:7})});
                A.message("复习完成，已安排 7 天后再次回顾"); await load("reviews"); await stats();
            });
        } catch (error) { list.innerHTML = `<p class="page-message error">${A.escape(error.message)}</p>`; }
    }
    document.querySelectorAll("#workspace-tabs button").forEach(button => button.onclick = () => {
        document.querySelectorAll("#workspace-tabs button,.workspace-panel").forEach(node => node.classList.remove("active"));
        button.classList.add("active"); const panel = document.querySelector(`.workspace-panel[data-panel="${button.dataset.panel}"]`);
        panel.classList.add("active"); load(button.dataset.panel);
    });
    document.querySelectorAll(".knowledge-form[data-create]").forEach(form => form.onsubmit = async event => {
        event.preventDefault(); const body = {};
        new FormData(form).forEach((value,key) => { if (value !== "") body[key] = /Id$/.test(key) ? Number(value) : value; });
        if (body.dueAt && !body.dueAt.includes("+")) body.dueAt += ":00+08:00";
        try {
            await A.request(`/api/v1/knowledge/${form.dataset.create}`, {method:"POST",body:JSON.stringify(body)});
            form.reset(); A.message("知识资产已保存"); await load(form.dataset.create); await stats();
        } catch (error) { A.message(error.message, true); }
    });
    document.querySelectorAll(".action-form").forEach(form => form.onsubmit = async event => {
        event.preventDefault(); const body = {};
        new FormData(form).forEach((value,key) => { if (value !== "") body[key] = /Id$/.test(key) ? Number(value) : value; });
        const action = form.dataset.action; let endpoint;
        if (action === "card-relation") endpoint = `cards/${body.fromId}/relations/${body.toId}`;
        if (action === "card-review") endpoint = `cards/${body.cardId}/reviews`;
        if (action === "claim-evidence") endpoint = `claims/${body.claimId}/evidence`;
        if (action === "entity-merge") endpoint = `entities/${body.sourceId}/merge/${body.targetId}`;
        if (action === "event-article") endpoint = `events/${body.eventId}/articles`;
        if (action === "project-item") endpoint = `projects/${body.projectId}/items`;
        try { await A.request(`/api/v1/knowledge/${endpoint}`, {method:"POST",body:JSON.stringify(body)});
            form.reset(); A.message("关联操作已完成"); await stats();
        } catch (error) { A.message(error.message, true); }
    });
    stats(); load("cards");
});
