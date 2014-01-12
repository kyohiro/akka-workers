package akkaworker.actors

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.PoisonPill
import akkaworker.task.Task


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
