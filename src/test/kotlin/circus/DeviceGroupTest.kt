package circus

import io.kotlintest.extensions.TestListener
import io.kotlintest.milliseconds
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

class DeviceGroupTest: StringSpec() {
    override fun listeners(): List<TestListener> = listOf(ActorListener)

    init {
        "be able to register a device actor" {
            val probe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", probe.ref))
            val registeredOne = probe.receiveMessage()
            val deviceActorOne = registeredOne.device

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceTwo", probe.ref))
            val registeredTwo = probe.receiveMessage()
            val deviceActorTwo = registeredTwo.device
            deviceActorOne shouldNotBe deviceActorTwo

            val recordProbe = ActorListener.actorTestKit.createTestProbe<Device.Factory.TemperatureRecorded>()
            deviceActorOne.tell(Device.Factory.RecordTemperature(0, 1.0, recordProbe.ref))
            recordProbe.expectMessage(Device.Factory.TemperatureRecorded(0))
            deviceActorTwo.tell(Device.Factory.RecordTemperature(1, 2.0, recordProbe.ref))
            recordProbe.expectMessage((Device.Factory.TemperatureRecorded(1)))
        }

        "ignore requests for wrong groupId" {
            val probe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("wrongGroup", "deviceOne", probe.ref))
            probe.expectNoMessage(500.milliseconds)
        }

        "return same actor for same deviceId" {
            val probe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", probe.ref))
            val registeredOne = probe.receiveMessage()
            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", probe.ref))
            val registeredTwo = probe.receiveMessage()
            registeredOne.device shouldBe registeredTwo.device
        }
    }
}
