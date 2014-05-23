
object Inty extends App {
  implicit class Inty(theInt: Int) {
    def double: Int = theInt * 2
    def halve: Int = theInt / 2
    def cube: Int = theInt * theInt * theInt
  }

  val two = 2
  
  println(two.double)
  println(two.halve)
  println(two.cube)  
}
