package akkaworker

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akkaworker.workers.Manager
import akkaworker.workers.Status._
import akkaworker.workers.Worker


class WorkerSpec extends TestKit(ActorSystem("WorkerSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender 
                 with Tools {

  override def afterAll(): Unit = system.shutdown()
  
  test("Worker should receive tasks from manager") {
    val manager = system.actorOf(Manager.props) 
    val client = system.actorOf(SomeClient.props(manager))
    val worker1 = TestProbe()
    val worker2 = TestProbe() 
    
    Thread.sleep(200) //Give some time to ensure workers join after client gives tasks
    
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

  test("Workers should handles amounts of small tasks quickly") {
    val manager = system.actorOf(Manager.props) 
    val client = system.actorOf(MillionsTaskClient.props(manager))
    
    val workers = (1 to 1024).map(n => system.actorOf(Worker.props(manager))).toList
    
    Thread.sleep(4000)
    client.isTerminated should be (true)
  }
  
  test("Multiple clients should work with manager and workers in concert") {
    val manager = system.actorOf(Manager.props, "manager") 
    val client1 = system.actorOf(MillionsTaskClient.props(manager), "client1") 
    val client2 = system.actorOf(MillionsTaskClient.props(manager), "client2")
    val client3 = system.actorOf(MillionsTaskClient.props(manager), "client3")
    
    Thread.sleep(200)
    
    val workers = (1 to 1024).map(n => system.actorOf(Worker.props(manager))).toList
    
    Thread.sleep(4000)
    
    client1.isTerminated should be (true)
    client2.isTerminated should be (true)
    client3.isTerminated should be (true)
  }
  
  test("Order of joining of client or worker should not matter") {
    val manager = system.actorOf(Manager.props, "manager2")
    val client1 = system.actorOf(MillionsTaskClient.props(manager), "client4") 
    val client2 = system.actorOf(MillionsTaskClient.props(manager), "client5")
    
    val workers1 = (1 to 1024).map(n => system.actorOf(Worker.props(manager))).toList
    Thread.sleep(500)
    val client3 = system.actorOf(MillionsTaskClient.props(manager), "client6")
    Thread.sleep(3000)
    client1.isTerminated should be (true)
    client2.isTerminated should be (true)
    client3.isTerminated should be (true)
  }
  
}