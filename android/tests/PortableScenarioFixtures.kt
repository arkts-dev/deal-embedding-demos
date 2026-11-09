package dev.deal.connectors.tests

/** Injected model answers only: never packaged as library policy or generation templates. */
internal object PortableScenarioFixtures {
    private fun section(fields:String, functions:String, view:String, classes:String=""):String = "<classes>$classes</classes>\n<fields>$fields</fields>\n<functions>$functions</functions>\n<view>$view</view>"
    val food=section("""
c_menu: string = "[]";
c_windows: string = "[]";
c_vegan: number = 2;
c_vegetarian: number = 2;
c_meat: number = 2;
c_budget: number = 80;
c_deadline: number = 1110;
c_window: number = 0;
c_message: string = "Load menu and delivery";
""","""
function foodRows(text: string): unknown[] { try { return json.parse("{\"items\":" + text + "}").items as unknown[]; } catch (caught) { return []; } }
function foodItem(text: string, kind: number): Record<string, unknown> {
 for (let item of foodRows(text)) { let row: Record<string, unknown> = item as Record<string, unknown>;
  if ((row.diameterMm as number) === 300) {
   if (kind === 0 && (row.vegan as boolean)) { return row; }
   if (kind === 1 && !(row.vegan as boolean) && (row.vegetarian as boolean)) { return row; }
   if (kind === 2 && !(row.vegetarian as boolean)) { return row; }
  }
 } return {id: "", name: "Unavailable", cents: 0, vegan: false};
}
function foodSummary(state: AppState): FoodSummary {
 let total: number = 0; let vegan: number = 0; let count: number = 0; let known: boolean = true; let names: string = ""; let kind: number = 0;
 for (let qty of [state.c_vegan, state.c_vegetarian, state.c_meat]) {
  let item: Record<string, unknown> = foodItem(state.c_menu, kind); kind = kind + 1;
  names = names + (item.name as string) + "; ";
  if ((item.id as string) === "" || qty < 0 || math.floor(qty) !== qty) { known = false; }
  total = total + (item.cents as number) * qty; count = count + qty;
  if (item.vegan as boolean) { vegan = vegan + qty; }
 }
 let windows: unknown[] = foodRows(state.c_windows); let onTime: boolean = false; let delivery: string = "Unavailable";
 if (state.c_window >= 0 && state.c_window < windows.length && math.floor(state.c_window) === state.c_window) {
  let window: Record<string, unknown> = windows[state.c_window] as Record<string, unknown>;
  total = total + (window.deliveryCents as number); onTime = window.meetsRequirement as boolean;
  delivery = (window.from as string) + "–" + (window.until as string);
 }
 let status: string = "Not feasible";
 if (known && state.c_message === "" && count === 6 && vegan >= 2 && total <= state.c_budget * 100 && onTime) { status = "Feasible"; }
 return {total: total / 100, count: count, vegan: vegan, status: status, delivery: delivery, names: names};
}
function c_load(state: AppState, value: number): AppState {
 sdk.call("host/pizza.menu", [], "c_menuAnswer");
 return {c_menu: "[]", c_windows: "[]", c_message: "Loading menu" __PASSTHROUGH__};
}
function c_menuAnswer(state: AppState, value: string): AppState {
 sdk.call("host/pizza.delivery", ["requiredBy", json.stringify(ui.clockToString(state.c_deadline)) as string], "c_deliveryAnswer");
 return {c_menu: value, c_message: "Loading delivery" __PASSTHROUGH__};
}
function c_deliveryAnswer(state: AppState, value: string): AppState {
 let message: string = "";
 if (foodRows(value).length === 0 || foodRows(state.c_menu).length === 0) { message = "Read failed; retry"; }
 return {c_windows: value, c_message: message __PASSTHROUGH__};
}
function c_deadline(state: AppState, value: number): AppState {
 sdk.call("host/pizza.delivery", ["requiredBy", json.stringify(ui.clockToString(value)) as string], "c_deliveryAnswer");
 return {c_deadline: value, c_windows: "[]", c_message: "Loading delivery" __PASSTHROUGH__};
}
function c_vegan(state: AppState, value: number): AppState { return {c_vegan: value __PASSTHROUGH__}; }
function c_vegetarian(state: AppState, value: number): AppState { return {c_vegetarian: value __PASSTHROUGH__}; }
function c_meat(state: AppState, value: number): AppState { return {c_meat: value __PASSTHROUGH__}; }
function c_budget(state: AppState, value: number): AppState { return {c_budget: value __PASSTHROUGH__}; }
function c_window(state: AppState, value: number): AppState { return {c_window: value __PASSTHROUGH__}; }
""","""
let summary: FoodSummary = foodSummary(state);
sections.push(ui.Section("content", "Crew dinner", "", [ui.Button("default", "Load food", "c_load", 0, true), ui.Text("default", state.c_message), ui.Text("default", summary.names), ui.NumberField("default", "Vegan quantity", state.c_vegan, 0, 20, "c_vegan"), ui.NumberField("default", "Vegetarian quantity", state.c_vegetarian, 0, 20, "c_vegetarian"), ui.NumberField("default", "Meat quantity", state.c_meat, 0, 20, "c_meat"), ui.NumberField("default", "Budget EUR", state.c_budget, 0, 500, "c_budget"), ui.TimeField("default", "Required by", state.c_deadline, "c_deadline"), ui.NumberField("default", "Delivery window", state.c_window, 0, 2, "c_window")]));
sections.push(ui.Section("summary", "Dinner check", "", [ui.Stat("default", "Total EUR", ui.formatNumber(summary.total, 2), "", "chart"), ui.Stat("default", "Pizzas", ui.numberToString(summary.count), "", "chart"), ui.Stat("default", "Vegan", ui.numberToString(summary.vegan), "", "chart"), ui.Text("default", summary.delivery), ui.Text("default", summary.status)]));
""","class FoodSummary { total: number; count: number; vegan: number; status: string; delivery: string; names: string; }")

    val equipment=section("""
c_catalogue: string = "[]";
c_packageQuote: string = "{}";
c_separateQuote: string = "{}";
c_from: string = "2026-10-23T14:15:00Z";
c_until: string = "2026-10-23T20:30:00Z";
c_budget: number = 20;
c_message: string = "Read catalogue and whole-basket quotes";
""","""
function equipmentRows(text: string): unknown[] { try { return json.parse("{\"items\":" + text + "}").items as unknown[]; } catch (caught) { return []; } }
function equipmentBaskets(text: string): Baskets {
 let rows: unknown[] = equipmentRows(text); let stand: string = ""; let cable: string = ""; let pack: string = "";
 for (let item of rows) { let row: Record<string, unknown> = item as Record<string, unknown>; let a: Record<string, unknown> = row.attributes as Record<string, unknown>;
  if ((row.available as number) >= 1) {
   if ((a.kind as string) === "stand" && (a.adjustable as boolean) && (a.clipAdapter as boolean)) { stand = row.id as string; }
   if ((a.kind as string) === "cable" && (a.balanced as boolean) && (a.connectorFrom as string) === "XLR female" && (a.connectorTo as string) === "XLR male" && (a.lengthMm as number) >= 10000) { cable = row.id as string; }
  }
 }
 for (let item of rows) { let row: Record<string, unknown> = item as Record<string, unknown>; let a: Record<string, unknown> = row.attributes as Record<string, unknown>;
  if ((a.kind as string) === "package" && (row.available as number) >= 1) {
   let hasStand: boolean = false; let hasCable: boolean = false;
   for (let part of (row.components as unknown[])) { let component: Record<string, unknown> = part as Record<string, unknown>;
    if ((component.itemId as string) === stand && (component.quantity as number) >= 1) { hasStand = true; }
    if ((component.itemId as string) === cable && (component.quantity as number) >= 1) { hasCable = true; }
   }
   if (hasStand && hasCable) { pack = row.id as string; }
  }
 }
 let separate: string = ""; if (stand !== "" && cable !== "") { separate = stand + ":1," + cable + ":1"; }
 let packageBasket: string = ""; if (pack !== "") { packageBasket = pack + ":1"; }
 return {packageBasket: packageBasket, separate: separate};
}
function quoteSummary(text: string, budget: number): QuoteSummary {
 try { let row: Record<string, unknown> = json.parse(text); let hire: number = (row.cents as number) / 100; let deposit: number = (row.depositCents as number) / 100;
  let status: string = "Unavailable"; if ((row.available as boolean) && (row.expiresInMinutes as number) > 0) { status = "Over budget"; if (hire <= budget) { status = "Feasible"; } }
  return {hire: hire, deposit: deposit, status: status};
 } catch (caught) { return {hire: 0, deposit: 0, status: "Not quoted"}; }
}
function c_load(state: AppState, value: number): AppState {
 sdk.call("host/rental.equipment", ["from", json.stringify(state.c_from) as string, "until", json.stringify(state.c_until) as string], "c_catalogueAnswer");
 return {c_catalogue: "[]", c_packageQuote: "{}", c_separateQuote: "{}", c_message: "Loading catalogue" __PASSTHROUGH__};
}
function c_catalogueAnswer(state: AppState, value: string): AppState {
 sdk.call("host/rental.quote", ["items", json.stringify(equipmentBaskets(value).packageBasket) as string, "from", json.stringify(state.c_from) as string, "until", json.stringify(state.c_until) as string], "c_packageAnswer");
 return {c_catalogue: value, c_message: "Quoting package" __PASSTHROUGH__};
}
function c_packageAnswer(state: AppState, value: string): AppState {
 sdk.call("host/rental.quote", ["items", json.stringify(equipmentBaskets(state.c_catalogue).separate) as string, "from", json.stringify(state.c_from) as string, "until", json.stringify(state.c_until) as string], "c_separateAnswer");
 return {c_packageQuote: value, c_message: "Quoting separate basket" __PASSTHROUGH__};
}
function c_separateAnswer(state: AppState, value: string): AppState {
 let message: string = "";
 if (quoteSummary(value, state.c_budget).status === "Not quoted" || quoteSummary(state.c_packageQuote, state.c_budget).status === "Not quoted") { message = "Read failed; retry"; }
 return {c_separateQuote: value, c_message: message __PASSTHROUGH__};
}
function c_budget(state: AppState, value: number): AppState { return {c_budget: value __PASSTHROUGH__}; }
function c_from(state: AppState, value: string): AppState {
 sdk.call("host/rental.equipment", ["from", json.stringify(value) as string, "until", json.stringify(state.c_until) as string], "c_catalogueAnswer");
 return {c_from: value, c_catalogue: "[]", c_packageQuote: "{}", c_separateQuote: "{}", c_message: "Loading catalogue" __PASSTHROUGH__};
}
function c_until(state: AppState, value: string): AppState {
 sdk.call("host/rental.equipment", ["from", json.stringify(state.c_from) as string, "until", json.stringify(value) as string], "c_catalogueAnswer");
 return {c_until: value, c_catalogue: "[]", c_packageQuote: "{}", c_separateQuote: "{}", c_message: "Loading catalogue" __PASSTHROUGH__};
}
""","""
let pack: QuoteSummary = quoteSummary(state.c_packageQuote, state.c_budget);
let separate: QuoteSummary = quoteSummary(state.c_separateQuote, state.c_budget);
let baskets: Baskets = equipmentBaskets(state.c_catalogue);
sections.push(ui.Section("content", "Vocal kit", "", [ui.Button("default", "Read equipment", "c_load", 0, true), ui.Text("default", state.c_message), ui.TextField("default", "From ISO", state.c_from, "c_from"), ui.TextField("default", "Until ISO", state.c_until, "c_until"), ui.NumberField("default", "Budget EUR", state.c_budget, 0, 500, "c_budget"), ui.Text("default", baskets.packageBasket), ui.Text("default", baskets.separate)]));
sections.push(ui.Section("summary", "Package", "Refundable deposit separate from hire", [ui.Stat("default", "Package hire EUR", ui.formatNumber(pack.hire, 2), "", "chart"), ui.Stat("default", "Package deposit EUR", ui.formatNumber(pack.deposit, 2), "", "chart"), ui.Text("default", pack.status)]));
sections.push(ui.Section("summary", "Separate basket", "Refundable deposit separate from hire", [ui.Stat("default", "Separate hire EUR", ui.formatNumber(separate.hire, 2), "", "chart"), ui.Stat("default", "Separate deposit EUR", ui.formatNumber(separate.deposit, 2), "", "chart"), ui.Text("default", separate.status)]));
""","class Baskets { packageBasket: string; separate: string; }\nclass QuoteSummary { hire: number; deposit: number; status: string; }")
}
