package akkaworker.actors

import scala.collection.mutable
import scala.concurrent.Future
import akka.actor.ActorRef
import akka.actor.TypedActor.Receiver

/**
 * Client will send tasks to the manager and wait for results. 
 */
trait Client[T] extends Receiver { 
  import Protocol._
  
  /** Client name */
  def name: String = "Anonymous Client"
  
  /** Tasks Set for keeping all task Id */ 
  def tasksSet: mutable.HashSet[Long]
  
  /** Results back from manager */
  def results: mutable.Map[Long, T] 
  
  /** Failed task id */
  def failures: mutable.Map[Long, Throwable] 
  
  def joinManager(manager: ActorRef): Unit
  
  /** Dispatch all the tasks to the manager */
  def dispatchTasks: Unit
  
  /** Define how to process successful results */
  def processResult(tf: TaskComplete[T]): Unit
  
  /** Define how to process failures */
  def processFailure(tf: TaskFailed): Unit
  
  /** Check how are all tasks going */
  def checkTasksStatus: Unit
  
  /** The future when all tasks have completed (succeeded or failed) */
  def allTasksComplete: Future[Traversable[T]]
  
  /** Handles manager's feedback */
  def onReceive(message: Any, sender: ActorRef) = message match {
    case tc :TaskComplete[T] => processResult(tc)
    case tf: TaskFailed => processFailure(tf)
  }
}

