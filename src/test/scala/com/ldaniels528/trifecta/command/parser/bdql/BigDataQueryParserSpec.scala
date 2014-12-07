package com.ldaniels528.trifecta.command.parser.bdql

import com.ldaniels528.trifecta.messages.query.{IOSource, BigDataSelection}
import com.ldaniels528.trifecta.messages.logic.Expressions._
import org.scalatest.Matchers._
import org.scalatest.{FeatureSpec, GivenWhenThen}

/**
 * Big Data Query Language (BD-QL) Parser Specification
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BigDataQueryParserSpec() extends FeatureSpec with GivenWhenThen {

  info("As a Big Data Query Parser")
  info("I want to be able to parse Big Data queries")

  feature("Ability to parse Big Data queries with implicit decoders") {
    scenario("A string containing a Big Data selection query using 'with default'") {
      Given("a Big Data selection query")
      val queryString =
        """
          |select symbol, exchange, lastTrade, open, prevClose, high, low, volume
          |from "kafka:shocktrade.quotes.avro" with default
          |where volume >= 1,000,000 and lastTrade <= 1
          |""".stripMargin

      When("The queries is parsed into a BD-QL object")
      val query = BigDataQueryParser(queryString)

      Then("The arguments should be successfully verified")
      query shouldBe BigDataSelection(
        source = IOSource(deviceURL = "kafka:shocktrade.quotes.avro", decoderURL = "default"),
        destination = None,
        fields = List("symbol", "exchange", "lastTrade", "open", "prevClose", "high", "low", "volume"),
        criteria = Some(AND(GE("volume", "1000000"), LE("lastTrade", "1"))),
        limit = None)
    }
  }

  feature("Ability to parse Big Data query") {
    scenario("A string containing a Big Data selection query") {
      Given("a Big Data selection query")
      val queryString =
        """
          |select symbol, exchange, lastTrade, open, prevClose, high, low, volume
          |from "kafka:shocktrade.quotes.avro" with "avro:file:avro/quotes.avsc"
          |where volume >= 1,000,000 and lastTrade <= 1
          |""".stripMargin

      When("The queries is parsed into a BD-QL object")
      val query = BigDataQueryParser(queryString)

      Then("The arguments should be successfully verified")
      query shouldBe BigDataSelection(
        source = IOSource(deviceURL = "kafka:shocktrade.quotes.avro", decoderURL = "avro:file:avro/quotes.avsc"),
        destination = None,
        fields = List("symbol", "exchange", "lastTrade", "open", "prevClose", "high", "low", "volume"),
        criteria = Some(AND(GE("volume", "1000000"), LE("lastTrade", "1"))),
        limit = None)
    }
  }

  feature("Ability to parse Big Data queries with an embedded insert") {
    scenario("A string containing a Big Data selection query") {
      Given("a Big Data selection query")
      val queryString =
        """
          |select symbol, exchange, lastTrade, volume
          |from "kafka:quotes" with "avro:file:avro/quotes.avsc"
          |into "es:/quotes/quote/AAPL" with json
          |where exchange == "OTCBB"
          |and lastTrade <= 1.0
          |and volume >= 1,000,000
          |limit 10
          |""".stripMargin

      When("The queries is parsed into a BD-QL object")
      val query = BigDataQueryParser(queryString)

      Then("The arguments should be successfully verified")
      query shouldBe BigDataSelection(
        source = IOSource(deviceURL = "kafka:quotes", decoderURL = "avro:file:avro/quotes.avsc"),
        destination = Some(IOSource(deviceURL = "es:/quotes/quote/AAPL", decoderURL = "json")),
        fields = List("symbol", "exchange", "lastTrade", "volume"),
        criteria = Some(AND(AND(EQ("exchange", "OTCBB"), LE("lastTrade", "1.0")), GE("volume", "1000000"))),
        limit = Some(10))
    }
  }

}
