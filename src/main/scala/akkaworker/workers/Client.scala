package akkaworker.workers

import akka.actor.{Actor, ActorRef}
import akkaworker.task.Task
import akka.actor.PoisonPill
import akka.actor.ActorLogging

/**
 * Client will send tasks to the manager and wait for results. 
 */
trait Client extends Actor 
             with ActorLogging {
  import Status._
  
  //to be implemented
  def dispatchTasks: Unit
  
  //to be implemented
  def processResult(tf: TaskComplete): Unit
  
  //to be implemented
  def processFailure(tf: TaskFailed): Unit
  
  //to be implemented
  def tasksComplete: Unit
  
  val manager: ActorRef
  manager !  JoinClient
  
  //Starting in not connected status
  def receive = notConnected
  
  def notConnected: Receive = {
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
  import Status._
  
  //to be implemented
  def produceTasks: Iterable[Task]
  
  val tasksSet = scala.collection.mutable.Set.empty[Long]
  var results = Map.empty[Long, Option[Any]] 
  
  def dispatchTasks = {
    val tasks = produceTasks
    tasks.map(tasksSet += _.id)
    manager ! RaiseBatchTask(tasks)
  }
  
  def allTasksDone = if (tasksSet.isEmpty) {
    tasksComplete 
    log.info("All tasks has been finished. Closing this Client.")
    manager ! GoodBye
    self ! PoisonPill
  }
  
  def processResult(tf: TaskComplete) = {
    results += tf.id -> tf.result
    tasksSet -= tf.id
    allTasksDone 
  }
  
  def processFailure(tf: TaskFailed) = {
    tasksSet -= tf.seq
    allTasksDone
  }
}
