package akkaworker.workers

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import akkaworker.task.Task

trait Client extends Actor {
  import Status._
  
  val manager: ActorRef
  
  manager ! JoinClient
  
  def generateTasks: List[Task] 
   
  def receive = notConnected
  
  def notConnected: Receive = {
    case Welcome => {
      context.become(sendingTask)
      generateTasks.foreach(manager ! RaiseTask(_))
    }
  }
  
  def sendingTask: Receive = {
    case _ =>
  }
}