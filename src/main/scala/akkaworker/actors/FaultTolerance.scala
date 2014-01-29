package akkaworker.actors

import akkaworker.util.TaskException

/** A client accepting all failed tasks */
trait AllTolerance[T] extends ResultHandling[T] {
  this: Client[T] =>
    
  /** Don't care if any failure, just mark it as success */
  def checkTasksStatus = if (tasksSet.isEmpty) promise.success(results.values) 
}

/** A client doesn't accept any failure of its tasks */
trait NoTolerance[T] extends ResultHandling[T] {
  this: Client[T] =>
  
  /** If there is any failure, mark the whole client as failed */
  def checkTasksStatus = {
    if (!failures.isEmpty && !promise.isCompleted) promise.failure(new TaskException(s"Client $name failed", failures.head._2))
    else if (tasksSet.isEmpty && failures.isEmpty) promise.success(results.values)
  }
} 