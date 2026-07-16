window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const rows = document.querySelector("#task-rows");
    try {
        const data = await A.request("/api/v1/tasks?page=0&size=100");
        rows.innerHTML = data.content.map(task => `<tr><td><a class="task-link" href="/tasks/${task.id}">${A.escape(task.taskNo)}</a></td>
            <td>${A.escape(task.sourceName)}</td><td><span class="status ${task.status === "SUCCESS" ? "on" : "off"}">${task.status}</span></td>
            <td><strong>${task.createdCount}</strong> / ${task.discoveredCount}</td><td>${task.duplicateCount}</td>
            <td>${task.durationMillis == null ? "—" : `${task.durationMillis} ms`}</td>
            <td><a class="button button-ghost button-small" href="/tasks/${task.id}">查看详情</a></td></tr>`).join("");
        document.querySelector("#task-empty").hidden = data.content.length > 0;
        document.querySelector("#task-count").textContent = `${data.totalElements} 个任务`;
    } catch (error) { A.message(error.message, true); }
});
