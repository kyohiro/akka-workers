package akkaworker.actors

import akkaworker.actors.Protocol._
import scala.concurrent.Promise

/** Defines whether and how a client handles failed tasks */
trait ResultHandling[T] extends Client[T] {
    
  val promise: Promise[Traversable[Option[T]]]
  
  /** Remove from tasks set, add to results map */
  def processResult(tf: TaskComplete[T]) = {
    tasksSet -= tf.id
    results += tf.id -> tf.result
    checkTasksStatus
  }
  
  /** Remove from tasks set, add to failure set, and check overall status */
  def processFailure(tf: TaskFailed) = {
    tasksSet -= tf.seq
    failures += tf.seq -> tf.result 
    checkTasksStatus
  } 
  
  def checkTasksStatus: Unit
}
