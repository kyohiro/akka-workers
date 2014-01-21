package akkaworker.task

import scala.concurrent.Future

trait Task extends Serializable {
  
  /** Type of the return value of task */
  type T
  
  /** A Task should have any unique id within tasks from the same client */
  val id: Long  
  
  /** Future for some thread blocking work */
  def workOnTask: Future[Option[T]]  
  
  override def hashCode = id.toInt 
  
  override def equals(obj: Any) = obj match {
    case t: Task => t.id == this.id
    case _ => false
  }
}
