window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const $ = id => document.querySelector(`#${id}`);

    async function loadSchedules() {
        const schedules = await A.request("/api/v1/operations/schedules");
        $("schedule-rows").innerHTML = schedules.map(item => `<tr>
            <td><strong>${A.escape(item.sourceName)}</strong><small>#${item.sourceId}</small></td>
            <td><span class="status ${item.enabled ? "on" : "off"}">${item.enabled ? "已启用" : "已停用"}</span></td>
            <td><input class="inline-number" data-interval="${item.sourceId}" type="number" min="1" max="10080" value="${item.intervalMinutes}"> 分钟</td>
            <td>${item.nextRunAt || "—"}</td><td>${item.lastRunAt || "—"}</td>
            <td class="row-actions"><button data-schedule="${item.sourceId}" data-enabled="${!item.enabled}">${item.enabled ? "停用" : "启用"}</button>
            <button data-save="${item.sourceId}">保存周期</button></td></tr>`).join("");
    }

    async function loadBackups() {
        const backups = await A.request("/api/v1/operations/backups");
        $("backup-rows").innerHTML = backups.map(item => `<tr><td><strong>${A.escape(item.name)}</strong>
            <small>${A.escape(item.relativePath)}</small></td><td>${(item.sizeBytes / 1024).toFixed(1)} KB</td>
            <td><span class="status on">${item.status}</span></td><td>${item.createdAt}</td></tr>`).join("");
        $("backup-empty").hidden = backups.length > 0;
    }

    $("schedule-rows").onclick = async event => {
        const button = event.target.closest("button");
        if (!button) return;
        const id = button.dataset.schedule || button.dataset.save;
        const interval = Number(document.querySelector(`[data-interval="${id}"]`).value);
        const currentEnabled = button.dataset.schedule ? button.dataset.enabled === "true" :
            document.querySelector(`[data-schedule="${id}"]`).textContent.includes("停用");
        try {
            await A.request(`/api/v1/operations/schedules/${id}`, {
                method:"PUT", body:JSON.stringify({enabled:currentEnabled, intervalMinutes:interval})
            });
            A.message("调度配置已更新"); await loadSchedules();
        } catch (error) { A.message(error.message, true); }
    };
    $("run-due").onclick = async () => {
        await A.request("/api/v1/operations/schedules/run-due", {method:"POST"});
        A.message("到期调度检查完成"); await loadSchedules();
    };
    $("recover-stale").onclick = async () => {
        try {
            const count = await A.request("/api/v1/operations/tasks/recover-stale", {method:"POST"});
            A.message(count > 0 ? `已回收 ${count} 个超时任务` : "没有需要回收的超时任务");
        } catch (error) { A.message(error.message, true); }
    };
    $("create-backup").onclick = async event => {
        event.currentTarget.disabled = true;
        try { await A.request("/api/v1/operations/backups", {method:"POST"});
            A.message("本地备份已创建"); await loadBackups();
        } catch (error) { A.message(error.message, true); }
        finally { event.currentTarget.disabled = false; }
    };
    try { await Promise.all([loadSchedules(), loadBackups()]); } catch (error) { A.message(error.message, true); }
});
