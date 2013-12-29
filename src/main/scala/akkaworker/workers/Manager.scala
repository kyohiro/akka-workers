package akkaworker.workers

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akkaworker.workers.Status._
import akkaworker.task.Task
import akka.event.Logging
import akka.actor.ActorLogging

object Manager {
  case class TaskSeq(seq: Long, task: Task, client: ActorRef)
  def props: Props = Props(new Manager)
}

class Manager extends Actor 
              with ActorLogging 
              with SeqGenerator {
  import Manager._
  
  var workers = Set.empty[ActorRef] 
  val newTasks = scala.collection.mutable.Queue.empty[TaskSeq]
  val tasksMap = scala.collection.mutable.HashMap.empty[Long, TaskSeq]
  
  def enqueTask(task: Task) = {
    val seq = nextSeq
    val taskSeq = TaskSeq(seq, task, sender)
    tasksMap += seq -> taskSeq
    newTasks += taskSeq
  }
  
  def getFirstTask: Option[TaskSeq] = if (newTasks.isEmpty) None else Some(newTasks.dequeue)
  
  def assignOneTask(worker: ActorRef) = getFirstTask match {
    case Some(task) => worker ! AssignTask(task.seq, task.task)
    case None => worker ! NoTaskAvailable
    
  }
 
  def receive = normal 
  
  val normal: Receive = {
    case RaiseTask(task) => {
      enqueTask(task) 
      workers.map(_ ! TaskAvailable)
      
      log.info("Got task {} from Client {}, totally {} tasks inbox now.", task, sender, newTasks.size)
      log.info("Broadcast Task Available message")
    }
    
    case RaiseBatchTask(tasks) => {
      tasks.map(enqueTask(_))
      workers.map(_ ! TaskAvailable)
      
      log.info("Got {} tasks from Client {}, totally {} tasks inbox now.", tasks.size, sender, newTasks.size)
      log.info("Broadcast Task Available")
    }
    
    case TaskFinished(seq, result) => {
      val taskSeq = tasksMap.get(seq) 
      tasksMap -= seq
      taskSeq.map(ts => ts.client ! TaskComplete(ts.task.id, result))
      assignOneTask(sender)
      
      log.debug(s"$sender finished task $seq, try assigning another one.")
      cnt += 1
      log.debug("Finished {} tasks in total", cnt)
    }
    
    case AskForTask => {
      assignOneTask(sender) 
      log.debug(s"Worker $sender is asking for task")
    }
                                     
    case JoinWorker => {
      workers += sender
      sender ! Welcome
    }
    case JoinClient => sender ! Welcome
  }
  
  var cnt = 0
}

trait SeqGenerator {
  var _seqCounter = 0L
  
  def curSeq =  _seqCounter
  
  def nextSeq = {
    _seqCounter += 1 
    _seqCounter
  }
}
