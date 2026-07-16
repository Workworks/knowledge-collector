(() => {
    "use strict";

    const form = document.querySelector("#request-form");
    const methodInput = document.querySelector("#request-method");
    const pathInput = document.querySelector("#request-path");
    const correlationIdInput = document.querySelector("#correlation-id");
    const bodyInput = document.querySelector("#request-body");
    const sendButton = document.querySelector("#send-request");
    const clearButton = document.querySelector("#clear-response");
    const responseMeta = document.querySelector("#response-meta");
    const responseOutput = document.querySelector("#response-output");
    const tabs = [...document.querySelectorAll(".response-tab")];
    let responseViews = {body: "", headers: ""};
    let activeView = "body";

    document.querySelectorAll(".preset-button").forEach((button) => {
        button.addEventListener("click", () => {
            methodInput.value = button.dataset.method;
            pathInput.value = button.dataset.path;
            bodyInput.value = "";
            pathInput.focus();
        });
    });

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            activeView = tab.dataset.view;
            tabs.forEach((item) => item.setAttribute("aria-selected", String(item === tab)));
            responseOutput.textContent = responseViews[activeView] || "无内容";
        });
    });

    clearButton.addEventListener("click", () => {
        responseViews = {body: "", headers: ""};
        responseMeta.innerHTML = '<span class="empty-response">尚未发送请求</span>';
        responseOutput.textContent = "选择一个预置请求，然后点击“发送请求”。";
        responseOutput.classList.remove("error-text");
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const method = methodInput.value;
        const path = pathInput.value.trim();

        if (!isSafeApplicationPath(path)) {
            showClientError("请求路径必须以单个 / 开头，且不能使用 // 或完整外部网址。");
            return;
        }

        const options = {
            method,
            headers: {
                "Accept": "application/json, text/html;q=0.9, text/plain;q=0.8"
            }
        };
        const correlationId = correlationIdInput.value.trim();
        if (correlationId) {
            options.headers["X-Correlation-Id"] = correlationId;
        }

        if (!["GET", "HEAD"].includes(method) && bodyInput.value.trim()) {
            if (!isValidJson(bodyInput.value)) {
                showClientError("请求体不是有效 JSON，请修正后重新发送。");
                return;
            }
            options.headers["Content-Type"] = "application/json";
            options.body = bodyInput.value;
        }

        sendButton.disabled = true;
        sendButton.textContent = "发送中…";
        responseOutput.classList.remove("error-text");
        responseOutput.textContent = "正在等待响应…";
        responseMeta.innerHTML = '<span class="badge">请求中</span>';
        const startedAt = performance.now();

        try {
            const response = await fetch(path, options);
            const elapsed = Math.round(performance.now() - startedAt);
            const rawBody = await response.text();
            responseViews = {
                body: formatBody(rawBody, response.headers.get("content-type")),
                headers: formatHeaders(response.headers)
            };
            renderResponseMeta(response, elapsed);
            responseOutput.textContent = responseViews[activeView] || "响应体为空";
            responseOutput.classList.toggle("error-text", !response.ok);
        } catch (error) {
            showClientError(`请求失败：${error.message}。请确认应用仍在运行，并检查浏览器控制台。`);
        } finally {
            sendButton.disabled = false;
            sendButton.textContent = "发送请求";
        }
    });

    function isSafeApplicationPath(path) {
        return path.startsWith("/") && !path.startsWith("//") && !path.includes("\\");
    }

    function isValidJson(value) {
        try {
            JSON.parse(value);
            return true;
        } catch {
            return false;
        }
    }

    function formatBody(body, contentType) {
        if (!body) {
            return "";
        }
        if (contentType && contentType.includes("json")) {
            try {
                return JSON.stringify(JSON.parse(body), null, 2);
            } catch {
                return body;
            }
        }
        return body;
    }

    function formatHeaders(headers) {
        return [...headers.entries()]
            .sort(([left], [right]) => left.localeCompare(right))
            .map(([name, value]) => `${name}: ${value}`)
            .join("\n");
    }

    function renderResponseMeta(response, elapsed) {
        const statusClass = response.ok ? "badge-success" : "badge-danger";
        const correlationId = response.headers.get("X-Correlation-Id") || "未返回";
        responseMeta.innerHTML = `
            <span class="badge ${statusClass}">${escapeHtml(`${response.status} ${response.statusText}`)}</span>
            <span class="badge">${elapsed} ms</span>
            <span class="badge">关联编号：${escapeHtml(correlationId)}</span>
        `;
    }

    function showClientError(message) {
        responseViews = {body: message, headers: ""};
        activeView = "body";
        tabs.forEach((tab) => tab.setAttribute("aria-selected", String(tab.dataset.view === "body")));
        responseMeta.innerHTML = '<span class="badge badge-danger">请求未发送</span>';
        responseOutput.textContent = message;
        responseOutput.classList.add("error-text");
    }

    function escapeHtml(value) {
        return value
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();
