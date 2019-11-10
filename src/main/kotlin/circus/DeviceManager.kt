package circus

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class DeviceManager(context: ActorContext<DeviceManagerMessage>): AbstractBehavior<DeviceManager.Factory.DeviceManagerMessage>(context) {
    companion object Factory {
        interface DeviceManagerMessage
        data class DeviceGroupTerminated(val groupId: String): DeviceManagerMessage
        data class RequestTrackDevice(val groupId: String, val deviceId: String, val replyTo: ActorRef<DeviceRegistered>): DeviceManagerMessage, DeviceGroup.Factory.DeviceGroupMessage
        data class DeviceRegistered(val device: ActorRef<Device.Factory.DeviceMessage>)
        data class RequestDeviceList(val requestId: Long, val groupId: String, val replyTo: ActorRef<ReplyDeviceList>): DeviceManagerMessage, DeviceGroup.Factory.DeviceGroupMessage
        data class ReplyDeviceList(val requestId: Long, val ids: Set<String>)

        fun createDeviceManager(): Behavior<DeviceManagerMessage> {
            return Behaviors.setup{ DeviceManager(it) }
        }
    }

    private var groupIdToActor = mutableMapOf<String, ActorRef<DeviceGroup.Factory.DeviceGroupMessage>>()

    init {
        context.log.info("DeviceManager started")
    }

    override fun createReceive(): Receive<DeviceManagerMessage> {
        return newReceiveBuilder()
                .onMessage(RequestTrackDevice::class.java) { trackGroup(it) }
                .onMessage(RequestDeviceList::class.java) { listGroup(it) }
                .onMessage(DeviceGroupTerminated::class.java) { dropGroup(it) }
                .onSignal(PostStop::class.java) { postStop() }
                .build()
    }

    private fun trackGroup(req: RequestTrackDevice): Behavior<DeviceManagerMessage> {
        val group = groupIdToActor[req.groupId]?: kotlin.run {
            context.log.info("Creating device group actor for {}", req.groupId)
            val groupActor = context.spawn(DeviceGroup.createDeviceGroup(req.groupId), "group-${req.groupId}")
            context.watchWith(groupActor, DeviceGroupTerminated(req.groupId))
            groupIdToActor[req.groupId] = groupActor
            groupActor
        }
        group.tell(req)

        return this
    }

    private fun listGroup(req: RequestDeviceList): Behavior<DeviceManagerMessage> {
        groupIdToActor[req.groupId]?.tell(req)?: kotlin.run {
            req.replyTo.tell(ReplyDeviceList(req.requestId, emptySet()))
        }
        return this
    }

    private fun dropGroup(req: DeviceGroupTerminated): Behavior<DeviceManagerMessage> {
        context.log.info("Device group actor for {} has been terminated", req.groupId)
        groupIdToActor.remove(req.groupId)
        return this
    }

    private fun postStop(): DeviceManager {
        context.log.info("DeviceManager stopped")
        return this
    }
}
