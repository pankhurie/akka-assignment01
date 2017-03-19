package com.knoldus.question2

import akka.actor.{Actor, Props, ActorSystem}
import akka.util.Timeout
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask

object TicketBooking extends App{

  val system = ActorSystem("TicketBookingSystem")
  val propsPerson = Props[Person]
  val propsReception = Props[Administrator]
  val refReception = system.actorOf(propsReception, "Administrator")
  val propsQueue = Props[TicketQueue]
  val refQueue1 = system.actorOf(propsQueue, "Queue1")
  val refQueue2 = system.actorOf(propsQueue, "Queue2")

  implicit val timeout = Timeout(10 seconds)

  //Persons 1,2,3,4 are respectively trying to book 1,2,3,4 tickets in queue 1
  for{
    i<-1 to 4
    person = system.actorOf(propsPerson, s"Person$i")
  } yield refQueue1.tell(i, person)
  //Persons 4,5,6 are again respectively trying to book 1,2,3 tickets in queue 2
  for{
    i<-1 to 3
    person = system.actorOf(propsPerson, s"Person${i+4}")
  } yield refQueue2.tell(i, person)

}

class TicketQueue extends Actor{
  override def receive: Receive ={
    //Referencing an actor by path
    case num:Int=> context.actorSelection("../Administrator").forward(num)
  }
}

class Person extends Actor{
  override def receive: Receive ={
    case msg => println(msg)
  }
}

class Administrator extends Actor{
  var remainingSeats=6

  override def receive: Receive ={
    case num:Int =>
      if(remainingSeats==0){
          println(s"${sender().path.name} is trying to book $num ticket(s) ...")
          sender() ! s"**Reception: Sorry ${sender().path.name}, Housefull!"
      }
      else{
        println(s"${sender().path.name} is trying to book $num ticket(s) ...")
        if(num<=remainingSeats){
          remainingSeats-=num
          sender() ! s"**Reception: Congrats ${sender().path.name}, $num ticket(s) booked successfully for you."
        }
        else{
          sender() ! s"**Reception: Sorry ${sender().path.name}, $num ticket(s) are not available. Only $remainingSeats left."
        }
      }
  }
}

