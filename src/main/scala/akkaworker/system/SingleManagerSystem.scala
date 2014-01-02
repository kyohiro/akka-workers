package akkaworker.system

import akkaworker.workers.Manager
import akka.actor.ActorRef
import akkaworker.workers.Worker
import akka.actor.Props
import akkaworker.workers.Status._

class SingleManagerSystem(val systemName: String, workerCount: Int) extends WorkingSystem {
  require(workerCount > 0, "Should have at least one worker")
  
  val manager = system.actorOf(Manager.props, "Manager")
  var workers = (for (i <- 0 to workerCount - 1) yield system.actorOf(Worker.props(manager), "Worker" + i)) toSet
  var clients = Set.empty[ActorRef]
  
  def clientJoin(clientProp: Props) = {
    val count = clients.size
    val client = system.actorOf(clientProp, "Client" + count)
    clients += client
    
    client ! StartClient(manager)
  }
  
}

object SingleManagerSystem {
  def apply(systemName: String, workerCount: Int) = new SingleManagerSystem(systemName, workerCount)
}