package akkaworker.actors

import scala.collection.mutable
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits._
import akkaworker.task.SeqTask
import akka.actor.ActorRef
import akka.actor.TypedActor
import akkaworker.util.SeqGenerator

/** Batch client doesn't allow any failure */
trait BatchClient[T] extends Client[T] 
                     with NoTolerance[T] 
                     with SeqGenerator {
  import Protocol._
  
  /** Manager to send tasks to */
  var manager: ActorRef = null
  
  /** Produce a list of tasks, will be called by dispatchTask */ 
  def produceTasks: Traversable[SeqTask[T]]  
  
  /** Promise and future for the overall status of client */
  val promise = Promise[Traversable[T]]
  def allTasksComplete = promise.future
  
  /** Tasks status fields */
  val tasksSet = mutable.HashSet.empty[Long]
  val results = mutable.HashMap.empty[Long, T] 
  val failures = mutable.HashMap.empty[Long, Throwable]
  
  def joinManager(manager: ActorRef) = {
    this.manager = manager 
    dispatchTasks
  }
    
  def dispatchTasks = {
    val tasks = produceTasks
    tasks.foreach(tasksSet += _.id)
    manager ! RaiseBatchTask(tasks, TypedActor.context.self)
  }
}