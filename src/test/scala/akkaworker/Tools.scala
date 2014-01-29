package akkaworker

import scala.concurrent._
import akkaworker.task.Task
import ExecutionContext.Implicits.global
import akkaworker.actors.Client
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorLogging
import scala.actors.PoisonPill
import akkaworker.util.SeqGenerator
import akkaworker.actors.Worker
import akkaworker.actors.BatchClient
import scala.annotation.tailrec

trait Tools extends SeqGenerator{
  def getRandomTasks(num: Long, timeLimit: Int = 50000000, failRate: Int = 0) = (1L to num).map(id => SomeTask(id, timeLimit, failRate)).toList
}

object SomeTask {
  def apply(id: Long, timeLimit: Int, failRate: Int = 0) = new SomeTask(id, timeLimit, failRate)
}

class SomeTask(val id: Long, timeLimit: Int, failureRate: Int) extends Task[Int] {
  type T = Int
  @tailrec
  private def calc(n: Int, acc: Int): Int = if (n == 0) 1 else calc(n-1, acc+1)
  def workOnTask = {
    val blockingTime = (Math.random() * timeLimit).toInt + 100000000
    val p = Promise[Option[Int]]
    val failed = Math.random() * failureRate
    val f = future {if(failed < 50) calc(blockingTime, 0) else throw new Exception("Calc failed") } 
    f onSuccess {case x => p.success(Some(blockingTime))} 
    f onFailure {case x => p.failure(x)}
    p.future
  }
}

//The type of results should match the type declaration
class ListTask(val id: Long) extends Task[List[String]] {
  type T = List[String]
  def workOnTask = Future {Some(List.empty[String])}
}

class SomeClient(val name: String) extends BatchClient[Int] with Tools { 
  def produceTasks = getRandomTasks(10L) 
}

class MillionsTaskClient(val name: String) extends BatchClient[Int] with Tools {
  def produceTasks = getRandomTasks(5000L, timeLimit = 100) 
}

class FailureTaskClient(val name: String) extends BatchClient[Int] with Tools {
  def produceTasks = getRandomTasks(100L, timeLimit = 100, failRate = 100)
}

