package akkaworker

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akkaworker.workers.Status._

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
    val client = system.actorOf(SomeClient.props)
    client ! StartClient(manager.ref)
    manager.expectMsg(JoinClient)
    manager.send(client, Welcome)
    manager.expectMsgClass(classOf[RaiseBatchTask])
  }
  
  test("Client should quit gracefully after receiving all results") {
    val manager = TestProbe()
    val client = system.actorOf(SomeClient.props, "someclient")
    client ! StartClient(manager.ref)
    manager.expectMsg(JoinClient)
    manager.send(client, Welcome)
    manager.expectMsgClass(classOf[RaiseBatchTask])
    
    (1 to 10).map(id => manager.send(client, TaskComplete(id, None)))
    manager.expectMsg(GoodBye)
  }
  
}