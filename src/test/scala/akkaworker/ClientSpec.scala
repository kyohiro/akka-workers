package akkaworker

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akkaworker.actors.Protocol._
import akka.actor.TypedActor
import akka.actor.TypedProps
import akkaworker.actors.Client
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps._
import akka.util.Timeout

class ClientSpec extends TestKit(ActorSystem("ClientSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender
                 with Tools {

  override def afterAll(): Unit = system.shutdown()
  
  test("Random tasks should finish in 0 to 1 seconds") {
    val tasks = getRandomTasks(100) 
    val fut = tasks.map(task => task.workOnTask)
    
    Thread.sleep(500)
    
    fut.foreach(f => f.isCompleted should be (true)) 
  }
  
  test("Manager should receive the tasks from Client") {
    val manager = TestProbe()
    val client = TypedActor(system).typedActorOf(TypedProps(classOf[Client], new SomeClient("client")))
    client.joinManager(manager.ref)
    manager.expectMsgClass(classOf[RaiseBatchTask])
  }
  
  test("Client future should tell when the task is completed and return results") {
    val manager = TestProbe()
    var ret = List.empty[Option[Any]]
    val client = TypedActor(system).typedActorOf(TypedProps(classOf[Client], new SomeClient("client")))
    val fut = client.allTasksComplete
    
    client.joinManager(manager.ref)
    manager.expectMsgClass(classOf[RaiseBatchTask]) 
    
    (1 to 10).foreach(n => client.onReceive(TaskComplete(n, Some(n)), manager.ref))
     
    Await.result(fut, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (10)
  }
  
}