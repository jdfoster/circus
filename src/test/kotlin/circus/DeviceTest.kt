package circus

import akka.actor.testkit.typed.javadsl.BehaviorTestKit
import akka.actor.testkit.typed.javadsl.TestInbox
import io.kotlintest.extensions.TestListener
import io.kotlintest.specs.StringSpec

class DeviceTest: StringSpec() {
    override fun listeners(): List<TestListener> = listOf(ActorListener)

    init {
        "reply with empty reading if no temperature is known" {
            val test = BehaviorTestKit.create(Device.createBehaviour("one", "two"))
            val inbox = TestInbox.create<Device.Factory.RespondTemperature>()
            test.run(Device.Factory.ReadTemperature(42L, inbox.ref))
            inbox.expectMessage(Device.Factory.RespondTemperature(42L, null))
        }
    }
}
