package com.example.akka.application

import akka.actor.{ActorIdentity, ActorPath, ActorSystem, Identify, Props}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import akka.pattern.ask
import com.example.akka.actor.{Consumer, Counter}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object ClusterShardingApplication extends App {

  private val logger = LoggerFactory getLogger ClusterShardingApplication.getClass

  private[this] val actorSystemName = "ClusterSystem"

  private val nodePortNumbers = Seq("2551", "2552", "0")

  logger info s"Starting cluster with ${nodePortNumbers.length} nodes ..."

  startClusterNodes(nodePortNumbers)

  /**
    * Every node shall contain a 'ShardRegion' actor
    * however, only one node shall contain the 'Consumer' actor.
    *
    * @param ports
    */
  def startClusterNodes(ports: Seq[String]): Unit = {
    ports foreach { port =>
      logger info s"Starting a node at port $port ..."

      // Override the configuration of the port and read application.conf from the classpath
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load("application"))

      logger debug s"Creating an actor actorSystem named '$actorSystemName' ..."
      val actorSystem = ActorSystem(actorSystemName, config)

      startSharedJournal(actorSystem, startStore = (port == "2551"),
        actorPath = ActorPath.fromString(s"akka.tcp://$actorSystemName@127.0.0.1:2551/user/store"))

      logger.debug("Registering supported entity types at actorSystem start up ...")
      ClusterSharding(actorSystem).start(typeName = Counter.shardName,
        entityProps = Counter.props, settings = ClusterShardingSettings(actorSystem),
        extractEntityId = Counter.extractEntityId, extractShardId = Counter.extractShardId)

      // TODO: do it only for port 0
      if (port != "2551" && port != "2552") {
        Cluster(actorSystem) registerOnMemberUp {
          val consumerActorName = Consumer.getClass.getSimpleName
          logger info s"Size of cluster has reached the threshold of minimum number required of members so," +
            s" creating a $consumerActorName actor that shall send scheduled messages ..."
          actorSystem.actorOf(Consumer.props, consumerActorName)
        }
      }

    }


    /**
      * Start a shared journal so that events can be persisted into it by persistent actor.
      *
      * @param actorSystem
      * @param startStore
      * @param actorPath
      */
    def startSharedJournal(actorSystem: ActorSystem, startStore: Boolean, actorPath: ActorPath) = {

      import scala.concurrent.duration._

      /**
       * Start the shared journal one one node (don't crash this SPOF)
       *  This will not be needed with a distributed journal
       */
      if (startStore) actorSystem.actorOf(Props[SharedLeveldbStore], "store")
      // register the shared journal

      import actorSystem.dispatcher
      implicit val timeout = Timeout(15 seconds)

      logger debug s"Finding identity of actor with path $actorPath ..."
      val future: Future[Any] = actorSystem.actorSelection(actorPath) ? Identify(None)


      future foreach {
        case ActorIdentity(_, Some(actorRef)) =>
          logger debug s"For the actor-system '${actorSystem.name}', setting the store to $actorRef for shared level journal ..."
          SharedLeveldbJournal.setStore(actorRef, actorSystem)
        case _ =>
          logger error s"Failed to start shard journal at $actorPath"
          logger info  s"Terminating the actor system ${actorSystem.name} ..."
          actorSystem.terminate()
      }
      future.failed.foreach {
        throwable: Throwable =>
          actorSystem.log.error("Lookup of shared journal at {} failed!", actorPath)
          logger error (s"Lookup of shared journal at $actorPath failed!", throwable)
          logger info s"Terminating the actor system ${actorSystem.name} ..."

          actorSystem.terminate()
      }
    }

  }


}
