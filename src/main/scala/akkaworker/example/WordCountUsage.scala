package akkaworker.example

import java.io.File
import scala.io.Source
import akkaworker.Producer
import akkaworker.system.SingleManagerSystem

object WordCountUsage {
  def tokenize(text: String): TraversableOnce[String] = text.toLowerCase.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
  
}

object WordCountUsageApp extends App {
  
  implicit val system = SingleManagerSystem("BatchSystem", 64)
  
  val files = new File(".").listFiles().toSeq.filter(f => f.isFile && f.getName != ".cache")
  val lines = files.flatMap(Source.fromFile(_) getLines)
  
  val producer = Producer.source(lines)
  val results = producer.map(WordCountUsage.tokenize(_).size.toLong)  
 
}