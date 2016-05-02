package maze

import java.io._

//_width:Int, _height:Int, _shuf:Double, _rooms:Int, _roomSize:Int, _roomVar:Int, _removals:Int
object Main {
  def main(args:Array[String]):Unit = {
    val a = args.map(_.toInt)
    val width = a(0)
    val height = a(1)
    val m = new Maze(width, height, a(2)/100.0, a(3), a(4), a(5), a(6));
    
    val paths = m.generate();
    println(m.print(m.carve(paths, m.build())));
    val startEnd = m.getStartAndEnd(paths)
    println("Start: "+startEnd._1+", End: "+startEnd._2)
    println("Press enter to write maze to file(s)...")
    System.in.read()
    val wal = m.getWalls(paths)
    
    val ww = new PrintWriter(new File("walls.mdf"))
    ww.write(m.dataString(startEnd, wal, width, height))
    ww.close
    
    val mesh = m.getMesh(paths, wal)
    
    val pw = new PrintWriter(new File("floor_data.obj"))
    pw.write( Geom.objGeomString(mesh._1) + 
        "\n" + Geom.objTexString(mesh._1) + 
        "\n" + Geom.objNormString(mesh._1) + 
        "\n" + Geom.objFaceString(mesh._1))  
    pw.close
  
    val ppw = new PrintWriter(new File("wall_data.obj"))
    ppw.write(Geom.objGeomString(mesh._2) + 
        "\n" + Geom.objTexString(mesh._2) + 
        "\n" + Geom.objNormString(mesh._2) + 
        "\n" + Geom.objFaceString(mesh._2))
    ppw.close
    
    println("Done.")
  }
}