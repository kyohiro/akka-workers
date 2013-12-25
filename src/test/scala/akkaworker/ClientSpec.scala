package akkaworker

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.ImplicitSender

class ClientSpec extends TestKit(ActorSystem("ClientSpec")) 
                 with FunSuite
                 with BeforeAndAfterAll
                 with ShouldMatchers
                 with ImplicitSender {

  override def afterAll(): Unit = system.shutdown()
  
  
}