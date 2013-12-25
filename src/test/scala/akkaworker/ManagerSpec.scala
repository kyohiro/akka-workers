package akkaworker

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import com.kyohiro.akkaworker.workers.Manager

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
    client.send(manager, Manager.JoinClient)
    worker.send(manager, Manager.JoinWorker)
    
    client.expectMsg(Manager.Welcome)
    worker.expectMsg(Manager.Welcome)
  }
}