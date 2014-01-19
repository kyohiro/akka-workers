package akkaworker.actors

import scala.concurrent.Future

trait DependentClient extends Client {
  
  /** Depend on some other clients to start */
  def dependents: Traversable[Client]
  
  /** When other dependents are all over, then this client can start */ 
  def canExecute: Future[Boolean]
  
  /** Execute something */
  def doExecute: Unit
  
}