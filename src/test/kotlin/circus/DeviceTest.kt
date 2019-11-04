package circus

import akka.actor.testkit.typed.javadsl.BehaviorTestKit
import akka.actor.testkit.typed.javadsl.TestInbox
import io.kotlintest.extensions.TestListener
import io.kotlintest.shouldBe
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

        "reply with latest temperature reading" {
            val recordProbe = ActorListener.actorTestKit.createTestProbe<Device.Factory.TemperatureRecorded>()
            val readProbe = ActorListener.actorTestKit.createTestProbe<Device.Factory.RespondTemperature>()
            val deviceActor = ActorListener.actorTestKit.spawn(Device.Factory.createBehaviour("group", "device"))
            deviceActor.tell(Device.Factory.RecordTemperature(1L, 24.0, recordProbe.ref))
            recordProbe.expectMessage(Device.Factory.TemperatureRecorded(1L))

            deviceActor.tell(Device.Factory.ReadTemperature(2L, readProbe.ref))
            val responseOne = readProbe.receiveMessage()
            responseOne.requestId.shouldBe(2L)
            responseOne.value.shouldBe(24.0)

            deviceActor.tell(Device.Factory.RecordTemperature(3L, 55.0, recordProbe.ref))
            recordProbe.expectMessage(Device.Factory.TemperatureRecorded(3L))

            deviceActor.tell(Device.Factory.ReadTemperature(4L, readProbe.ref))
            val responseTwo = readProbe.receiveMessage()
            responseTwo.requestId.shouldBe(4L)
            responseTwo.value.shouldBe(55.0)
        }
    }
}
