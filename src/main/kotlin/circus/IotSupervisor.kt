package circus

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class IotSupervisor(context: ActorContext<Unit>): AbstractBehavior<Unit>(context) {
    companion object Factory {
        fun createBehaviour(): Behavior<Unit> {
            return Behaviors.setup{ IotSupervisor(it) }
        }
    }

    init {
        context.log.info("IoT application started")
    }

    override fun createReceive(): Receive<Unit> {
        return newReceiveBuilder()
                .onSignal(PostStop::class.java){ this.postStop() }
                .build()
    }

    private fun postStop(): IotSupervisor {
        context.log.info("IoT application stopped")
        return this
    }
}
