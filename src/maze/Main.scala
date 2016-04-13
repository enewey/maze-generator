package maze

object Main {
  def main(args:Array[String]):Unit = {
    val m = new Maze();
    val grid = m.generate(m.build(args(0).toInt, args(1).toInt, Array()));
    println(m.print(grid));
  }
}