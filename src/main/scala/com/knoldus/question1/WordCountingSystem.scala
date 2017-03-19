package com.knoldus.question1

import akka.actor.{Actor, Props, ActorSystem}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask

object WordCountingSystem extends App{
  val system = ActorSystem("WordCountingSystem")

  //Q1
  val propsAdder = Props[Adder]
  val refAdder = system.actorOf(propsAdder)
  implicit val timeout = Timeout(10 seconds)
  val count = refAdder ? ("This is a simple   sentence\nAnd i like as well as love it")
  count.map(x=>println("Total count of words is: "+x))
}

class WordCountingSystem extends Actor{
  var counter = 0;
  override def receive: Receive = {
    case string:String => {
      val length = string.split("[ ,!.]+").size
      sender() ! length
    }
    case msg => println("msg: "+msg+ ", "+msg.getClass)
  }
}

class Adder extends Actor{
  var count = 0;
  var total = 0;

  override def receive: Receive = {
    case text: String => {
      val sentences = text.split("\n")
      total = sentences.length
      implicit val timeout = Timeout(10 seconds)
      val listOfFuture = (for(i <- sentences.indices) yield context.actorOf(RoundRobinPool(5).props(Props[WordCountingSystem])) ? sentences(i)).toList
      val futureOfList = Future.sequence(listOfFuture)
      Await.result(futureOfList.map{_ map{
        case x:Int => count+=x
      }}, 10 second)
      sender() ! count
    }
    case _ => sender() ! "I don't understand?"
  }
}