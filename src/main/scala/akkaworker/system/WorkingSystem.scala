package akkaworker.system

import akka.actor.ActorSystem
import akkaworker.workers.Manager
import akkaworker.workers.Worker
import akka.actor.ActorRef
import akka.actor.Props
import scala.collection.immutable.Set

trait WorkingSystem {
  val systemName: String
  
  val system = ActorSystem(systemName)
  
  var workers: Set[ActorRef] 
  
  var clients: Set[ActorRef]
  
  //to be implemented
  def clientJoin(clientProp: Props)
  
  //to be implemented
  def workerJoin(workerProp: Props)
  
  def shutdown = system.shutdown()
  
  def allClientsClosed = clients.forall(_.isTerminated)
}
