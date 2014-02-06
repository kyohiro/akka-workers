package akkaworker.builder

import akkaworker.Producer
import akkaworker.task.SeqTask
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import akkaworker.actors.BatchClient
import akkaworker.system.SingleManagerSystem
import akkaworker.system.WorkingSystem
import akkaworker.Result

object ClientBuilder {
  def apply[T](producer: Producer[T]) = new ClientBuilder(producer)
}

class ClientBuilder[T] private (val producer: Producer[T]) {
  
  def map[U](fn: T => U)(implicit system: WorkingSystem): Result[U] = Producer.result(mapFunction(producer, fn, system)) 
     
  def foreach[U](fn: T => U)(implicit system: WorkingSystem): Unit = mapFunction(producer, fn, system) 
  
  def filter(fn: T => Boolean)(implicit sytem: WorkingSystem): Result[T] = ???
  
  def flatMap[U](fn: T => TraversableOnce[U])(implicit system: WorkingSystem): Result[U] = ???
  
  def optionMap[U](fn: T => Option[U])(implicit system: WorkingSystem): Result[U] = ??? 
  
  def mapFunction[T, U](producer: Producer[T], fn: T => U, system: WorkingSystem): Future[Traversable[U]] = {
    val client = BuilderClient(producer, fn)
    system.clientJoin(client)
    client.allTasksComplete
  }
}

case class BuilderTask[T, U](id: Long, source: T, fn: T => U) extends SeqTask[U] {
  override def workOnTask = Future {fn(source)} 
}

case class BuilderClient[T, U](producer: Producer[T], fn: T => U) extends BatchClient[U] {
  override def produceTasks = Producer.traverse(producer).map((t: T) => BuilderTask(nextSeq, t, fn))
} 