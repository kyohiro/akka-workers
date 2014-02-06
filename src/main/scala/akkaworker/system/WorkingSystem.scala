package akkaworker.system

import scala.concurrent.Future

import akka.actor.{ActorRef, ActorSystem, Props}
import akkaworker.actors.Client

trait WorkingSystem {
  val systemName: String
  
  val system = ActorSystem(systemName)
  
  var workers = Set.empty[ActorRef]
  
  var clients = Set.empty[Client[_]]
  
  val allFutures: Iterable[Future[Traversable[_]]]
  
  /** When a client joins the manager, to be implemented */
  def clientJoin(client: Client[_])
  
  /** When a worker joins the manager, to be implemented */
  def workerJoin(workerProp: Props)
  
  def shutdown = system.shutdown()
  
  def allCompleted = allFutures.forall(f => f.isCompleted == true)
  
}
