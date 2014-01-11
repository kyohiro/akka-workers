package akkaworker.actors

import akka.actor.{Actor, ActorRef, PoisonPill, ActorLogging}
import akkaworker.task.Task
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits._

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

/**
 * Client will send a batch of tasks to the manager only once.
 * After all the results return, the client can be closed.
 */
trait SingleBatchTaskClient extends Client {
  import Protocol._
  
  //to be implemented
  def produceTasks: Iterable[Task]  
  
  val promise = Promise[Iterable[Option[Any]]]
  val tasksDone = promise.future 
  
  val tasksSet = scala.collection.mutable.Set.empty[Long]
  var results = Map.empty[Long, Option[Any]] 
  
  def whenAllTasksFinish[T](callback: Iterable[Option[Any]] => T) = tasksDone.onSuccess{case results => callback(results)}
    
  override def dispatchTasks = {
    val tasks = produceTasks
    tasks.map(tasksSet += _.id)
    manager ! RaiseBatchTask(tasks)
  }
  
  override def processResult(tf: TaskComplete) = {
    results += tf.id -> tf.result
    tasksSet -= tf.id
    allTasksDone 
  }
  
  override def processFailure(tf: TaskFailed) = {
    tasksSet -= tf.seq
    allTasksDone
  }
  
  def allTasksDone = if (tasksSet.isEmpty) {
    promise.success(results.values)
    log.info("All tasks has been finished. Closing this Client.")
    self ! PoisonPill
  }
}
