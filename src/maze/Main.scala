package maze

import java.io._

//_width:Int, _height:Int, _shuf:Double, _rooms:Int, _roomSize:Int, _roomVar:Int, _removals:Int
object Main {
  def main(args:Array[String]):Unit = {
    val a = args.map(_.toInt)
    val width = a(0)
    val height = a(1)
    val m = new Maze(width, height, a(2)/100.0, a(3), a(4), a(5), a(6));
    
    
    
//    val testF = List((0,0), (1,1), (2,0), (0,2), (2,2))
//    val testW = List((1,0), (0,1), (2,1), (1,2))
//    val testMesh = m.getMesh(testF, testW) 
//    val tpw = new PrintWriter(new File("testfloor_data.obj"))
//    tpw.write( Geom.objGeomString(testMesh._1) + 
//        "\n" + Geom.objTexString(testMesh._1) + 
//        "\n" + Geom.objNormString(testMesh._1) + 
//        "\n" + Geom.objFaceString(testMesh._1))  
//    tpw.close
//  
//    val tppw = new PrintWriter(new File("testwall_data.obj"))
//    tppw.write(Geom.objGeomString(testMesh._2) + 
//        "\n" + Geom.objTexString(testMesh._2) + 
//        "\n" + Geom.objNormString(testMesh._2) + 
//        "\n" + Geom.objFaceString(testMesh._2))
//    tppw.close
    
    
    
    
    val paths = m.generate();
    println(m.print(m.carve(paths, m.build())));
    println("Press enter to write maze to file(s)...")
    System.in.read()
    val wal = m.getWalls(paths)
    
    val ww = new PrintWriter(new File("walls.mdf"))
    ww.write(m.dataString(wal, width, height))
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