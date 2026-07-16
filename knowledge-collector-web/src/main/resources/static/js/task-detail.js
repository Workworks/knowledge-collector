window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const id = document.querySelector("main").dataset.taskId;
    try {
        const [task, items] = await Promise.all([
            A.request(`/api/v1/tasks/${id}`), A.request(`/api/v1/tasks/${id}/items`)
        ]);
        document.querySelector("#task-title").textContent = task.taskNo;
        document.querySelector("#task-source").textContent = task.sourceName;
        document.querySelector("#task-status").textContent = task.status;
        document.querySelector("#task-discovered").textContent = task.discoveredCount;
        document.querySelector("#task-created").textContent = task.createdCount;
        document.querySelector("#task-duplicates").textContent = task.duplicateCount;
        document.querySelector("#task-duration").textContent = task.durationMillis == null ? "—" : `${task.durationMillis} ms`;
        document.querySelector("#task-items").innerHTML = items.map(item => `<tr>
            <td>${item.ID}</td><td><span class="status ${item.STATUS === "CREATED" ? "on" : "off"}">${item.STATUS}</span></td>
            <td class="url-cell">${A.escape(item.NORMALIZED_URL || item.ORIGINAL_URL)}</td>
            <td>${item.ARTICLE_ID ? `<a href="/articles/${item.ARTICLE_ID}">文章 #${item.ARTICLE_ID}</a>` : "—"}</td></tr>`).join("");
        document.querySelector("#task-item-empty").hidden = items.length > 0;
    } catch (error) { A.message(error.message, true); }
});
