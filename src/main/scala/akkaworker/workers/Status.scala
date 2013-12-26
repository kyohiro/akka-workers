package akkaworker.workers

import akkaworker.task.Task

object Status {
  //Worker joins
  case object JoinWorker
  //Client joins
  case object JoinClient
  //Ack welcome message
  case object Welcome
  //Worker ask for task
  case object AskForTask
 
  trait Operation
  trait OperationReply
  
  //Client raise task to manager
  case class RaiseTask(task: Task) extends Operation
  //Manager assign task to workers
  case class AssignTask(task: Task) extends Operation
  //Workers ack task finished message
  case class TaskFinished(id: Long, result: Option[Any]) extends OperationReply
}