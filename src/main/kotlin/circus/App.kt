package circus

import akka.actor.typed.ActorSystem

fun main(args: Array<String>) {
    ActorSystem.create(IotSupervisor.createBehaviour(), "iot-system")
}
