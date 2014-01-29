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
import akkaworker.actors.Manager
import akkaworker.actors.Protocol._
import akkaworker.actors.Worker
import akka.actor.TypedActor
import akka.actor.TypedProps
import akkaworker.actors.Client
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._

class WorkerSpec extends TestKit(ActorSystem("WorkerSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender 
                 with Tools {
  
  
  override def afterAll(): Unit = system.shutdown()
  
  test("Worker should receive tasks from manager") {
    val manager = system.actorOf(Manager.props) 
    val client = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new SomeClient("client")))
    val worker1 = TestProbe()
    val worker2 = TestProbe() 
    
    client.joinManager(manager)
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
    val client = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))    
    client.joinManager(manager)
    val fut = client.allTasksComplete
    
    val workers = (1 to 1024).map(n => system.actorOf(Worker.props)).toList
    workers.foreach(w => w ! StartWorker(manager))
    
    Await.result(fut, 4 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
  }
  
  test("Multiple clients should work with manager and workers in concert") {
    val manager = system.actorOf(Manager.props, "manager") 
    val client1 = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))   
    val client2 = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))   
    val client3 = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))   
    
    client1.joinManager(manager)
    client2.joinManager(manager)
    client3.joinManager(manager)
    val fut1 = client1.allTasksComplete
    val fut2 = client2.allTasksComplete
    val fut3 = client3.allTasksComplete
    
    Thread.sleep(200)
    
    val workers = (1 to 1024).map(n => system.actorOf(Worker.props)).toList
    workers.foreach(w => w ! StartWorker(manager))
    
    Await.result(fut1, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
    Await.result(fut2, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
    Await.result(fut3, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
  }
  
  test("Order of joining of client or worker should not matter") {
    val manager = system.actorOf(Manager.props, "manager2")
    val client1 = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))   
    val client2 = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))  
    client1.joinManager(manager)
    client2.joinManager(manager)
    
    val workers1 = (1 to 512).map(n => system.actorOf(Worker.props)).toList
    workers1.foreach(w => w ! StartWorker(manager))
    Thread.sleep(500)
    val client3 = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new MillionsTaskClient("client")))   
    client3.joinManager(manager)
    
    val fut1 = client1.allTasksComplete
    val fut2 = client2.allTasksComplete
    val fut3 = client3.allTasksComplete
    
    Await.result(fut1, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
    Await.result(fut2, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
    Await.result(fut3, 5 seconds).asInstanceOf[Traversable[Option[Any]]].size should be (5000)
  }
  
  test("Worker should respond task failure to client correctly") {
    val manager = system.actorOf(Manager.props, "manager3")
    val client = TypedActor(system).typedActorOf(TypedProps(classOf[Client[_]], new FailureTaskClient("Failure Client")))   
    client.joinManager(manager)
    val fut = client.allTasksComplete
    
    val workers = (1 to 128).map(n => system.actorOf(Worker.props)).toList
    workers.foreach(w => w ! StartWorker(manager))
    try {
      fut onFailure {case f => 1 should be (1)}
      Await.result(fut, 3 seconds)
    }
    catch {
      case e: Throwable => println(e)
    }
    
  }
  
}