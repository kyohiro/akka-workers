package akkaworker.workers

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akkaworker.workers.Status._
import akkaworker.task.Task
import akka.event.Logging
import akka.actor.ActorLogging

object Manager {
  def props: Props = Props(new Manager)
}

class Manager extends Actor 
              with ActorLogging {
  
  var workers = Set.empty[ActorRef] 
  var workingTasks = scala.collection.mutable.HashMap.empty[Long, Task]
  var newTasks = scala.collection.mutable.Queue.empty[Task]
  
  def getFirstTask: Option[Task] = if (newTasks.isEmpty) None else Some(newTasks.dequeue)
  
  def assignOneTask(worker: ActorRef) = getFirstTask.map(task => {
    worker ! AssignTask(task)
    workingTasks += task.id -> task
  })
 
  def receive = normal 
  
  val normal: Receive = {
    case RaiseTask(task) => {
      newTasks += task
      log.debug(s"Got task $task from Client")
      workers.map(_ ! TaskAvailable)
    }
    
    case RaiseBatchTask(tasks) => {
      newTasks ++= tasks 
      val n = tasks.size
      log.debug(s"Got $n tasks from Client")
      workers.map(_ ! TaskAvailable)
    }
    
    case TaskFinished(id, result) => {
      workingTasks -= id 
      assignOneTask(sender)
    }
    
    case AskForTask => {
      log.debug(s"Worker $sender is asking for task")
      assignOneTask(sender) 
    }
                                     
    case JoinWorker => {
      workers += sender
      sender ! Welcome
    }
    case JoinClient => sender ! Welcome
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
