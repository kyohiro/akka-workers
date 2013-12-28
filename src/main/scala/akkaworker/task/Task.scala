package akkaworker.task

import scala.concurrent.Future

trait Task {
  val id: Long  //A Task should have any unique id
  def workOnTask: Future[Option[Any]]  
}
