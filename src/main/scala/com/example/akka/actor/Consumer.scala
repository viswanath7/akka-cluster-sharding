package com.example.akka.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ClusterSharding
import com.example.akka.actor.Counter.{CounterEnvelopeMessage, DecrementCounter, GetCounterValue, IncrementCounter}

import scala.language.postfixOps
import scala.util.Random

object Consumer {

  def props: Props = Props[Consumer]

  sealed trait Operation
  case object IncrementOperation extends Operation
  case object DecrementOperation extends Operation
  case object FetchOperation extends Operation

  case class OperationalMessage(operation: Operation)

  // The current configuration has 10 nodes
  private val maximumClusterNodes = Counter.numberOfShards / 10
}

class Consumer extends Actor with ActorLogging {

  /**
    * Messages to the entities are always sent via the local ShardRegion so,
    * one shall use counter shard region actor reference to send messages to counter entities.
    */
  val counterShardRegion: ActorRef = ClusterSharding(context.system).shardRegion(Counter.shardName)

  import Consumer._
  import context.dispatcher
  import scala.concurrent.duration._

  // Send increment message every 2 seconds
  private val incrementMessage: OperationalMessage = OperationalMessage(IncrementOperation)
  private val decrementMessage = OperationalMessage(DecrementOperation)
  private val fetchMessage = OperationalMessage(FetchOperation)

  // Send increment message every 2 seconds
  context.system.scheduler.schedule(2 seconds, 2 seconds, self, incrementMessage)

  // Send decrement message every 3 seconds
  context.system.scheduler.schedule(3 seconds, 3 seconds, self, decrementMessage)

  // Send fetch message every 6 seconds
  context.system.scheduler.schedule(6 seconds, 6 seconds, self, fetchMessage)

  override def receive: Receive = {
    case OperationalMessage(IncrementOperation) =>
      log info s"Handling message for increment operation"
      // Chooses a node randomly (between 0 and 9) and sends a increment message to Counter entity running on that node.
      counterShardRegion ! CounterEnvelopeMessage(getEntityIdentifier, IncrementCounter)
    case OperationalMessage(DecrementOperation) =>
      log info s"Handling message for decrement operation"
      // Chooses a node randomly (between 0 and 9) and sends a decrement message to Counter entity running on that node.
      counterShardRegion ! CounterEnvelopeMessage(getEntityIdentifier, DecrementCounter)
    case OperationalMessage(FetchOperation) =>
      log info s"Handling message for fetch operation"
      // Chooses a node randomly (between 0 and 9) and sends message to Counter entity running on that node for fetching the current counter value.
      counterShardRegion ! CounterEnvelopeMessage(getEntityIdentifier, GetCounterValue)
    case counterState: Int =>
      log info s"Counter entity on node ${sender().path.name}: Current value of counter is $counterState"
  }

  /**
    * Chooses an entity identifier randomly.
    * Number of entities are dictated here by maximum number of nodes; which happens to be 10 in our application.
    * This function therefore generates a number between 0 and 9 for 'Counter' entity's identifier.
    * The 'Counter' entity corresponding to the randomly generated identifier will eventually be chosen for transmission of message.
    *
    * @return identifier of entity which happens to be 'Consumer'
    */
  private def getEntityIdentifier: Long = Random.nextInt(maximumClusterNodes).toLong


}


