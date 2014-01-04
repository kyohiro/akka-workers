package akkaworker.system

import akka.actor.{ActorRef, Props}
import akkaworker.workers.{Manager, Worker}
import akkaworker.workers.Protocol._

class SingleManagerSystem(val systemName: String) extends WorkingSystem {
  
  val manager = system.actorOf(Manager.props, "Manager")
  
  def clientJoin(clientProp: Props) = {
    val client = system.actorOf(clientProp)
    clients += client
    
    client ! StartClient(manager)
  }
  
  def workerJoin(workerProp: Props) = {
    val worker = system.actorOf(workerProp) 
    workers += worker
    
    worker ! StartWorker(manager)
  }
  
}

object SingleManagerSystem {
  def apply(systemName: String) = new SingleManagerSystem(systemName)
  def apply(systemName: String, workerCount: Int) = {
    val s = new SingleManagerSystem(systemName)
    (1 to workerCount).foreach(n => s.workerJoin(Worker.props))
    s
  }
}