package dev.deal.connectors.tests

/** Model-facing raw protocol fixture, not an alternate production parser. */
internal fun rawSourceFixture(deal: String, dealui: String): String {
    var token = "fixture"
    while (("\n" + deal + "\n" + dealui).contains("\n<<<$token:")) token += "x"
    return "DEAL-SOURCE/1 $token\n<<<$token:deal>>>\n$deal\n<<<$token:dealui>>>\n$dealui\n<<<$token:end>>>"
}
internal fun rawPatchFixture(digest: String, search: String, replacement: String): String {
    var token = "fixture"
    while (("\n" + search + "\n" + replacement).contains("\n<<<$token:")) token += "x"
    return "DEAL-PATCH/1 $token $digest\n<<<$token:deal-search>>>\n$search\n<<<$token:replacement>>>\n$replacement\n<<<$token:end>>>"
}
