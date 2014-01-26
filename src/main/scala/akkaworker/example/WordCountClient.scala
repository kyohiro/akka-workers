package akkaworker.example

import java.io.File
import scala.concurrent.Future
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits._
import akkaworker.actors.BatchClient
import akkaworker.task.Task
import akkaworker.util.SeqGenerator
import akkaworker.system.SingleManagerSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class WordCountClient extends BatchClient with SeqGenerator {
  val name = "Word Count Client"
    
  type T = (String, Long)
    
  override def produceTasks = files.map(WordCountTask(nextSeq, _)) 
    
  lazy val files = new File(".").listFiles().toSeq.filter(f => f.isFile && f.getName != ".cache")
  
}

object WordCountTask {
  def apply(id: Long, file: File) = new WordCountTask(id, file)
}

class WordCountTask(val id: Long, val file: File) extends Task {
  type T = (String, Long)
  
  override def workOnTask = Future {Some(file.getName() -> Source.fromFile(file).getLines.map(_.size).sum.toLong)}
  
}

object WordCountExample extends App {
  val client = new WordCountClient 
  val system = SingleManagerSystem("system")
  system.clientJoin(client)
  val fut = client.allTasksComplete
  
  val ret = Await.result(fut, 2 seconds)
  fut foreach println
}