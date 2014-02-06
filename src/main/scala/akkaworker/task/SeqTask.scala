package akkaworker.task

import scala.concurrent.Future

trait SeqTask[+T] extends Task[T]{
  
  /** A Task should have any unique id within tasks from the same client */
  val id: Long  
}
