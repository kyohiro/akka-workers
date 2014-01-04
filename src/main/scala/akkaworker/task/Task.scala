package akkaworker.task

import scala.concurrent.Future

trait Task extends Serializable {
  val id: Long  //A Task should have any unique id within tasks from the same client
  
  def workOnTask: Future[Option[Any]]  
}
