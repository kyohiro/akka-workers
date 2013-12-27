package akkaworker.workers

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akkaworker.workers.Status._
import akka.actor.ActorLogging

object Worker {
  def props(manager: ActorRef): Props = Props(new Worker(manager))
}

class Worker(val manager: ActorRef) extends Actor 
                                    with ActorLogging {
  
  manager ! JoinWorker
  
  def checkForTask = context.system.scheduler.scheduleOnce(1000 millis, context.self, AskForTask)
  
  def sayJobFinished(id: Long, result: Option[Any]) = context.self ! TaskFinished(id, result)
  
  def receive = notConnected 
  
  def notConnected: Receive = {
    case Welcome => {
      checkForTask
      context.become(waitingForTask)
    }
  }
  
  def waitingForTask: Receive = {
    case AskForTask => {
      manager ! AskForTask
      checkForTask
    } 
    case AssignTask(task) => {
      task.workOnTask onComplete { case x => sayJobFinished(task.id, x.get) } //TODO : No error handling now
      context.become(working)
    } 
  }
   
  def working: Receive = {
    case AskForTask => log.debug("Worker is working now, skip this task demand.") 
    case tf: TaskFinished => {
      manager ! tf
      checkForTask
      context.become(waitingForTask)
    }
  } 
}