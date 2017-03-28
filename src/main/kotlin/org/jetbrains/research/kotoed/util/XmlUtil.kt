package org.jetbrains.research.kotoed.util

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

fun evaluateXPath(xpath: String, xml: InputStream): List<Node> {
    val b = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val d = b.parse(xml)

    val x = XPathFactory.newInstance().newXPath()
    val r = x.evaluate(xpath, d, XPathConstants.NODESET) as NodeList

    return (0..r.length - 1).map { r.item(it) }
}

fun xml2json(xml: InputStream): JsonObject {
    val b = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
    val p = b.newSAXParser()
    val r = p.xmlReader
    val xjch = Xml2JsonContentHandler().also { r.contentHandler = it }
    r.parse(InputSource(xml))
    return xjch.result
}

class Xml2JsonContentHandler : DefaultHandler() {

    private val stack = Stack<JsonObject>()

    val result: JsonObject
        get() {
            assert(1 == stack.size)
            return stack.peek()
        }

    override fun startDocument() {
        stack.push(JsonObject())
    }

    override fun endDocument() {}

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
        val res = JsonObject()

        (0..attributes.length - 1).map { i ->
            Pair(attributes.getQName(i), attributes.getValue(i))
        }.forEach { (k, v) ->
            res.put(k, v)
        }

        stack.push(res)
    }


    override fun endElement(uri: String?, localName: String?, qName: String?) {
        val me = stack.pop()
        val parent = stack.peek()

        if (parent.containsKey(qName)) {
            val prev = parent.getValue(qName)

            if (prev is JsonArray) {
                prev.add(me)
            } else {
                parent.put(qName, JsonArray(prev, me))
            }
        } else {
            parent.put(qName, me)
        }
    }

}
