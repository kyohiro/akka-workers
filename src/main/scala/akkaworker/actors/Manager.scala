package akkaworker.actors

import scala.collection.mutable
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akkaworker.task.Task
import akkaworker.actors.Protocol._


object Manager {
  case class TaskSeq(seq: Long, task: Task, client: ActorRef)
  def props: Props = Props(new Manager)
}

class Manager extends Actor 
              with ActorLogging 
              with SeqGenerator {
  import Manager._
  
  //Mutable collections for workers and clients status connected to this manager
  val workers = mutable.HashSet.empty[ActorRef] 
  val clientsStatus = mutable.HashMap.empty[ActorRef, Boolean] 
  
  //Tasks status, task sequence here is unique within this manager, it's different from the task's id
  val newTasks = scala.collection.mutable.Queue.empty[TaskSeq]
  val tasksMap = scala.collection.mutable.HashMap.empty[Long, TaskSeq]
  
  //To know if the client(s) has finished all tasks
  private[this] def clientCompleted(client: ActorRef) = clientsStatus.getOrElse(client, false)
  private[this] def allClientsCompleted = clientsStatus.forall(_._2 == true) 
  
  private[this] def enqueTask(task: Task) = {
    val seq = nextSeq
    val taskSeq = TaskSeq(seq, task, sender)
    tasksMap += seq -> taskSeq
    newTasks += taskSeq
  }
  
  private[this] def getFirstTask: Option[TaskSeq] = if (newTasks.isEmpty) None else Some(newTasks.dequeue)
  
  private[this] def assignOneTask(worker: ActorRef) = getFirstTask match {
    case Some(task) => worker ! AssignTask(task.seq, task.task); log.debug("Assigned task {} to {}", task.seq, worker)
    case None => worker ! NoTaskAvailable
  }
  
  private[this] def tellTaskAvail = if (!newTasks.isEmpty) sender ! TaskAvailable
 
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
      log.debug(s"$sender finished task $seq")
      
      tellTaskAvail
    }
    
    case TaskFailed(seq: Long) => {
      val taskSeq = tasksMap.get(seq) 
      tasksMap -= seq
      taskSeq.map(ts => ts.client ! TaskFailed(ts.task.id))
      log.debug(s"$sender failed task $seq")
       
      tellTaskAvail
    }
    
    case AskForTask => {
      assignOneTask(sender) 
      log.debug(s"Worker $sender is asking for task")
    }
                                     
    case JoinWorker => {
      workers.add(sender) 
      context.watch(sender)
      sender ! Welcome
    }
    case JoinClient => {
      clientsStatus.put(sender, false)
      context.watch(sender)
      sender ! Welcome
    }
    
    case Terminated(actor) => {
      if (clientsStatus.contains(actor)) clientsStatus.update(actor, true)
      else workers.remove(actor)
    }
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
