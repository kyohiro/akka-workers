package akkaworker.actors

import akka.actor.{Actor, ActorRef, PoisonPill, ActorLogging}
import akkaworker.task.Task
import scala.concurrent.{Future, Promise}


/**
 * Client will send tasks to the manager and wait for results. 
 */
trait Client extends Actor 
             with ActorLogging {
  import Protocol._
  
  //to be implemented
  def dispatchTasks: Unit
  
  //to be implemented
  def processResult(tf: TaskComplete): Unit
  
  //to be implemented
  def processFailure(tf: TaskFailed): Unit
  
  var manager: ActorRef = null
  
  //Starting in not connected status
  def receive = notConnected
  
  def notConnected: Receive = {
    case StartClient(mgr: ActorRef) => {
      if (mgr eq null) log.error("Manager conntected to should not be null!")
      else {
        manager = mgr 
        manager !  JoinClient
      }
    }
    //Dispatch tasks and wait for responses
    case Welcome => {
      dispatchTasks
      context.become(tasksSent)
    }
  }
  
  def tasksSent: Receive = {
    case tc: TaskComplete => processResult(tc)  
    case tf: TaskFailed => processFailure(tf)
  }
}
