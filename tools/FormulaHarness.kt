package dev.deal.embedding

/** Parser/binding tests; production-emitter arithmetic is also compiled by verify-behavior.py. */
fun main() {
    fun parse(s:String)=MaintainedFormula.parse(s)
    fun rejected(s:String) { check(runCatching{parse(s)}.exceptionOrNull() is BehaviorCandidate.Rejected) { "Accepted forbidden formula: $s" } }
    val expression=parse("choose(budget >= sum(items.rows, row.cost * row.quantity) && ready.value, clock(start.value - 15 - travel.result), \"Refresh\")")
    check(MaintainedFormula.references(expression)==setOf("budget","items.rows","row.cost","row.quantity","ready.value","start.value","travel.result"))
    val tree=parse("1 + 2 * 3") as MaintainedFormula.Expression.Operation
    check(tree.name=="add" && (tree.arguments[1] as MaintainedFormula.Expression.Operation).name=="multiply")
    val left=parse("10 - 3 - 2") as MaintainedFormula.Expression.Operation
    check(left.name=="subtract" && (left.arguments[0] as MaintainedFormula.Expression.Operation).name=="subtract")
    check((parse("1.25e2") as MaintainedFormula.Expression.Literal).value==125.0)
    check((parse("\"a\\u263a\\n\"") as MaintainedFormula.Expression.Literal).value=="a☺\n")
    check(MaintainedFormula.references(parse("true || !false")).isEmpty())
    check(MaintainedFormula.references(parse("x + "+"1 + ".repeat(100)+"2"))==setOf("x"))
    listOf("", "a = 1", "a; b", "host.quote(items)", "await read()", "foo(1)",
        "floor(1,2)", "choose(true,1)", "1e999", "[1]", "{a:1}", "a..b", "a.",
        "1 == 1", "1 % 2", "1 /*comment*/ + 2", "1 trailing", "\"\\q\"", "\"\\u12xz\"", "\"unterminated",
        "function()", "constructor.constructor(1)").forEach(::rejected)
    val emitted=mutableListOf<String>()
    val bound=MaintainedFormula.lower(parse("a - b * 2"),{ "\$state.$it" },{ emitted.add("literal:$it");"literal" },{ op,args->emitted.add("$op:${args.joinToString()}");op })
    check(bound=="subtract" && emitted.last()=="subtract:\$state.a, multiply")
    emitted.clear()
    check(runCatching { MaintainedFormula.lower(parse("a + missing"),{ if(it=="missing")throw BehaviorCandidate.Rejected("Missing port") else it },{emitted.add("literal");"literal"},{op,_->emitted.add(op);op}) }.exceptionOrNull() is BehaviorCandidate.Rejected)
    check(emitted.isEmpty()) { "Failed binding partially lowered a formula" }
    println("PASS pure formulas: grammar, precedence, nested references, strict strings, injection rejection and binding preflight")
}
