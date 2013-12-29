package akkaworker

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import akkaworker.workers.Status._
import akkaworker.workers.Manager
import scala.language.postfixOps
import scala.concurrent.duration._

class ManagerSpec extends TestKit(ActorSystem("ManagerSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender {

  override def afterAll(): Unit = system.shutdown()
  
  test("Manager should answer Join") {
    val client = TestProbe()
    val worker = TestProbe()
    
    val manager = system.actorOf(Manager.props)
    client.send(manager, JoinClient)
    worker.send(manager, JoinWorker)
    
    client.expectMsg(Welcome)
    worker.expectMsg(Welcome)
  }
  
  test("Manager should send tasks to workers when they have tasks") {
    val manager = system.actorOf(Manager.props) 
    val client = system.actorOf(SomeClient.props(manager)) 
    
    Thread.sleep(200)
    
    val worker = TestProbe()
    worker.send(manager, JoinWorker)
    worker.expectMsg(Welcome)
   
    worker.reply(AskForTask)
    worker.expectMsgClass(classOf[AssignTask])
    worker.reply(TaskFinished(1, None))
    worker.expectMsgClass(classOf[AssignTask])
  }
  
  test("Manager should tell workers task available when clients join") {
    val manager = system.actorOf(Manager.props) 
    
    val worker = TestProbe()
    worker.send(manager, JoinWorker)
    worker.expectMsg(Welcome)
    
    worker.expectNoMsg(1 second)
   
    val client = system.actorOf(SomeClient.props(manager)) 
    worker.expectMsg(TaskAvailable)
    worker.reply(AskForTask)
    worker.expectMsgClass(classOf[AssignTask])
    worker.reply(TaskFinished(1, None))
    worker.expectMsgClass(classOf[AssignTask])
  }
  
}