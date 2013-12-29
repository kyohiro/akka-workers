package akkaworker.workers

import akkaworker.task.Task

object Status {
  sealed trait Message
  case object JoinWorker extends Message     //Worker tells Manager to join
  case object JoinClient extends Message     //Client tells Manager to join
  case object Welcome extends Message        //Manager ACK to worker for welcome
  case object GoodBye extends Message        //When client/worker leaves the cluster
  case object TaskAvailable extends Message  //Manager tells all workers that task is available
  case object NoTaskAvailable extends Message//Manager tells workers that no task is available now
  case object AskForTask extends Message     //Worker ask for task
 
  sealed trait Operation
  sealed trait OperationReply
  case class RaiseTask(task: Task) extends Operation                                //Client raise task to Manager
  case class RaiseBatchTask(tasks: Iterable[Task]) extends Operation                //Client raise a batch of tasks
  case class AssignTask(seq: Long, task: Task) extends Operation                    //Manager assign task to a worker
  case class TaskFinished(seq: Long, result: Option[Any]) extends OperationReply    //Worker says task has been finished
  case class TaskComplete(id: Long, result: Option[Any]) extends OperationReply     //Manager tells client task has been finished
}