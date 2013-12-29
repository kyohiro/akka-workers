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
  
  def sayJobFinished(id: Long, result: Option[Any]) = context.self ! TaskFinished(id, result)
  
  //starting in not connected status
  def receive = notConnected 
  
  def notConnected: Receive = {
    case Welcome => {
      manager ! AskForTask 
      context.become(waitingForTask)
    }
  }
  
  def waitingForTask: Receive = {
    case TaskAvailable => {
      manager ! AskForTask
    }
    case AssignTask(task) => {
      task.workOnTask onComplete { case x => sayJobFinished(task.id, x.get) } //TODO : No error handling now
      context.become(working)
    } 
  }
   
  def working: Receive = {
    case tf: TaskFinished => {
      manager ! tf
      context.become(waitingForTask)
    }
  } 
}