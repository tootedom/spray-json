package spray.json

import org.specs2.mutable.Specification
import org.scalacheck._
import org.specs2.matcher.ScalaCheckMatchers

object JsValueGenerators {
  import Gen._
  import Arbitrary.arbitrary

  // some characters have special meaning in parboiled
  // see org.parboiled.support.Chars, we have to exclude those
  val parseableString: Gen[String] = arbitrary[String].map(_.filterNot(_ > 0xfd00))
  val genString: Gen[JsString] = parseableString.map(JsString(_))
  val genBoolean: Gen[JsBoolean] = oneOf(JsFalse, JsTrue)
  val genLongNumber: Gen[JsNumber] = arbitrary[Long].map(JsNumber(_))
  val genIntNumber: Gen[JsNumber] = arbitrary[Long].map(JsNumber(_))
  val genDoubleNumber: Gen[JsNumber] = arbitrary[Long].map(JsNumber(_))
  def genArray(depth: Int): Gen[JsArray] =
    if (depth == 0) JsArray()
    else
      for {
        n <- choose(0, 15)
        els <- Gen.containerOfN[List, JsValue](n, genValue(depth - 1))
      } yield JsArray(els)
  def genField(depth: Int): Gen[(String, JsValue)] =
    for {
      key <- parseableString
      value <- genValue(depth)
    } yield key -> value
  def genObject(depth: Int): Gen[JsObject] =
    if (depth == 0) JsObject()
    else
      for {
        n <- choose(0, 15)
        fields <- Gen.containerOfN[List, (String, JsValue)](n, genField(depth - 1))
      } yield JsObject(fields)

  def genValue(depth: Int): Gen[JsValue] =
    oneOf(
      JsNull: Gen[JsValue],
      genString,
      genBoolean,
      genLongNumber,
      genDoubleNumber,
      genIntNumber,
      genArray(depth),
      genObject(depth))
  implicit val arbitraryValue: Arbitrary[JsValue] = Arbitrary(genValue(5))
}

class RoundTripSpecs extends Specification with ScalaCheckMatchers {
  import JsValueGenerators.arbitraryValue

  "Parsing / Printing round-trip" should {
    "starting from JSON using compactPrint" in prop { (json: JsValue) =>
      json.compactPrint.parseJson must_== json
    }
    "starting from JSON using prettyPrint" in prop { (json: JsValue) =>
      json.prettyPrint.parseJson must_== json
    }
  }
}
