package akkaworker.task

import scala.concurrent.Future

trait Task[+T] {
  
  /** A Task should have any unique id within tasks from the same client */
  val id: Long  
  
  /** Future for some thread blocking work */
  def workOnTask: Future[Option[T]]  
}
