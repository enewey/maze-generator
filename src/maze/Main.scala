package maze

object Main {
  def main(args:Array[String]):Unit = {
    val m = new Maze();
    val grid = m.generate(m.build(50, 30, Array()));
    println(m.print(grid));
  }
}