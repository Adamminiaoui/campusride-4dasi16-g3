"use strict";
const el = id => document.getElementById(id);
let rides = [], bookings = [], editingId = null, busy = false;
const labels = {PENDING: "En attente", CONFIRMED: "Confirmée", CANCELLED: "Annulée", COMPLETED: "Terminée"};
const message = (text, error = false) => { el("message").textContent = text; el("message").classList.toggle("error", error); el("message").hidden = false; };
async function request(path, method = "GET", body) {
  const response = await fetch(path, { method, headers: body ? {"Content-Type":"application/json"} : {}, body: body ? JSON.stringify(body) : undefined });
  if (response.status === 204) return null;
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || "Erreur de requête");
  return result;
}
async function run(fn) { if (busy) return; busy = true; el("submit-reservation").disabled = true; try { await fn(); } catch(e) { message(e.message || "Service indisponible", true); } finally { busy = false; el("submit-reservation").disabled = false; } }
function selectedRide() { return rides.find(r => String(r.id) === el("ride").value); }
function updatePrice() { const r = selectedRide(); if (!r) return; el("ride-details").textContent = `${r.pricePerSeat} par place · ${r.seats} places dans le trajet fictif`; el("seats").max = r.seats; el("total").textContent = `Total estimé : ${(Number(r.pricePerSeat) * Number(el("seats").value)).toFixed(2)}`; }
async function refresh() {
  bookings = await request("/dev/bookings"); const passenger = Number(el("passenger").value);
  el("rows").replaceChildren(); const mine = bookings.filter(b => b.passengerId === passenger);
  el("empty").hidden = mine.length > 0;
  for (const booking of mine) {
    const row = document.createElement("tr");
    const ride = rides.find(r => r.id === booking.rideId); const rideLabel = ride ? `${ride.departure} → ${ride.destination}` : `Trajet #${booking.rideId}`;
    for (const value of [`#${booking.id}`, rideLabel, booking.seats, Number(booking.totalPrice).toFixed(2), labels[booking.status] || booking.status]) { const cell = document.createElement("td"); cell.textContent = value; row.append(cell); }
    const cell = document.createElement("td"); const actions = document.createElement("div"); actions.className = "reservation-actions"; cell.append(actions);
    const detail = document.createElement("button"); detail.type = "button"; detail.className = "action"; detail.textContent = "Historique"; detail.setAttribute("aria-label", `Voir l’historique de la réservation ${booking.id}`); detail.onclick = () => run(() => showDetails(booking)); actions.append(detail);
    if (booking.status === "PENDING" && rides.some(r => r.id === booking.rideId)) {
      const edit = document.createElement("button"); edit.type = "button"; edit.className = "action"; edit.textContent = "Modifier";
      edit.setAttribute("aria-label", `Modifier la réservation ${booking.id}`); edit.onclick = () => { editingId = booking.id; el("ride").value = String(booking.rideId); el("ride").disabled = true; el("passenger").disabled = true; el("seats").value = booking.seats; el("form-title").textContent = `Modifier la réservation #${booking.id}`; el("submit-reservation").textContent = "Enregistrer les modifications"; el("reset-edit").hidden = false; updatePrice(); el("reserve").scrollIntoView({behavior:"smooth",block:"center"}); el("seats").focus({preventScroll:true}); }; actions.append(edit);
    }
    if (booking.status === "PENDING") {
      const del = document.createElement("button"); del.type = "button"; del.className = "action delete"; del.textContent = "Supprimer";
      del.setAttribute("aria-label", `Supprimer la réservation ${booking.id}`); del.onclick = () => { const dialog = el("delete-reservation"); dialog.returnValue = "cancel"; dialog.addEventListener("close", () => { if (dialog.returnValue === "confirm") run(async () => { await request(`/dev/standalone/bookings/${booking.id}?passengerId=${encodeURIComponent(booking.passengerId)}`, "DELETE"); if (editingId === booking.id) resetEdit(); el("details").textContent = "Aucune réservation sélectionnée."; await refresh(); message("Réservation de test et historique supprimés."); }); }, {once:true}); dialog.showModal(); }; actions.append(del);
    }
    row.append(cell); el("rows").append(row);
  }
}
async function showDetails(booking) {
  const history = await request(`/dev/bookings/${booking.id}/history`);
  const panel = el("details"); panel.replaceChildren();
  const heading = document.createElement("h3"); heading.textContent = `Réservation #${booking.id}`; panel.append(heading);
  const h = document.createElement("p"); h.textContent = history.map(x => `${x.previousStatus ? (labels[x.previousStatus] || x.previousStatus) : "Création"} → ${labels[x.newStatus] || x.newStatus} · ${x.changedAt.replace("T", " ").slice(0,16)} UTC`).join(" ; "); panel.append(h); panel.scrollIntoView({behavior:"smooth",block:"nearest"});
}
function resetEdit() { editingId = null; el("ride").disabled = false; el("passenger").disabled = false; el("seats").value = 1; el("form-title").textContent = "Nouvelle réservation"; el("submit-reservation").textContent = "Réserver"; el("reset-edit").hidden = true; updatePrice(); }
el("reset-edit").onclick = resetEdit;
el("reserve").onsubmit = e => { e.preventDefault(); run(async () => { const isEdit = editingId !== null; const b = await request(isEdit ? `/dev/standalone/bookings/${editingId}` : "/dev/standalone/bookings", isEdit ? "PUT" : "POST", {rideId:Number(el("ride").value), passengerId:Number(el("passenger").value), seats:Number(el("seats").value)}); resetEdit(); await refresh(); await showDetails(b); message(`Réservation #${b.id} ${isEdit ? "modifiée" : "créée en attente"}. Prix calculé : ${Number(b.totalPrice).toFixed(2)}.`); }); };
el("ride").onchange = updatePrice; el("seats").oninput = updatePrice;
el("passenger").onchange = () => run(refresh); el("refresh").onclick = () => run(refresh);
run(async () => { rides = await request("/dev/standalone/rides"); for (const r of rides) el("ride").add(new Option(`${r.departure} → ${r.destination}`, r.id)); updatePrice(); await refresh(); });
