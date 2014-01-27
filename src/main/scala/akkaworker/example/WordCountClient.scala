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
    
  type T = Long 
    
  override def produceTasks = files.flatMap(Source.fromFile(_).getLines).map(WordCountTask(nextSeq, _)) 
  
    
  lazy val files = new File(".").listFiles().toSeq.filter(f => f.isFile && f.getName != ".cache")
}

object WordCountTask {
  def apply(id: Long, line: String) = new WordCountTask(id, line)
}

class WordCountTask(val id: Long, val line: String) extends Task {
  type T = Long
  
  override def workOnTask = Future {Some(line.split(" ").size.toLong)}
  
}

object WordCountExample extends App {
  val client = new WordCountClient 
  val system = SingleManagerSystem("system", 64)
  system.clientJoin(client)
  val fut = client.allTasksComplete
  val ret = Await.result(fut, 2 seconds)
  
  val allWords = ret.foldLeft(0l)((sum, x) => x.get + sum) 
  
  println(s"Altogether $allWords words.")
}