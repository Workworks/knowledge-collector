window.addEventListener("DOMContentLoaded", async () => {
    const A = window.AdminCommon;
    const $ = selector => document.querySelector(selector);
    let conversations = [];
    let activeId = null;

    const titleFrom = content => {
        const line = (content || "AI 生成资料").replace(/[#*`>]/g, "").split(/\r?\n/)
            .map(value => value.trim()).find(Boolean) || "AI 生成资料";
        return line.slice(0, 80);
    };

    function renderConversationList() {
        $("#conversation-list").innerHTML = conversations.map(item => `
            <button class="conversation-item ${item.id === activeId ? "active" : ""}"
                    data-conversation-id="${item.id}" type="button">
                <strong>${A.escape(item.title)}</strong><small>${A.escape(item.model || item.provider)}</small>
            </button>`).join("");
    }

    function messageHtml(message) {
        const assistant = message.role === "ASSISTANT";
        const saved = message.savedArticleId ? `<a class="saved-material" href="/articles/${message.savedArticleId}">已入库 #${message.savedArticleId}</a>` : "";
        const action = assistant && !message.savedArticleId ? `
            <form class="save-material-form" data-message-id="${message.id}">
                <input maxlength="500" value="${A.escape(titleFrom(message.content))}" aria-label="资料标题">
                <button class="button button-secondary button-small" type="submit">保存到资料库</button>
            </form>` : saved;
        return `<article class="chat-message ${assistant ? "assistant" : "user"}">
            <div class="message-avatar">${assistant ? "AI" : "我"}</div><div class="message-body">
                <div class="message-content">${A.escape(message.content).replace(/\n/g, "<br>")}</div>
                ${assistant ? `<div class="message-meta">${A.escape(message.model || "本地模型")} · ${message.durationMillis || 0} ms</div>${action}` : ""}
            </div></article>`;
    }

    function renderConversation(conversation) {
        activeId = conversation.id;
        $("#chat-empty").hidden = conversation.messages.length > 0;
        $("#chat-messages").innerHTML = conversation.messages.map(messageHtml).join("");
        renderConversationList();
        $("#chat-messages").scrollTop = $("#chat-messages").scrollHeight;
    }

    async function loadList(selectFirst = true) {
        conversations = await A.request("/api/v1/ai/chat/conversations");
        renderConversationList();
        if (selectFirst && conversations.length && !activeId) await openConversation(conversations[0].id);
    }

    async function openConversation(id) {
        renderConversation(await A.request(`/api/v1/ai/chat/conversations/${id}`));
    }

    async function createConversation() {
        const conversation = await A.request("/api/v1/ai/chat/conversations", {
            method:"POST", body:JSON.stringify({})
        });
        activeId = conversation.id;
        await loadList(false);
        renderConversation(conversation);
        $("#chat-input").focus();
    }

    $("#new-conversation").onclick = () => createConversation().catch(error => A.message(error.message, true));
    $("#conversation-list").onclick = event => {
        const button = event.target.closest("[data-conversation-id]");
        if (button) openConversation(Number(button.dataset.conversationId)).catch(error => A.message(error.message, true));
    };
    $("#chat-input").onkeydown = event => {
        if (event.ctrlKey && event.key === "Enter") $("#chat-form").requestSubmit();
    };
    $("#chat-form").onsubmit = async event => {
        event.preventDefault();
        const content = $("#chat-input").value.trim();
        if (!content) return;
        if (!activeId) await createConversation();
        $("#chat-send").disabled = true;
        $("#chat-send").textContent = "思考中…";
        $("#chat-input").value = "";
        try {
            await A.request(`/api/v1/ai/chat/conversations/${activeId}/messages`, {
                method:"POST", body:JSON.stringify({content})
            });
            await openConversation(activeId);
            await loadList(false);
        } catch (error) {
            $("#chat-input").value = content;
            A.message(error.message, true);
        } finally {
            $("#chat-send").disabled = false;
            $("#chat-send").textContent = "发送";
        }
    };
    $("#chat-messages").onsubmit = async event => {
        const form = event.target.closest(".save-material-form");
        if (!form) return;
        event.preventDefault();
        const button = form.querySelector("button");
        button.disabled = true;
        try {
            const saved = await A.request(`/api/v1/ai/chat/messages/${form.dataset.messageId}/save`, {
                method:"POST", body:JSON.stringify({title:form.querySelector("input").value})
            });
            await openConversation(activeId);
            A.message(`AI 材料已进入待审核资料库（#${saved.articleId}）`);
        } catch (error) {
            A.message(error.message, true);
            button.disabled = false;
        }
    };

    try { await loadList(); } catch (error) { A.message(error.message, true); }
});
