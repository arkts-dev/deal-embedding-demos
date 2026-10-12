package dev.deal.apps.organizer
import dev.deal.embedding.WireField
import dev.deal.embedding.WireType

fun organizerContextSchema():WireType {
    val text=WireType("string",maximum=1024)
    val names=WireType("array",element=text,maximum=128)
    return WireType("record",fields=listOf(
        WireField("show",text),WireField("venue",text),WireField("date",text),WireField("timeZone",text),WireField("requirements",names),
        WireField("requirement",text,true),WireField("specification",text,true),WireField("quantity",WireType("int",minimumInt=1,maximumInt=999),true),
        WireField("from",text,true),WireField("until",text,true),WireField("searchTerms",names,true)
    ))
}
