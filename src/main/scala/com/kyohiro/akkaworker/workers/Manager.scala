package com.kyohiro.akkaworker.workers
import akka.actor.Actor
import com.kyohiro.akkaworker.task.Task
import com.kyohiro.akkaworker.task.Task
import akka.actor.ActorRef
import akka.actor.Props

object Manager {
  //Worker joins
  case object JoinWorker
  //Client joins
  case object JoinClient
  //Ack welcome message
  case object Welcome
  //Worker ask for task
  case object AskForTask
 
  //Client raise task to manager
  case class RaiseTask(task: Task) 
  //Manager assign task to workers
  case class AssignTask(task: Task)
  //Workers ack task finished message
  case class TaskFinished(id: Long, result: Option[Any])
  
  def props: Props = Props(new Manager())
}


class Manager extends Actor {
  import Manager._
  
  var workers = Set.empty[ActorRef] 
  var clients = Set.empty[ActorRef]
  
  var workingTasks = scala.collection.mutable.HashMap.empty[Long, Task]
  var newTasks = scala.collection.mutable.Queue.empty[Task]
  
  def getFirstTask: Option[Task] = if (newTasks.isEmpty) None else Some(newTasks.dequeue)
  
  def assignOneTask(worker: ActorRef) = getFirstTask.map(task => {
    worker ! AssignTask(task)
    workingTasks += task.id -> task
  })
 
  def receive = normal 
  
  val normal: Receive = {
    case RaiseTask(task) => newTasks.enqueue(task)  
      
    case TaskFinished(id, result) => {
      workingTasks -= id 
      assignOneTask(sender)
    }
    
    case AskForTask => assignOneTask(sender) 
                                     
    case JoinWorker => workers += sender
                       sender ! Welcome
    case JoinClient => clients += sender
                       sender ! Welcome
  }
}

trait SeqGenerator {
  var _seqCounter = 0L
  
  def curSeq =  _seqCounter
  
  def nextSeq = {
    _seqCounter += 1 
    _seqCounter
  }
}