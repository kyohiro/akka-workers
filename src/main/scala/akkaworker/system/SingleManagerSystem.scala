package akkaworker.system

import scala.collection.mutable
import akka.actor.{ActorRef, Props}
import akkaworker.actors.{Manager, Worker}
import akkaworker.actors.Protocol._
import akkaworker.actors.Client
import akka.actor.TypedActor
import akka.actor.TypedProps
import scala.concurrent.impl.Future
import scala.concurrent.Future

class SingleManagerSystem(val systemName: String) extends WorkingSystem {
  
  def this(systemName:String, initialWorkers: Int) = {
    this(systemName)
    (1 to initialWorkers).foreach(n => workerJoin(Worker.props))
  }
  
  val manager = system.actorOf(Manager.props, "Manager")
  
  val allFutures = mutable.Set.empty[Future[Traversable[Option[Any]]]]
  
  def clientJoin(client: Client) = {
    val clientActor = TypedActor(system).typedActorOf(TypedProps(classOf[Client], client)) 
    clients += clientActor
    allFutures += client.allTasksComplete
    
    clientActor.joinManager(manager)
  }
  
  def workerJoin(workerProp: Props) = {
    val worker = system.actorOf(workerProp) 
    workers += worker
    worker ! StartWorker(manager)
  }
  
}

object SingleManagerSystem {
  def apply(systemName: String) = new SingleManagerSystem(systemName)
  def apply(systemName: String, workerCount: Int) = new SingleManagerSystem(systemName, workerCount)
}