package antimirov

import org.scalacheck.{Prop, Properties, Test}
import org.typelevel.claimant.Claim

import Util._

object DfaTest extends Properties("DfaTest") with TimingProperties { self =>

  override def overrideParameters(params: Test.Parameters): Test.Parameters =
    params
      .withMinSuccessfulTests(100)
      //.withPropFilter(Some("dfa regression #2"))

  override def scale: Long = 20L
  override def enableTiming = true
  override def failOnAbort = false

  timedProp("dfa accepts regex strings", genRxAndStrs) { case (rx, set) =>
    val dfa = rx.toDfa
    set.iterator.map { s =>
      val lhs = rx.accepts(s)
      val rhs = dfa.accepts(s)
      Claim(lhs == rhs) :| s"failed to accept '$s'"
    }.foldLeft(Prop(true))(_ && _)
  }

  timedProp("accepts = !rejects", genRxAndStrs) { case (rx, set) =>
    val dfa = rx.toDfa
    set.iterator.map { s =>
      Claim(dfa.rejects(s) != dfa.accepts(s))
    }.foldLeft(Prop(true))(_ && _)
  }

  timedProp("(x = y) -> (x.toString = y.toString)", genRx, genRx) { (rx1, rx2) =>
    val (dfa1, dfa2) = (rx1.toDfa, rx2.toDfa)
    Claim((dfa1 != dfa2) || (s"$dfa1" == s"$dfa2"))
  }

  property("dfa regression #1") =
    Claim(Rx.parse("[ab]b").toDfa.rejects("aa"))

  property("dfa regression #2") =
    Claim(Rx.parse("").toDfa.accepts(""))

  property("minimization") =
    Prop.forAllNoShrink(genRxAndStrs) { case (rx, set) =>
      val dfa1 = rx.toDfa
      val dfa2 = dfa1.minimize
      set.iterator.map { s =>
        Prop(dfa1.accepts(s) == dfa2.accepts(s)) :| s"disagree on $s"
      }.foldLeft(Prop(true))(_ && _)
    }
}
