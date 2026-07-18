(() => {
  const A = window.AdminCommon, form = document.querySelector("#user-form"), box = document.querySelector("#users-table"), dialog = document.querySelector("#password-dialog");
  let users = [];
  const reset = () => { form.reset(); form.id.value = ""; form.enabled.checked = true; form.username.disabled = false; };
  async function load() {
    try {
      const keyword = new FormData(document.querySelector("#user-search")).get("keyword") || "";
      const page = await A.request(`/api/v1/users?keyword=${encodeURIComponent(keyword)}&page=0&size=100`); users = page.content;
      box.innerHTML = `<div class="table-scroll"><table><thead><tr><th>用户</th><th>角色</th><th>状态</th><th>最近登录</th><th>操作</th></tr></thead><tbody>${users.map(u => `<tr><td><strong>${A.escape(u.displayName)}</strong><small>${A.escape(u.username)}</small></td><td>${u.role}</td><td>${u.enabled ? "已启用" : "已停用"}</td><td>${u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : "从未登录"}</td><td><button class="button" data-edit="${u.id}">编辑</button> <button class="button" data-reset="${u.id}">重置密码</button></td></tr>`).join("")}</tbody></table></div>`;
    } catch (e) { box.textContent = e.message; A.message(e.message, true); }
  }
  form.addEventListener("submit", async e => { e.preventDefault(); const d = Object.fromEntries(new FormData(form)); const id = d.id; d.enabled = form.enabled.checked; try { await A.request(id ? `/api/v1/users/${id}` : "/api/v1/users", {method: id ? "PUT" : "POST", body: JSON.stringify(d)}); A.message("用户已保存"); reset(); await load(); } catch (x) { A.message(x.message, true); } });
  box.addEventListener("click", e => { const edit = e.target.dataset.edit, resetId = e.target.dataset.reset; if (edit) { const u = users.find(x => String(x.id) === edit); Object.assign(form, {}); form.id.value=u.id; form.username.value=u.username; form.username.disabled=true; form.displayName.value=u.displayName; form.role.value=u.role; form.enabled.checked=u.enabled; form.password.value=""; scrollTo({top:0,behavior:"smooth"}); } if (resetId) { dialog.querySelector("[name=id]").value=resetId; dialog.showModal(); } });
  document.querySelector("#password-form").addEventListener("submit", async e => { e.preventDefault(); const d=Object.fromEntries(new FormData(e.target)); try { await A.request(`/api/v1/users/${d.id}/reset-password`, {method:"POST",body:JSON.stringify({password:d.password})}); dialog.close(); e.target.reset(); A.message("密码已重置"); } catch(x){ A.message(x.message,true); } });
  dialog.querySelector("[data-close]").addEventListener("click", () => dialog.close()); document.querySelector("#clear-user").addEventListener("click", reset); document.querySelector("#user-search").addEventListener("submit", e => {e.preventDefault();load();}); load();
})();
