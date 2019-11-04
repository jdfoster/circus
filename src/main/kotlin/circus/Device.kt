package circus

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive


class Device(private val context: ActorContext<DeviceMessage>, groupId: String, deviceId: String): AbstractBehavior<Device.Factory.DeviceMessage>() {
    companion object Factory {
        interface DeviceMessage {}
        data class ReadTemperature(val requestId: Long, val replyTo: ActorRef<RespondTemperature>): DeviceMessage
        data class RespondTemperature(val requestId: Long, val value: Double?)

        fun createBehaviour(groupId: String, deviceId: String): Behavior<DeviceMessage> {
            return Behaviors.setup{ Device(it, groupId, deviceId) }
        }
    }

    private var lastTemperatureReading: Double? = null

    init {
        context.log.info("Device {}-{} started", groupId, deviceId)
    }

    override fun createReceive(): Receive<DeviceMessage> {
        return newReceiveBuilder()
                .onMessage(ReadTemperature::class.java) { readTemperature(it) }
                .onSignal(PostStop::class.java) { postStop() }
                .build()
    }

    private fun readTemperature(req: ReadTemperature): Behavior<DeviceMessage> {
        req.replyTo.tell(RespondTemperature(req.requestId, lastTemperatureReading))
        return this
    }

    private fun postStop(): Device {
        context.log.info("IoT application stopped")
        return this
    }
}
