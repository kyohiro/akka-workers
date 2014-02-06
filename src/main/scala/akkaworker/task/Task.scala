package akkaworker.task

import scala.concurrent.Future

trait Task[+T] {
  
  /** Future for some thread blocking work */
  def workOnTask: Future[T]
}