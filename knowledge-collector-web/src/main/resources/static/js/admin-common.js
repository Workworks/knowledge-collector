window.AdminCommon = {
    async request(url, options = {}) {
        const response = await fetch(url, {
            headers: {"Accept": "application/json", "Content-Type": "application/json",
                "X-Correlation-Id": `web-admin-${Date.now()}`}, ...options
        });
        if (response.status === 204) return null;
        const payload = await response.json();
        if (!response.ok || payload.success === false) {
            const details = payload.error?.fieldErrors?.map(item => `${item.field}: ${item.message}`).join("；");
            throw new Error(details || payload.error?.message || `HTTP ${response.status}`);
        }
        return payload.data;
    },
    message(text, error = false) {
        const element = document.querySelector("#page-message");
        if (!element) return;
        element.textContent = text; element.hidden = false;
        element.className = `page-message ${error ? "message-error" : "message-success"}`;
    },
    escape(value) {
        const element = document.createElement("span");
        element.textContent = value ?? "";
        return element.innerHTML;
    }
};
