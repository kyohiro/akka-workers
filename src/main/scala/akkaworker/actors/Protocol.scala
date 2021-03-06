package akkaworker.actors

import akkaworker.task.SeqTask
import akka.actor.ActorRef

object Protocol {
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
  case class StartClient(manager: ActorRef) extends Operation                       //Tell client which manager to connect to
  case class StartWorker(manager: ActorRef) extends Operation                       //Tell worker which manager to connect to
  case class RaiseTask(task: SeqTask[_], client: ActorRef) extends Operation                                //Client raise task to Manager
  case class RaiseBatchTask(tasks: Traversable[SeqTask[_]], client: ActorRef) extends Operation             //Client raise a batch of tasks
  case class AssignTask(seq: Long, task: SeqTask[_]) extends Operation                    //Manager assign task to a worker
  case class TaskFinished[T](seq: Long, result: T) extends OperationReply    //Worker says task has been finished
  case class TaskComplete[T](id: Long, result: T) extends OperationReply     //Manager tells client task has been finished
  case class TaskFailed(seq: Long, result: Throwable) extends OperationReply      //Worker tells manager the failure of task
}