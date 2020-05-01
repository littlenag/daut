package daut29_abstraction

import daut.Monitor

/**
  * Events: these represent behavior of spacecraft. A spacecraft can boot,
  * consisting of a boot start and a boot end. A double boot is
  * a boot followed by a boot.
  *
  * Property: in a double boot there should be less than 50 seconds
  * from the start of the first boot to the end of the second boot.
  *
  * Approach: first we abstract from the boot starts and boot ends to the boots.
  * Then we abstract from the boots to the double boots.
  * Then we verify the double boots.
  *
  * Note: of course, in this case, this could be formulated without abstraction, but
  * it is an example.
  */

trait Event

case class BootStart(time: Int) extends Event

case class BootEnd(time: Int) extends Event

case class Boot(time1: Int, time2: Int) extends Event

case class DoubleBoot(time1: Int, time2: Int) extends Event

/**
  * We create a special kind of monitor than can abstract events. The function
  * `process` processes an event stream and produces a new event stream.
  * The function `addAbstraction` can be used in a monitor to add events to
  * the event stream. The function `process` takes a trace as argument.
  * It calls `Monitor.verify(trace: List[Event])` which I added to Daut.
  * See new version in github.
  *
  * Note that one could overwrite `verify` to call `addAbstraction` on every
  * input event, such that the input trace would always be included in the
  * output trace. I did not do that here.
  */

class Process[Event] extends Monitor[Event] {
  var abstraction = new scala.collection.mutable.ListBuffer[Event]()

  def addAbstraction(event: Event): Unit = {
    abstraction += event
  }

  def process(events: List[Event]): List[Event] = {
    verify(events)
    println(abstraction)
    abstraction.toList
  }
}

/**
  * Process that abstracts boot starts and boot ends to boots.
  */

class M1 extends Process[Event] {
  always {
    case BootStart(time1) => watch {
      case BootEnd(time2) => addAbstraction(Boot(time1, time2))
    }
  }
}

/**
  * Process that abstracts boots to double boots.
  */

class M2 extends Process[Event] {
  always {
    case Boot(time1, _) => watch {
      case Boot(_, time2) => addAbstraction(DoubleBoot(time1, time2))
    }
  }
}

/**
  * Process that verifies double boots.
  */

class M3 extends Process[Event] {
  always {
    case DoubleBoot(time1, _) =>
      watch {
        case DoubleBoot(_, time2) =>
          ensure(time2 - time1 < 50)
      }
  }
}

/**
  * Creating initial trace and applying the processes one by one.
  */

object Main {
  def main(args: Array[String]): Unit = {
    val m1 = new M1
    val m2 = new M2
    val m3 = new M3

    val trace0 = List(
      BootStart(10),
      BootEnd(20),

      BootStart(30),
      BootEnd(40),

      BootStart(50),
      BootEnd(60),

      BootStart(70),
      BootEnd(80)
    )

    val trace1 = m1.process(trace0)
    val trace2 = m2.process(trace1)
    m3.process(trace2)
  }
}