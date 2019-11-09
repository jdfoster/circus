package circus

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class DeviceGroup(context: ActorContext<DeviceGroupMessage>, private val groupId: String): AbstractBehavior<DeviceGroup.Factory.DeviceGroupMessage>(context) {
    companion object Factory {
        interface DeviceGroupMessage
        data class DeviceTerminated(val device: ActorRef<Device.Factory.DeviceMessage>, val groupId: String, val deviceId: String): DeviceGroupMessage
        data class RequestDeviceList(val requestId: Long, val groupId: String, val replyTo: ActorRef<ReplyDeviceList>): DeviceGroupMessage
        data class ReplyDeviceList(val requestId: Long, val ids: Set<String>)

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
                .onMessage(DeviceTerminated::class.java) { dropDevice(it) }
                .onMessage(RequestDeviceList::class.java) { listDevices(it) }
                .onSignal(PostStop::class.java) { postStop() }
                .build()
    }

    private fun trackDevice(req: DeviceManager.Factory.RequestTrackDevice): Behavior<DeviceGroupMessage> {
        when (groupId) {
            req.groupId -> {
                val device = deviceIdToActor[req.deviceId]?: kotlin.run {
                    context.log.info("Creating device actor for {}", req.deviceId)
                    val deviceActor = context.spawn(Device.createBehaviour(req.groupId, req.deviceId), "device-${req.deviceId}")
                    context.watchWith(deviceActor, DeviceTerminated(deviceActor, req.groupId, req.deviceId))
                    deviceIdToActor[req.deviceId] = deviceActor
                    deviceActor
                }

                req.replyTo.tell(DeviceManager.Factory.DeviceRegistered(device))
            }
            else -> context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", req.groupId, groupId)
        }

        return this
    }

    private fun dropDevice(req: DeviceTerminated): Behavior<DeviceGroupMessage> {
        context.log.info("Device actor for {} has been terminated", req.deviceId)
        deviceIdToActor.remove(req.deviceId)
        return this
    }

    private fun listDevices(req: RequestDeviceList): Behavior<DeviceGroupMessage> {
        return when (groupId) {
            req.groupId -> {
                req.replyTo.tell(ReplyDeviceList(req.requestId, deviceIdToActor.keys))
                this
            }
            else -> Behaviors.unhandled()
        }
    }

    private fun postStop(): DeviceGroup {
        context.log.info("Device group {} stopped", groupId)
        return this
    }
}
