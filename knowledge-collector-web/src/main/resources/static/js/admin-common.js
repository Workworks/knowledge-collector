window.AdminCommon = {
  async request(url, options = {}) {
    const multipart = options.body instanceof FormData;
    const headers = {Accept: "application/json", "X-Correlation-Id": `web-admin-${Date.now()}`, ...(multipart ? {} : {"Content-Type": "application/json"}), ...(options.headers || {})};
    const response = await fetch(url, {...options, headers});
    if (response.status === 204) return null;
    const payload = await response.json().catch(() => ({}));
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
  escape(value) { const element = document.createElement("span"); element.textContent = value ?? ""; return element.innerHTML; }
};
