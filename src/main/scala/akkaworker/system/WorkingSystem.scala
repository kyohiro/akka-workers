package akkaworker.system

import akka.actor.{ActorRef, ActorSystem, Props}

trait WorkingSystem {
  val systemName: String
  
  val system = ActorSystem(systemName)
  
  var workers = Set.empty[ActorRef]
  
  var clients = Set.empty[ActorRef]
  
  //to be implemented
  def clientJoin(clientProp: Props)
  
  //to be implemented
  def workerJoin(workerProp: Props)
  
  def shutdown = system.shutdown()
  
  def allClientsClosed = clients.forall(_.isTerminated)
}
