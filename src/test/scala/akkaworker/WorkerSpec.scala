package akkaworker

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.ImplicitSender
import akkaworker.workers.Manager
import akkaworker.workers.Status._
import akka.testkit.TestProbe
import scala.concurrent.duration._
import scala.language.postfixOps


class WorkerSpec extends TestKit(ActorSystem("WorkerSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender {

  override def afterAll(): Unit = system.shutdown()
  
  test("Worker should receive tasks from manager") {
    val manager = system.actorOf(Manager.props, "Manager") 
    val client = system.actorOf(SomeClient.props(manager))
    val worker1 = TestProbe()
    val worker2 = TestProbe() 
    
    worker1.send(manager, JoinWorker)
    worker1.expectMsg(Welcome)
    worker2.send(manager, JoinWorker)
    worker2.expectMsg(Welcome)
    
    worker1.expectNoMsg(500 millis)
    
    worker1.send(manager, AskForTask)
    worker1.expectMsgClass(classOf[AssignTask])
    worker2.send(manager, AskForTask)
    worker2.expectMsgClass(classOf[AssignTask])
    
    worker1.reply(TaskFinished(1L, None))
    worker1.expectMsgClass(classOf[AssignTask])
    worker2.reply(TaskFinished(2L, Some(5L)))
    worker2.expectMsgClass(classOf[AssignTask])  
  }
  
}