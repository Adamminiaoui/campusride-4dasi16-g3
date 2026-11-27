"use strict";

const $ = (id) => document.getElementById(id);
const apiBase = "/dev/cities";
const state = { cities: [], busy: false };

function notify(message, error = false) {
  $("notice").textContent = message;
  $("notice").classList.toggle("error", error);
  $("notice").hidden = false;
}

async function api(path = "", method = "GET", body) {
  const response = await fetch(apiBase + path, {
    method,
    headers: body ? { "Content-Type": "application/json", Accept: "application/json" } : { Accept: "application/json" },
    body: body ? JSON.stringify(body) : undefined
  });
  if (response.status === 204) return null;
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!response.ok) throw new Error(data?.message || `Erreur HTTP ${response.status}. Vérifiez que Ride est lancé avec le profil crud.`);
  return data;
}

async function run(action) {
  if (state.busy) return;
  state.busy = true;
  document.querySelectorAll("button").forEach((b) => { b.disabled = true; });
  try { await action(); }
  catch (error) { notify(error.message === "Failed to fetch" ? "Impossible de joindre Ride. Vérifiez que le service est démarré." : error.message, true); }
  finally { state.busy = false; document.querySelectorAll("button").forEach((b) => { b.disabled = false; }); }
}

function cell(row, value) {
  const td = document.createElement("td");
  if (value instanceof Node) td.append(value); else td.textContent = String(value);
  row.append(td);
}

function actionButton(label, action, id, danger = false) {
  const b = document.createElement("button");
  b.type = "button"; b.className = `action${danger ? " delete" : ""}`;
  b.textContent = label; b.dataset.action = action; b.dataset.id = id;
  return b;
}

function render() {
  const q = $("search").value.trim().toLowerCase();
  const rows = state.cities.filter((c) => [c.id, c.name, c.governorate].some((v) => String(v).toLowerCase().includes(q)));
  $("rows").replaceChildren();
  for (const city of rows) {
    const tr = document.createElement("tr");
    cell(tr, `#${city.id}`); cell(tr, city.name); cell(tr, city.governorate);
    const actions = document.createElement("div"); actions.className = "actions";
    actions.append(actionButton("Modifier", "edit", city.id), actionButton("Supprimer", "delete", city.id, true));
    cell(tr, actions);
    $("rows").append(tr);
  }
  $("city-count").textContent = state.cities.length;
  $("empty").hidden = rows.length > 0;
  $("empty").querySelector("h3").textContent = q ? "Aucune ville ne correspond." : "Aucune ville pour l’instant.";
}

async function load() { state.cities = await api(); render(); }

function resetForm() {
  $("city-form").reset(); $("city-id").value = "";
  $("form-title").textContent = "Nouvelle ville";
  $("submit").textContent = "Ajouter la ville";
}

function confirmDelete(title) {
  const dialog = $("delete-dialog");
  $("delete-title").textContent = title;
  return new Promise((resolve) => {
    dialog.returnValue = "cancel";
    dialog.addEventListener("close", () => resolve(dialog.returnValue === "confirm"), { once: true });
    dialog.showModal();
  });
}

$("city-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    const id = $("city-id").value;
    const body = { name: $("name").value, governorate: $("governorate").value };
    const city = await api(id ? `/${id}` : "", id ? "PUT" : "POST", body);
    resetForm(); await load();
    notify(id ? `Ville #${city.id} mise à jour.` : `Ville #${city.id} (${city.name}) ajoutée.`);
  });
});

$("rows").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-action]"); if (!button || state.busy) return;
  const city = state.cities.find((c) => String(c.id) === button.dataset.id);
  if (button.dataset.action === "edit") {
    $("city-id").value = city.id; $("name").value = city.name; $("governorate").value = city.governorate;
    $("form-title").textContent = `Modifier la ville #${city.id}`;
    $("submit").textContent = "Enregistrer les modifications";
    $("name").focus();
  } else if (await confirmDelete(`Supprimer ${city.name} ?`)) {
    await run(async () => { await api(`/${city.id}`, "DELETE"); if ($("city-id").value === String(city.id)) resetForm(); await load(); notify(`Ville ${city.name} supprimée.`); });
  }
});

$("search").addEventListener("input", render);
$("reset").addEventListener("click", resetForm);
$("refresh").addEventListener("click", () => run(async () => { await load(); notify("Données actualisées."); }));
run(load);
