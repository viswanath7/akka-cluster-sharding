package com.example.akka.actor

import akka.actor.{ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.example.akka.actor.Counter._
import org.slf4j.LoggerFactory

object Counter {

  private val logger = LoggerFactory getLogger Counter.getClass

  def props: Props = Props[Counter]

  sealed trait Command
  case object IncrementCounter extends Command
  case object DecrementCounter extends Command
  case object GetCounterValue extends Command
  case object HaltCounter extends Command

  sealed trait Event
  case class CounterValueChangedEvent(delta: Int) extends Event

  val numberOfShards = 100

  val shardName: String = Counter.getClass.getSimpleName

  /**
    * Envelope that wraps the identifier of an entity, along with the actual message that's sent to the entity actor.
    * It's the primary class that represents incoming messages, to interact with persistent actor 'Counter'.
    *
    * @param entityId
    * @param counterCommand
    */
  case class CounterEnvelopeMessage(entityId: Long, counterCommand: Command)

  /**
    * Partial function that extracts from an incoming message,
    * an entity's id and command, to eventually send it to the persistent entity.
    */
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case CounterEnvelopeMessage(entityId, payload) => (entityId.toString, payload)
  }

  /**
    * Partial function that extracts from an incoming message,
    * the identifier of a shard. The function defines grouping of shards.
    *
    */
  val extractShardId: ShardRegion.ExtractShardId = {
    /**
      * Same shard-identifier must be returned for a specific entity-identifier at all times,
      * to prevent an entity actor accidentally being started in several locations simultaneously.
      *
      * The objective is to provide uniform distribution i.e. same number of entities in each shard.
      *
      * As a rule of thumb, the number of shards should be a factor ten greater than the planned
      * maximum number of cluster nodes. Less shards than number of nodes will result in that
      * some nodes will not host any shards. Too many shards will result in inefficient management
      * of the shards, e.g. re-balancing overhead, and increased latency as coordinator is involved
      * in the routing of the first message for each shard. The sharding algorithm must be the same
      * on all nodes in a running cluster. It can be changed after stopping all nodes of a cluster.
      *
      * Sharding algorithm employed here takes absolute value of hash code of entity identifier
      * modulo number of shards.
      */
    case CounterEnvelopeMessage(id, msg) => (id.toLong % numberOfShards).toString
  }

}

// An entity actor that uses event sourcing
class Counter extends PersistentActor with ActorLogging {

  private var counterState = 0

  // Unique entity identifier
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  private def updateState(event: CounterValueChangedEvent) =
    counterState += event.delta

  // Recovery handler
  override def receiveRecover: Receive = {
    case event: CounterValueChangedEvent => updateState(event)
  }

  // Command handler
  override def receiveCommand: Receive = {
    case IncrementCounter =>
      log info "Received a command to increase the counter by one"
      persist(event = CounterValueChangedEvent(1) )(updateState)
    case DecrementCounter =>
      log info "Received a command to decrease the counter by one"
      persist(event = CounterValueChangedEvent(-1) )(updateState)
    case GetCounterValue =>
      log info s"Received a command to fetch the current value of the counter; which is $counterState"
      log info s"Sending integer $counterState to ${sender().path} ..."
      sender ! counterState
    case HaltCounter =>
      log info "Received a command to stop the counter actor"
      context stop self
    case cmd =>
      log warning s"Ignoring the unsupported command $cmd"
  }


}

