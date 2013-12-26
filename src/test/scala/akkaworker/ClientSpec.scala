package akkaworker

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.ImplicitSender
import scala.concurrent.Await
import akkaworker.workers.Client
import akkaworker.workers.Status._
import akka.testkit.TestProbe

class ClientSpec extends TestKit(ActorSystem("ClientSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender
                 with Tools {

  override def afterAll(): Unit = system.shutdown()
  
  test("Random tasks should finish in 0 to 5 seconds") {
    val tasks = getRandomTasks(100) 
    val fut = tasks.map(task => task.workOnTask)
    
    Thread.sleep(5500)
    
    fut.foreach(f => f.isCompleted should be (true)) 
  }
  
  test("Manager should receive the tasks from Client") {
    val manager = TestProbe()
    val client = system.actorOf(SomeClient.props(manager.ref))
    
    manager.expectMsg(JoinClient)
    manager.reply(Welcome)
    
    (1 to 10).map(x => manager.expectMsgClass(classOf[RaiseTask]))
  }
}