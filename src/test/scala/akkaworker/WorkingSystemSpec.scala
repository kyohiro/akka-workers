package akkaworker

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akkaworker.system.SingleManagerSystem

class WorkingSystemSpec extends FunSuite
                        with ShouldMatchers
                        with Tools {
  test("Working system should start and welcome clients correctly") {
    val system = SingleManagerSystem("TestSystem", 256)
    val client1 = MillionsTaskClient.props
    system.clientJoin(client1)
    
    Thread.sleep(2500)
    system.allClientsClosed should be (true)
    
    val client2 = MillionsTaskClient.props
    system.clientJoin(client2)
    system.allClientsClosed should be (false)
    
    Thread.sleep(2500)
    system.allClientsClosed should be (true)
  }
}