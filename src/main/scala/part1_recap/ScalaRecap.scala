package part1_recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ScalaRecap extends App {

  val aCondition = false
  var aVarCondition = true

  def ff(par: Int) = {
    if (par > 4) 42 else 100
  }

  // OO scala
  class Animal
  trait Mammal {
    def suck() = println("huhuhum")
  }

  object Mammal

  //Generics
  abstract class MyList[T]

  //method notations
  1 + 2 //infix notation
  1.+(2)

  //FP
  val inc: Int => Int = _ + 1
  inc(1)
  List(1,2,34).map(inc).foreach(println)

  // HOF map, flatmap, filter

  // for-comprehantions

  val res = List(1,2,3,4,5,6,7,8).map(_ + 1).map(_ * 2).filter(_ < 10)

  //Pattern matching
  val unk = 2
  val order = unk match {
    case 2 => println("yeiiii")
  }

  try {
    throw new RuntimeException
  } catch {
    case _: RuntimeException => println("yeah, this is pattern matched")
    case _ => println("i dont know")
  }

  /**
    * Scala advanced
    */

  import scala.concurrent.ExecutionContext.Implicits.global
  // multithreading fundamentals
  val future = Future[Int] {
    Thread.sleep(100)
    10
  }

  future.onComplete {
    case Success(value) => println("got")
    case Failure(exception) => println("failed")
  }

  Thread.sleep(300)


  val partialF: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case _ => 0
  }

  // type aliases
  type AkkaReceive = PartialFunction[Any, Unit]

  def receive: AkkaReceive = {
    case 1 => "hello"
    case _ => "confused"
  }

  // Implicits
  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()
  setTimeout(() => println("timeout"))

  case class Person(name: String){
    def greet = println(s"hi my name is $name")
  }

  //implicits conversions
  implicit def fromStringToPerson(s: String) = Person(s)
  "John".greet

  implicit class Dog(name: String){
    def bark = println("bark")
  }

  "lassie".bark

  //implicit organization
  // 1 local scope
  implicit val numberOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted //

  // 2 imported scope
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a,b) => a.name.compareTo(b.name) < 0)
  }

  List(Person("brad"),Person("troy"),Person("zelda"),Person("anna"),Person("nicolas")).sorted.foreach(println)


}
