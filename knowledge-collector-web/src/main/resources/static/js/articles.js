window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const $ = id => document.querySelector(`#${id}`);
    const reviewMode = document.querySelector("main").dataset.reviewMode === "true";
    let page = 0;
    let totalPages = 0;

    async function loadOptions() {
        const [topics, sources, tags] = await Promise.all([
            A.request("/api/v1/topics/options"),
            A.request("/api/v1/sources?page=0&size=100"),
            A.request("/api/v1/tags")
        ]);
        $("article-topic").innerHTML += topics.map(item =>
            `<option value="${item.id}">${A.escape(item.name)}</option>`).join("");
        $("article-source").innerHTML += sources.content.map(item =>
            `<option value="${item.id}">${A.escape(item.name)}</option>`).join("");
        $("article-tag").innerHTML += tags.map(item =>
            `<option value="${item.id}">${A.escape(item.name)}</option>`).join("");
    }

    function statusLabel(value) {
        return {UNREAD:"未读",READ:"已读",ARCHIVED:"已归档",IGNORED:"已忽略"}[value] || value;
    }

    async function load() {
        const query = new URLSearchParams({page, size: 12, sort: $("article-sort").value});
        if (reviewMode) query.set("reviewStatus", "PENDING_REVIEW");
        [["keyword","article-keyword"],["sourceId","article-source"],["topicId","article-topic"],
            ["readingStatus","article-status"],["tagId","article-tag"],["minQuality","article-quality"]]
            .forEach(([name,id]) => { if ($(id).value) query.set(name, $(id).value); });
        if ($("article-favorite").checked) query.set("favorite", "true");
        query.set("archived", $("article-archived").checked ? "true" : "false");
        const data = await A.request(`/api/v1/articles?${query}`);
        totalPages = data.totalPages;
        $("article-grid").innerHTML = data.content.map(article => `<article class="library-card">
            <div class="card-topline"><span class="status ${article.readingStatus === "READ" ? "on" : "off"}">${statusLabel(article.readingStatus)}</span>
            ${article.contentOrigin === "AI_GENERATED" ? '<span class="origin-pill">AI 内容</span>' : ''}
            <span class="quality-pill">${article.qualityScore} 分</span></div>
            <h2><a href="/articles/${article.id}">${A.escape(article.title)}</a></h2>
            <p class="article-summary">${A.escape(article.summary || "暂无摘要")}</p>
            <div class="article-meta"><span>${A.escape(article.sourceName)}</span><span>${article.readingMinutes} 分钟</span>
            <span>${article.favorite ? "★ 已收藏" : "☆ 未收藏"}</span></div>
            <div class="card-actions"><a class="button button-secondary button-small" href="/articles/${article.id}">开始阅读</a>
            <button class="button button-ghost button-small" data-favorite="${article.id}" data-value="${!article.favorite}">${article.favorite ? "取消收藏" : "收藏"}</button></div>
        </article>`).join("");
        $("article-empty").hidden = data.content.length > 0;
        $("article-count").textContent = `${data.totalElements} 篇资料`;
        $("article-page").textContent = `第 ${page + 1} / ${Math.max(totalPages, 1)} 页`;
        $("article-prev").disabled = page === 0;
        $("article-next").disabled = page + 1 >= totalPages;
    }

    $("article-search").onclick = () => { page = 0; load().catch(error => A.message(error.message, true)); };
    $("article-reset").onclick = () => {
        document.querySelector("#article-filter").reset();
        page = 0;
        load().catch(error => A.message(error.message, true));
    };
    $("article-prev").onclick = () => { page--; load(); };
    $("article-next").onclick = () => { page++; load(); };
    $("article-grid").onclick = async event => {
        const button = event.target.closest("[data-favorite]");
        if (!button) return;
        await A.request(`/api/v1/articles/${button.dataset.favorite}/reading/state`, {
            method:"PATCH", body:JSON.stringify({favorite:button.dataset.value === "true"})
        });
        await load();
    };
    try { await loadOptions(); await load(); } catch (error) { A.message(error.message, true); }
});
