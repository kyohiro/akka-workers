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

trait Tools extends SeqGenerator{
  def getRandomTasks(num: Long) = (1L to num).map(id => SomeTask(id)).toList
}

object SomeTask {
  def apply(id: Long) = new SomeTask(id)
}

class SomeTask(val id: Long) extends Task {
  def workOnTask = {
    val blockingTime = (Math.random() * 5000).toLong
    val p = Promise[Option[Long]]
    future {blocking(Thread.sleep(blockingTime))} onComplete {case _ => p.success(Some(blockingTime))} 
    p.future
  }
}

trait SeqGenerator {
  var _seqCounter = 0L
  
  def curSeq =  _seqCounter
  
  def nextSeq = {
    _seqCounter += 1 
    _seqCounter
  }
}

object SomeClient {
  def props(manager: ActorRef): Props = Props(new SomeClient(manager))
}

class SomeClient(val manager: ActorRef) extends SingleBatchTaskClient with Tools with ActorLogging {
  def produceTasks = getRandomTasks(10L) 
  def tasksComplete = {
    log.info("All tasks have been completed.")
    self ! PoisonPill
  } 
}