package akkaworker

import scala.concurrent._
import akkaworker.task.Task
import ExecutionContext.Implicits.global
import akkaworker.workers.Client
import akka.actor.ActorRef
import akka.actor.Props
import akkaworker.workers.SingleBatchTaskClient
import akka.actor.ActorLogging
import scala.actors.PoisonPill
import akkaworker.workers.SeqGenerator
import akkaworker.workers.Worker

trait Tools extends SeqGenerator{
  def getRandomTasks(num: Long, timeLimit: Int = 5000) = (1L to num).map(id => SomeTask(id, timeLimit)).toList
  
}

object SomeTask {
  def apply(id: Long, timeLimit: Int) = new SomeTask(id, timeLimit)
}

class SomeTask(val id: Long, timeLimit: Int) extends Task {
  def workOnTask = {
    val blockingTime = (Math.random() * timeLimit).toLong
    val p = Promise[Option[Long]]
    future {blocking(Thread.sleep(blockingTime))} onComplete {case _ => p.success(Some(blockingTime))} 
    p.future
  }
}


object SomeClient {
  def props(manager: ActorRef): Props = Props(new SomeClient(manager))
}

class SomeClient(val manager: ActorRef) extends SingleBatchTaskClient with Tools { 
  def produceTasks = getRandomTasks(10L) 
  def tasksComplete = {
    log.info("All tasks have been completed.")
  } 
}

object MillionsTaskClient {
  def props(manager: ActorRef): Props = Props(new MillionsTasksClient(manager))
}

class MillionsTasksClient(val manager: ActorRef) extends SingleBatchTaskClient with Tools {
  def produceTasks = getRandomTasks(5L, timeLimit = 100) 
  def tasksComplete = {
    log.info("All tasks have been completed.")
  }
}

