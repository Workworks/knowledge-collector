window.addEventListener("DOMContentLoaded", () => {
    const A = window.AdminCommon;
    const root = document.querySelector("main");
    const id = root.dataset.articleId;
    const $ = selector => document.querySelector(selector);

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
});
