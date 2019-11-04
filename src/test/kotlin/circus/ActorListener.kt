package circus

import akka.actor.testkit.typed.javadsl.ActorTestKit
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TestListener

object ActorListener: TestListener {
    val actorTestKit: ActorTestKit = ActorTestKit.create()

    override fun afterSpecClass(spec: Spec, results: Map<TestCase, TestResult>) {
        actorTestKit.shutdownTestKit()
    }
}
