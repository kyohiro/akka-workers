package akkaworker

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akkaworker.system.SingleManagerSystem

class WorkingSystemSpec extends FunSuite
                        with ShouldMatchers
                        with Tools {
  test("Working system should start and welcome clients correctly") {
    val system = SingleManagerSystem("TestSystem", 256)
    val client1 = new MillionsTaskClient("Million client 1")
    system.clientJoin(client1)
    
    Thread.sleep(2500)
    system.allCompleted should be (true)
    
    val client2 = new FailureTaskClient("Failure client 2")
    system.clientJoin(client2)
    system.allCompleted should be (false)
    
    Thread.sleep(2500)
    system.allCompleted should be (true)
  }
}