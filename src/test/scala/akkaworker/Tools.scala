package akkaworker

import scala.concurrent._
import akkaworker.task.Task
import ExecutionContext.Implicits.global
import akkaworker.actors.Client
import akka.actor.ActorRef
import akka.actor.Props
import akkaworker.actors.SingleBatchTaskClient
import akka.actor.ActorLogging
import scala.actors.PoisonPill
import akkaworker.actors.SeqGenerator
import akkaworker.actors.Worker
import scala.annotation.tailrec

trait Tools extends SeqGenerator{
  def getRandomTasks(num: Long, timeLimit: Int = 50000000, failRate: Int = 0) = (1L to num).map(id => SomeTask(id, timeLimit, failRate)).toList
}

object SomeTask {
  def apply(id: Long, timeLimit: Int, failRate: Int = 0) = new SomeTask(id, timeLimit, failRate)
}

class SomeTask(val id: Long, timeLimit: Int, failureRate: Int) extends Task {
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


object SomeClient {
  def props: Props = Props(new SomeClient)
}

class SomeClient extends SingleBatchTaskClient with Tools { 
  def produceTasks = getRandomTasks(10L) 
  def tasksComplete = {
    log.info("All tasks have been completed.")
  } 
}

object MillionsTaskClient {
  def props: Props = Props(new MillionsTaskClient)
}

class MillionsTaskClient extends SingleBatchTaskClient with Tools {
  def produceTasks = getRandomTasks(5000L, timeLimit = 100) 
  def tasksComplete = {
    log.info("All tasks have been completed.")
  }
}

object FailureTaskClient {
  def props(manager: ActorRef): Props = Props(new FailureTaskClient)
}

class FailureTaskClient extends SingleBatchTaskClient with Tools {
  def produceTasks = getRandomTasks(100L, timeLimit = 100, failRate = 100)
  def tasksComplete = {
    log.info("All tasks have been completed.")
  }
}

