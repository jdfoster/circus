package circus

import akka.actor.typed.ActorRef

class DeviceManager {
    companion object Factory {
        interface DeviceManagerMessage {}
        data class RequestTrackDevice(val groupId: String, val deviceId: String, val replyTo: ActorRef<DeviceRegistered>): DeviceManagerMessage, DeviceGroup.Factory.DeviceGroupMessage
        data class DeviceRegistered(val device: ActorRef<Device.Factory.DeviceMessage>)
    }
}
