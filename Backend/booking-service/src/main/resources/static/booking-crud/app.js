"use strict";

const $ = (id) => document.getElementById(id);
const apiBase = "/dev/bookings";
const state = { bookings: [], history: [], bookingId: "", busy: false };
const statusNames = { PENDING: "En attente", CONFIRMED: "Confirmée", CANCELLED: "Annulée", COMPLETED: "Terminée" };

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
  if (!response.ok) throw new Error(data?.message || `Erreur HTTP ${response.status}. Vérifiez que Booking est lancé avec le profil crud.`);
  return data;
}

async function run(action) {
  if (state.busy) return;
  state.busy = true;
  $("history-booking").disabled = true;
  document.querySelectorAll("button").forEach((button) => { button.disabled = true; });
  try { await action(); }
  catch (error) { notify(error.message === "Failed to fetch" ? "Impossible de joindre Booking. Vérifiez que le service est démarré." : error.message, true); }
  finally {
    state.busy = false;
    $("history-booking").disabled = false;
    document.querySelectorAll("button").forEach((button) => { button.disabled = false; });
    $("history-fields").disabled = !state.bookingId;
  }
}

function badge(status) {
  const span = document.createElement("span");
  span.className = `badge ${status || ""}`;
  span.textContent = status || "—";
  if (status) span.title = statusNames[status];
  return span;
}

function cell(row, value, subtext) {
  const td = document.createElement("td");
  if (value instanceof Node) td.append(value); else td.textContent = String(value);
  if (subtext) { const small = document.createElement("small"); small.textContent = subtext; td.append(small); }
  row.append(td);
  return td;
}

function actionButton(label, action, id, danger = false) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `action${danger ? " delete" : ""}`;
  button.textContent = label;
  button.dataset.action = action;
  button.dataset.id = id;
  return button;
}

function dateLabel(value) { return value ? value.replace("T", " ").slice(0, 19) : "—"; }

function renderBookings() {
  const query = $("booking-search").value.trim().toLowerCase();
  const rows = state.bookings.filter((b) => [b.id, b.rideId, b.driverId, b.passengerId, b.status].some((v) => String(v).toLowerCase().includes(query)));
  $("booking-rows").replaceChildren();
  for (const booking of rows) {
    const row = document.createElement("tr");
    cell(row, `#${booking.id}`, dateLabel(booking.createdAt));
    cell(row, `#${booking.rideId}`);
    cell(row, `#${booking.driverId}`, `Passager #${booking.passengerId}`);
    cell(row, booking.seats);
    cell(row, Number(booking.totalPrice).toFixed(2));
    cell(row, badge(booking.status));
    const actions = document.createElement("div"); actions.className = "actions";
    actions.append(actionButton("Modifier", "edit", booking.id), actionButton("Historique", "history", booking.id), actionButton("Supprimer", "delete", booking.id, true));
    cell(row, actions);
    $("booking-rows").append(row);
  }
  $("booking-count").textContent = state.bookings.length;
  $("booking-empty").hidden = rows.length > 0;
  $("booking-empty").querySelector("h3").textContent = query ? "Aucune réservation ne correspond." : "Votre première réservation commence ici.";
}

function renderHistory() {
  $("history-rows").replaceChildren();
  for (const history of state.history) {
    const row = document.createElement("tr");
    cell(row, `#${history.id}`, dateLabel(history.changedAt));
    cell(row, badge(history.previousStatus));
    cell(row, badge(history.newStatus));
    cell(row, `#${history.changedBy}`);
    const actions = document.createElement("div"); actions.className = "actions";
    actions.append(actionButton("Modifier", "edit", history.id), actionButton("Supprimer", "delete", history.id, true));
    cell(row, actions); $("history-rows").append(row);
  }
  $("history-empty").hidden = state.history.length > 0;
  $("history-count").textContent = `${state.history.length} entrée${state.history.length > 1 ? "s" : ""}`;
  $("history-heading-id").textContent = state.bookingId ? `#${state.bookingId}` : "";
  $("history-fields").disabled = !state.bookingId;
  const selected = state.bookings.find((b) => String(b.id) === state.bookingId);
  $("selected-booking-info").textContent = selected ? `Trajet #${selected.rideId} · Passager #${selected.passengerId} · ${statusNames[selected.status]}` : "Sélectionnez une réservation pour consulter son historique.";
}

async function loadHistory() {
  state.history = state.bookingId ? await api(`/${state.bookingId}/history`) : [];
  renderHistory();
}

async function loadBookings() {
  state.bookings = await api();
  if (!state.bookings.some((b) => String(b.id) === state.bookingId)) state.bookingId = "";
  const select = $("history-booking");
  select.replaceChildren(new Option("Choisir une réservation", ""));
  state.bookings.forEach((b) => select.add(new Option(`#${b.id} · Trajet #${b.rideId} · Passager #${b.passengerId}`, b.id)));
  select.value = state.bookingId;
  renderBookings();
  await loadHistory();
}

function resetBooking() {
  $("booking-form").reset(); $("booking-id").value = "";
  $("booking-form-title").textContent = "Nouvelle réservation";
  $("booking-submit").textContent = "Créer la réservation";
}

function resetHistory() {
  $("history-form").reset(); $("history-id").value = "";
  $("history-form-title").textContent = "Nouvelle entrée";
  $("history-submit").textContent = "Ajouter l’entrée";
}

function switchTab(history) {
  $("bookings-panel").hidden = history;
  $("history-panel").hidden = !history;
  $("bookings-tab").classList.toggle("active", !history);
  $("history-tab").classList.toggle("active", history);
  $("bookings-tab").removeAttribute("aria-current"); $("history-tab").removeAttribute("aria-current");
  $(history ? "history-tab" : "bookings-tab").setAttribute("aria-current", "page");
}

function confirmDelete(title, description) {
  const dialog = $("delete-dialog");
  $("delete-title").textContent = title;
  $("delete-description").textContent = description;
  return new Promise((resolve) => {
    dialog.returnValue = "cancel";
    dialog.addEventListener("close", () => resolve(dialog.returnValue === "confirm"), { once: true });
    dialog.showModal();
  });
}

$("booking-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    const id = $("booking-id").value;
    const form = new FormData(event.target);
    const body = Object.fromEntries(["rideId", "driverId", "passengerId", "seats", "changedBy"].map((name) => [name, Number(form.get(name))]));
    body.totalPrice = form.get("totalPrice"); body.status = form.get("status");
    const booking = await api(id ? `/${id}` : "", id ? "PUT" : "POST", body);
    state.bookingId = String(booking.id);
    resetBooking(); resetHistory();
    await loadBookings();
    notify(id ? `Réservation #${booking.id} mise à jour. L’historique est complété si le statut a changé.` : `Réservation #${booking.id} créée avec son entrée d’historique.`);
  });
});

$("booking-rows").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-action]"); if (!button || state.busy) return;
  const booking = state.bookings.find((b) => String(b.id) === button.dataset.id);
  if (button.dataset.action === "edit") {
    $("booking-id").value = booking.id;
    for (const name of ["rideId", "driverId", "passengerId", "seats", "totalPrice", "status"]) $(name).value = booking[name];
    $("changedBy").value = "";
    $("booking-form-title").textContent = `Modifier la réservation #${booking.id}`;
    $("booking-submit").textContent = "Enregistrer les modifications";
    $("rideId").focus();
  } else if (button.dataset.action === "history") {
    await run(async () => { state.bookingId = String(booking.id); $("history-booking").value = state.bookingId; resetHistory(); await loadHistory(); switchTab(true); });
  } else if (await confirmDelete(`Supprimer la réservation #${booking.id} ?`, "La réservation et toutes ses entrées d’historique seront supprimées définitivement.")) {
    await run(async () => { await api(`/${booking.id}`, "DELETE"); if ($("booking-id").value === String(booking.id)) resetBooking(); resetHistory(); await loadBookings(); notify(`Réservation #${booking.id} et son historique supprimés.`); });
  }
});

$("history-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    const id = $("history-id").value;
    const body = { previousStatus: $("previousStatus").value || null, newStatus: $("newStatus").value, changedBy: Number($("history-author").value), changedAt: $("history-date").value || null };
    await api(`/${state.bookingId}/history${id ? `/${id}` : ""}`, id ? "PUT" : "POST", body);
    resetHistory(); await loadHistory(); notify(id ? "Entrée d’historique mise à jour." : "Entrée d’historique ajoutée.");
  });
});

$("history-rows").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-action]"); if (!button || state.busy) return;
  const history = state.history.find((h) => String(h.id) === button.dataset.id);
  if (button.dataset.action === "edit") {
    $("history-id").value = history.id; $("previousStatus").value = history.previousStatus || "";
    $("newStatus").value = history.newStatus; $("history-author").value = history.changedBy;
    $("history-date").value = history.changedAt.slice(0, 19);
    $("history-form-title").textContent = `Modifier l’entrée #${history.id}`;
    $("history-submit").textContent = "Enregistrer les modifications";
    $("previousStatus").focus();
  } else if (await confirmDelete(`Supprimer l’entrée #${history.id} ?`, "Cette opération supprime uniquement cette entrée d’historique. La réservation est conservée.")) {
    await run(async () => { await api(`/${state.bookingId}/history/${history.id}`, "DELETE"); if ($("history-id").value === String(history.id)) resetHistory(); await loadHistory(); notify("Entrée d’historique supprimée."); });
  }
});

$("history-booking").addEventListener("change", () => run(async () => { state.bookingId = $("history-booking").value; resetHistory(); await loadHistory(); }));
$("booking-search").addEventListener("input", renderBookings);
$("booking-reset").addEventListener("click", resetBooking);
$("history-reset").addEventListener("click", resetHistory);
$("bookings-tab").addEventListener("click", () => switchTab(false));
$("history-tab").addEventListener("click", () => switchTab(true));
$("refresh").addEventListener("click", () => run(async () => { await loadBookings(); notify("Données actualisées."); }));
run(loadBookings);
