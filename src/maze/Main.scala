package maze

object Main {
  def main(args:Array[String]):Unit = {
    val m = new Maze();
    val grid = m.generate(m.build(100, 50, Array()));
    println(m.print(grid));
  }
}