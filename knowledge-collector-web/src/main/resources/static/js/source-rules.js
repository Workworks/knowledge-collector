window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const root = document.querySelector("main");
    const sourceId = root.dataset.sourceId;
    const $ = id => document.querySelector(`#${id}`);
    async function load() {
        try {
            const rules = await A.request(`/api/v1/sources/${sourceId}/rules`);
            $("rule-rows").innerHTML = rules.map(rule => `<tr><td>v${rule.version}</td>
            <td><code>${A.escape(rule.listSelector)}</code><small>${A.escape(rule.contentSelector)}</small></td>
            <td><span class="status ${rule.enabled ? "on" : "off"}">${rule.enabled ? "启用" : "历史"}</span></td>
            <td>${A.escape(rule.createdAt)}</td><td>${rule.enabled ? "—" :
                `<button data-activate="${rule.id}">启用此版本</button>`}</td></tr>`).join("");
        } catch (error) { A.message(error.message, true); }
    }
    $("rule-form").addEventListener("submit", async event => {
        event.preventDefault();
        const body = {
            listSelector: $("list-selector").value, linkSelector: $("link-selector").value,
            titleSelector: $("title-selector").value, contentSelector: $("content-selector").value,
            authorSelector: $("author-selector").value, publishTimeSelector: $("time-selector").value,
            datePattern: $("date-pattern").value, removeSelectors: $("remove-selectors").value,
            enabled: $("rule-enabled").checked
        };
        try {
            await A.request(`/api/v1/sources/${sourceId}/rules`,
                {method: "POST", body: JSON.stringify(body)});
            A.message("规则新版本已保存"); await load();
        } catch (error) { A.message(error.message, true); }
    });
    $("rule-test").onclick = async () => {
        try {
            const result = await A.request(`/api/v1/sources/${sourceId}/rules/test`, {method: "POST"});
            A.message(`规则测试成功，解析到 ${result.entryCount} 篇文章`);
        } catch (error) { A.message(error.message, true); }
    };
    $("rule-rows").onclick = async event => {
        const button = event.target.closest("[data-activate]");
        if (!button) return;
        try {
            await A.request(`/api/v1/sources/${sourceId}/rules/${button.dataset.activate}/active`, {method: "PUT"});
            await load();
        } catch (error) { A.message(error.message, true); }
    };
    await load();
});
