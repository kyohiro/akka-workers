package com.kyohiro.akkaworker.workers

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._

object Worker {
  case object CheckForTask 
  case class JobFinished(id: Long, result: Option[Any])
  
  def props(manager: ActorRef): Props = Props(new Worker(manager))
}

class Worker(val manager: ActorRef) extends Actor {
  import Manager._ 
  import Worker._
  
  manager ! JoinWorker
  
  def checkForTask = context.system.scheduler.scheduleOnce(1000 millis, context.self, CheckForTask)
  
  def sayJobFinished(id: Long, result: Option[Any]) = context.self ! JobFinished(id, result)
  
  def receive = notConnected 
  
  def notConnected: Receive = {
    case Welcome => {
      checkForTask
      context.become(waitingForTask)
    }
  }
  
  def waitingForTask: Receive = {
    case AskForTask => CheckForTask
    case AssignTask(task) => {
      task.workOnTask onComplete { case x => sayJobFinished(task.id, x.get) } //TODO : No error handling now
      context.become(working)
    } 
  }
   
  def working: Receive = {
    case AskForTask => //working, do not respond to ask for task message
    case JobFinished(id, result) => {
      manager ! TaskFinished(id, result)
      context.become(waitingForTask)
    }
  } 
}