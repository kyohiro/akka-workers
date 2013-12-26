package akkaworker.task

import scala.concurrent.Future

trait Task {
  val id: Long
  def workOnTask: Future[Option[Any]]
}
