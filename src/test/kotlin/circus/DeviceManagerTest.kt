package circus

import io.kotlintest.extensions.TestListener
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

class DeviceManagerTest: StringSpec() {
    override fun listeners(): List<TestListener> = listOf(ActorListener)

    init {
        "be able to register a group actor" {
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val managerActor = ActorListener.actorTestKit.spawn(DeviceManager.createDeviceManager())

            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("groupOne", "device", registeredProbe.ref))
            val registeredOne = registeredProbe.receiveMessage()
            val deviceActorOne = registeredOne.device

            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("groupTwo", "device", registeredProbe.ref))
            val registeredTwo = registeredProbe.receiveMessage()
            val deviceActorTwo = registeredTwo.device
            deviceActorOne shouldNotBe deviceActorTwo

            val recordProbe = ActorListener.actorTestKit.createTestProbe<Device.Factory.TemperatureRecorded>()
            deviceActorOne.tell(Device.Factory.RecordTemperature(0, 1.0, recordProbe.ref))
            recordProbe.expectMessage(Device.Factory.TemperatureRecorded(0))
            deviceActorTwo.tell(Device.Factory.RecordTemperature(1, 2.0, recordProbe.ref))
            recordProbe.expectMessage((Device.Factory.TemperatureRecorded(1)))
        }

        "return same actor for same deviceId" {
            val registerProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val managerActor = ActorListener.actorTestKit.spawn(DeviceManager.createDeviceManager())

            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registerProbe.ref))
            val registeredOne = registerProbe.receiveMessage()
            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "deviceOne", registerProbe.ref))
            val registeredTwo = registerProbe.receiveMessage()
            registeredOne.device shouldBe registeredTwo.device
        }

        "be able to list active devices" {
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val managerActor = ActorListener.actorTestKit.spawn(DeviceManager.createDeviceManager())

            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("groupOne", "deviceOne", registeredProbe.ref))
            registeredProbe.receiveMessage()

            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("groupTwo", "deviceTwo", registeredProbe.ref))
            registeredProbe.receiveMessage()

            val deviceListProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.ReplyDeviceList>()
            managerActor.tell(DeviceManager.Factory.RequestDeviceList(0, "groupOne", deviceListProbe.ref))
            deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(0, setOf("deviceOne")))
            managerActor.tell(DeviceManager.Factory.RequestDeviceList(1, "groupTwo", deviceListProbe.ref))
            deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(1, setOf("deviceTwo")))
        }

        "be able to list active devices after one shuts down" {
            val registeredProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.DeviceRegistered>()
            val managerActor = ActorListener.actorTestKit.spawn(DeviceManager.createDeviceManager())

            managerActor.tell(DeviceManager.Factory.RequestTrackDevice("group", "device", registeredProbe.ref))
            val registeredOne = registeredProbe.receiveMessage()
            val toShutdown = registeredOne.device

            val deviceListProbe = ActorListener.actorTestKit.createTestProbe<DeviceManager.Factory.ReplyDeviceList>()
            managerActor.tell(DeviceManager.Factory.RequestDeviceList(0, "group", deviceListProbe.ref))
            deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(0, setOf("device")))

            toShutdown.tell(Device.Factory.Passivate)
            registeredProbe.expectTerminated(toShutdown, registeredProbe.remainingOrDefault)

            registeredProbe.awaitAssert {
                managerActor.tell(DeviceManager.Factory.RequestDeviceList(1, "group", deviceListProbe.ref))
                deviceListProbe.expectMessage(DeviceManager.Factory.ReplyDeviceList(1, emptySet()))
            }
        }
    }
}