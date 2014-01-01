package akkaworker.system

import akka.actor.ActorSystem
import akkaworker.workers.Manager
import akkaworker.workers.Worker
import akka.actor.ActorRef
import akka.actor.Props


class WorkingSystem(systemName: String, managerProp: Props, workerProp: Props, workersCount: Int){
  val system = ActorSystem(systemName) 
  val manager = system.actorOf(managerProp)
  val workers = for (idx <- 1 to workersCount) yield system.actorOf(workerProp)
  
}