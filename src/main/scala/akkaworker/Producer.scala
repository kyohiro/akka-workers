package akkaworker

import akkaworker.builder.ClientBuilder
import scala.concurrent.Future
import akkaworker.system.WorkingSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akkaworker.system.WorkingSystem
import akkaworker.system.WorkingSystem

object Producer {
  def source[T](source: Traversable[T]): Source[T] = Source(source) 
  
  def result[U](result: Future[Traversable[U]]): Result[U] = Result(result)
  
  def traverse[T](producer: Producer[T]): Traversable[T] = producer match {
    case Source(source) => source  
    case Result(result) => throw new UnsupportedOperationException("Result is future, does not support traverse operation.")
  }
    
}

sealed trait Producer[+T] {
  
  def map[U](fn: T => U)(implicit system: WorkingSystem): Producer[U] = ClientBuilder(this).map(fn)
  
  def foreach[U](fn: T => U)(implicit system: WorkingSystem): Unit = ClientBuilder(this).foreach(fn)
    
  def filter(fn: T => Boolean)(implicit system: WorkingSystem): Producer[T] = ClientBuilder(this).filter(fn) 
  
  def flatMap[U](fn: T => TraversableOnce[U])(implicit system: WorkingSystem): Producer[U] = ClientBuilder(this).flatMap(fn) 
  
  def optionMap[U](fn: T => Option[U])(implicit system: WorkingSystem): Producer[U] =  ClientBuilder(this).optionMap(fn)
  
}


case class Source[+T](source: Traversable[T]) extends Producer[T]

case class Result[+U](result: Future[Traversable[U]]) extends Producer[U]