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
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registeredProbe.ref))
            val registeredOne = registeredProbe.receiveMessage()
            val deviceActorOne = registeredOne.device

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceTwo", registeredProbe.ref))
            val registeredTwo = registeredProbe.receiveMessage()
            val deviceActorTwo = registeredTwo.device
            deviceActorOne shouldNotBe deviceActorTwo

            val recordProbe = ActorListener.actorTestKit.createTestProbe<Device.Factory.TemperatureRecorded>()
            deviceActorOne.tell(Device.Factory.RecordTemperature(0, 1.0, recordProbe.ref))
            recordProbe.expectMessage(Device.Factory.TemperatureRecorded(0))
            deviceActorTwo.tell(Device.Factory.RecordTemperature(1, 2.0, recordProbe.ref))
            recordProbe.expectMessage((Device.Factory.TemperatureRecorded(1)))
        }

        "ignore requests for wrong groupId" {
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("wrongGroup", "deviceOne", registeredProbe.ref))
            registeredProbe.expectNoMessage(500.milliseconds)
        }

        "return same actor for same deviceId" {
            val registerProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registerProbe.ref))
            val registeredOne = registerProbe.receiveMessage()
            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registerProbe.ref))
            val registeredTwo = registerProbe.receiveMessage()
            registeredOne.device shouldBe registeredTwo.device
        }

        "be able to list active devices" {
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registeredProbe.ref))
            registeredProbe.receiveMessage()

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceTwo", registeredProbe.ref))
            registeredProbe.receiveMessage()

            val deviceListProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.ReplyDeviceList>()
            groupActor.tell(DeviceManager.Factory.RequestDeviceList(0, "group", deviceListProbe.ref))
            deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(0, setOf("deviceOne", "deviceTwo")))
        }

        "be able to list active devices after one shuts down" {
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val groupActor = ActorListener.actorTestKit.spawn(DeviceGroup.createDeviceGroup("group"))

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registeredProbe.ref))
            val registeredOne = registeredProbe.receiveMessage()
            val toShutdown = registeredOne.device

            groupActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceTwo", registeredProbe.ref))
            registeredProbe.receiveMessage()

            val deviceListProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.ReplyDeviceList>()
            groupActor.tell(DeviceManager.Factory.RequestDeviceList(0, "group", deviceListProbe.ref))
            deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(0, setOf("deviceOne", "deviceTwo")))

            toShutdown.tell(Device.Factory.Passivate)
            registeredProbe.expectTerminated(toShutdown, registeredProbe.remainingOrDefault)

            registeredProbe.awaitAssert {
                groupActor.tell(DeviceManager.Factory.RequestDeviceList(1, "group", deviceListProbe.ref))
                deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(1, setOf("deviceTwo")))
            }
        }
    }
}
