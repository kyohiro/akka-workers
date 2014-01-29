package akkaworker.actors

import scala.concurrent.Future

trait DependentClient[T] extends Client[T] {
  
  /** Depend on some other clients to start */
  def dependents: Traversable[Client[_]]
  
  /** When other dependents are all over, then this client can start */ 
  def canExecute: Future[Boolean]
  
  /** Execute something */
  def doExecute: Unit
  
}