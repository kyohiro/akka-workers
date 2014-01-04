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
    val client = system.actorOf(SomeClient.props)
    val worker1 = TestProbe()
    val worker2 = TestProbe() 
    
    client ! StartClient(manager)
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
    worker1.expectMsg(TaskAvailable)
    worker2.reply(TaskFinished(2L, Some(5L)))
    worker2.expectMsg(TaskAvailable)
  }

  test("Workers should handles amounts of small tasks quickly") {
    val manager = system.actorOf(Manager.props) 
    val client = system.actorOf(MillionsTaskClient.props)
    client ! StartClient(manager)
    
    val workers = (1 to 1024).map(n => system.actorOf(Worker.props)).toList
    workers.foreach(w => w ! StartWorker(manager))
    
    Thread.sleep(4000)
    client.isTerminated should be (true)
  }
  
  test("Multiple clients should work with manager and workers in concert") {
    val manager = system.actorOf(Manager.props, "manager") 
    val client1 = system.actorOf(MillionsTaskClient.props, "client1") 
    val client2 = system.actorOf(MillionsTaskClient.props, "client2")
    val client3 = system.actorOf(MillionsTaskClient.props, "client3")
    
    client1 ! StartClient(manager)
    client2 ! StartClient(manager)
    client3 ! StartClient(manager)
    Thread.sleep(200)
    
    val workers = (1 to 1024).map(n => system.actorOf(Worker.props)).toList
    workers.foreach(w => w ! StartWorker(manager))
    
    Thread.sleep(4000)
    
    client1.isTerminated should be (true)
    client2.isTerminated should be (true)
    client3.isTerminated should be (true)
  }
  
  test("Order of joining of client or worker should not matter") {
    val manager = system.actorOf(Manager.props, "manager2")
    val client1 = system.actorOf(MillionsTaskClient.props, "client4") 
    val client2 = system.actorOf(MillionsTaskClient.props, "client5")
    client1 ! StartClient(manager)
    client2 ! StartClient(manager)
    
    val workers1 = (1 to 512).map(n => system.actorOf(Worker.props)).toList
    workers1.foreach(w => w ! StartWorker(manager))
    Thread.sleep(500)
    val client3 = system.actorOf(MillionsTaskClient.props, "client6")
    client3 ! StartClient(manager)
    Thread.sleep(4000)
    client1.isTerminated should be (true)
    client2.isTerminated should be (true)
    client3.isTerminated should be (true)
  }
  
  test("Worker should respond task failure to client correctly") {
    val manager = system.actorOf(Manager.props, "manager3")
    val client = system.actorOf(FailureTaskClient.props(manager), "client7") 
    client ! StartClient(manager)
    
    val workers = (1 to 128).map(n => system.actorOf(Worker.props)).toList
    workers.foreach(w => w ! StartWorker(manager))
    
    Thread.sleep(2000)
    client.isTerminated should be (true)
    
  }
  
}