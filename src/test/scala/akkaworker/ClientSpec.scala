package akkaworker

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akkaworker.actors.Protocol._

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
  
}