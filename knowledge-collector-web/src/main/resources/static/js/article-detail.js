window.addEventListener("DOMContentLoaded", () => {
    const A = window.AdminCommon;
    const root = document.querySelector("main");
    const id = root.dataset.articleId;
    const $ = selector => document.querySelector(selector);

    function renderAi(data) {
        if (!data) return;
        $("#ai-status").textContent = data.status === "SUCCESS" ? "已完成" : "失败";
        $("#ai-summary").textContent = data.oneSentenceSummary || data.errorMessage || "暂无分析结果";
        $("#ai-core-summary").textContent = data.coreSummary || "";
        $("#ai-result").hidden = data.status !== "SUCCESS";
        $("#ai-key-points").innerHTML = (data.keyPoints || []).map(item => `<li>${A.escape(item)}</li>`).join("");
        $("#ai-keywords").innerHTML = [...(data.keywords || []), ...(data.tags || [])]
            .map(item => `<span class="tag tag-ai">${A.escape(item)}</span>`).join("");
        $("#ai-category").textContent = data.category || "未分类";
        $("#ai-value").textContent = data.readingValue ?? "—";
        $("#ai-meta").textContent = `${data.provider || "AI"} · ${data.model || "默认模型"} · ${data.durationMillis || 0} ms`;
        const groups = [["文章大纲",data.outline],["关键结论",data.keyConclusions],["关键数据",data.keyData],
            ["重要案例",data.importantCases],["人物",data.people],["机构",data.organizations],["产品",data.products],
            ["技术",data.technologies],["地点",data.locations],["时间",data.timeInformation],["信息性质",data.informationNature]];
        $("#ai-structured").innerHTML = groups.filter(([,items]) => items?.length).map(([name,items]) =>
            `<div><strong>${A.escape(name)}</strong><ul>${items.map(item => `<li>${A.escape(item)}</li>`).join("")}</ul></div>`).join("") +
            `<p><strong>文章类型</strong> ${A.escape(data.articleType || "未分类")}</p><p><strong>来源可信度</strong> ${A.escape(data.sourceCredibility || "待核验")}</p><p><strong>推荐理由</strong> ${A.escape(data.readingReason || "暂无")}</p>`;
    }
    async function loadAi() {
        try { renderAi(await A.request(`/api/v1/articles/${id}/ai`)); }
        catch (error) { $("#ai-summary").textContent = error.message; }
    }
    $("#ai-analyze").onclick = async event => {
        event.currentTarget.disabled = true;
        event.currentTarget.textContent = "AI 正在分析…";
        try {
            renderAi(await A.request(`/api/v1/articles/${id}/ai/analyze`, {method:"POST"}));
            A.message("AI 分析已完成");
        } catch (error) { A.message(error.message, true); }
        finally { event.currentTarget.disabled = false; event.currentTarget.textContent = "使用默认 AI 重新分析"; }
    };

    async function state(body, message) {
        const data = await A.request(`/api/v1/articles/${id}/reading/state`,
            {method:"PATCH", body:JSON.stringify(body)});
        $("#reading-status").textContent = data.readingStatus;
        $("#favorite-button").textContent = data.favorite ? "★ 取消收藏" : "☆ 收藏";
        $("#favorite-button").dataset.value = String(!data.favorite);
        A.message(message);
    }
    $("#favorite-button").onclick = event =>
        state({favorite:event.currentTarget.dataset.value === "true"}, "收藏状态已更新");
    document.querySelectorAll("[data-reading-status]").forEach(button => {
        button.onclick = () => state({readingStatus:button.dataset.readingStatus,
            archived:button.dataset.readingStatus === "ARCHIVED"}, "阅读状态已更新");
    });
    $("#note-form").onsubmit = async event => {
        event.preventDefault();
        await A.request(`/api/v1/articles/${id}/reading/note`,
            {method:"PUT", body:JSON.stringify({content:$("#article-note").value})});
        A.message("个人笔记已保存");
    };
    $("#tag-form").onsubmit = async event => {
        event.preventDefault();
        const data = await A.request(`/api/v1/articles/${id}/reading/tags`,
            {method:"PUT", body:JSON.stringify({tagNames:$("#article-tags").value})});
        $("#tag-list").innerHTML = data.tags.map(tag => `<span class="tag">${A.escape(tag.name)}</span>`).join("");
        A.message("标签已更新");
    };
    $("#use-selection").onclick = () => {
        const selected = window.getSelection()?.toString().trim();
        if (!selected) return A.message("请先在正文中选中一段内容", true);
        $("#card-content").value = selected;
    };
    $("#knowledge-card-form").onsubmit = async event => {
        event.preventDefault();
        await A.request("/api/v1/knowledge/cards", {method:"POST", body:JSON.stringify({articleId:Number(id),
            title:$("#card-title").value,content:$("#card-content").value,cardType:$("#card-type").value,
            tags:$("#card-tags").value,sourceLocation:"文章阅读页人工摘录",confirmed:true})});
        event.currentTarget.reset(); A.message("知识卡片已保存，可在知识工作台继续关联与研究");
    };
    loadAi();
});
