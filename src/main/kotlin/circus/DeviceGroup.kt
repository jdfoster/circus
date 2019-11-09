package circus

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class DeviceGroup(private val context: ActorContext<DeviceGroupMessage>, private val groupId: String): AbstractBehavior<DeviceGroup.Factory.DeviceGroupMessage>() {
    companion object Factory {
        interface DeviceGroupMessage {}
        data class DeviceTerminated(val device: ActorContext<Device.Factory.DeviceMessage>, val groupId: String, val deviceId: String): DeviceGroupMessage

        fun createDeviceGroup(groupId: String): Behavior<DeviceGroupMessage> {
            return Behaviors.setup{ DeviceGroup(it, groupId) }
        }
    }

    private var deviceIdToActor = mutableMapOf<String, ActorRef<Device.Factory.DeviceMessage>>()

    init {
        context.log.info("DeviceGroup {} started", groupId)
    }

    override fun createReceive(): Receive<DeviceGroupMessage> {
        return newReceiveBuilder()
                .onMessage(DeviceManager.Factory.RequestTrackDevice::class.java) { trackDevice(it) }
                .onSignal(PostStop::class.java) { postStop() }
                .build()
    }

    private fun trackDevice(req: DeviceManager.Factory.RequestTrackDevice): Behavior<DeviceGroupMessage> {
        when (groupId) {
            req.groupId -> {
                val device = when (val device = deviceIdToActor.get(req.deviceId)) {
                    null -> {
                        context.log.info("Creating device actor for {}", req.deviceId)
                        val deviceActor = context.spawn(Device.createBehaviour(req.groupId, req.deviceId), "device-${req.deviceId}")
                        deviceIdToActor.put(req.deviceId, deviceActor)
                        deviceActor
                    }
                    else -> device
                }

                req.replyTo.tell(DeviceManager.Factory.DeviceRegistered(device))
            }
            else -> context.log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.", req.groupId, groupId)
        }

        return this
    }

    private fun postStop(): DeviceGroup {
        context.log.info("Device group {} stopped", groupId)
        return this
    }
}
