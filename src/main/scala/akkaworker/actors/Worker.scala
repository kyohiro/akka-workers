package akkaworker.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akkaworker.actors.Protocol._

object Worker {
  def props: Props = Props(new Worker)
}

class Worker extends Actor with ActorLogging {
  var manager: ActorRef = null 
  
  def sayJobFinished(id: Long, result: Option[Any]) = context.self ! TaskFinished(id, result)
  
  def askManagerForTask = {
    manager ! AskForTask
    context.become(waitingTaskACK)
  }
  
  def workOnTask(assignedTask: AssignTask) = {
    log.debug("Assigned task, work on it.")
    context.become(working)
    val f = assignedTask.task.workOnTask 
    f onSuccess { case x => sayJobFinished(assignedTask.seq, x) } 
    f onFailure { case x => log.warning(x.getStackTraceString)
                            manager ! TaskFailed(assignedTask.seq, x) } 
  }
  
  //starting in not connected status
  def receive = notConnected 
  
  def notConnected: Receive = {
    case StartWorker(mgr: ActorRef) => {
      if (mgr eq null) log.error("Manager conntected to should not be null!")
      else {
        manager = mgr 
        manager ! JoinWorker 
      }
    }
    case Welcome => askManagerForTask
  }
  
  def waitingForTask: Receive = {
    case TaskAvailable => askManagerForTask 
    case assignedTask: AssignTask => workOnTask(assignedTask) 
  }
  
  def waitingTaskACK: Receive = {
    case NoTaskAvailable => context.become(waitingForTask)
    case assignedTask: AssignTask => workOnTask(assignedTask)
  } 
   
  def working: Receive = {
    case tf: TaskFinished[_] => {
      manager ! tf
      context.become(waitingForTask)
    }
    case a: Any => log.warning("unexpected message, {}", a)
    
  } 
}