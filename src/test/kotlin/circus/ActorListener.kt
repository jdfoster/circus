package circus

import akka.actor.testkit.typed.javadsl.ActorTestKit
import io.kotlintest.extensions.TestListener

object ActorListener: TestListener {
    val actorTestKit: ActorTestKit = ActorTestKit.create()

    override fun afterProject() {
        actorTestKit.shutdownTestKit()
    }
}
