package maze

//import Jama._

object Geom {
  
  type Vec2 = (Float, Float)
  type Vec3 = (Float, Float, Float)
  type Face = (Triplet, Triplet, Triplet)
  type Triplet = (Int, Int, Int)
  type Obj = (List[Vec3], List[Vec2], List[Vec3], List[Face]) //geometry, uvs, normals, faces
  
  def emptyObj = (List[Vec3](), List[Vec2](), List[Vec3](), List[Face]())
  
  //Uses the passed-in coordinates as the bottom-left-nearside corner
  // Coords are from -1 to 1
  //Return value is (geometry, normals, texture coords)
  def cubeObj(xin:Int, yin:Int, zin:Int, wi:Int, he:Int, face:Int): Obj = {
    val x = (xin*2) - (wi)
    val y = yin*2 
    val z = zin*2 - (he)
    val v0 = ((-1.0f+x),-1.0f+y, (1.0f+z))
    val v1 = ((1.0f+x), -1.0f+y, (1.0f+z))
    val v2 = ((1.0f+x), 1.0f+y,  (1.0f+z))
    val v3 = ((-1.0f+x),1.0f+y,  (1.0f+z))
    val v4 = ((-1.0f+x),-1.0f+y, (-1.0f+z))
    val v5 = ((1.0f+x), -1.0f+y, (-1.0f+z))
    val v6 = ((1.0f+x), 1.0f+y,  (-1.0f+z))
    val v7 = ((-1.0f+x),1.0f+y,  (-1.0f+z))
        
    val t0 = (0.0f, 0.0f)
    val t1 = (1.0f, 0.0f)
    val t2 = (1.0f, 1.0f)
    val t3 = (0.0f, 1.0f)
    
    val n0 = (0.0f+x,  0.0f+y,  1.0f+z)
    val n1 = (0.0f+x,  1.0f+y,  0.0f+z)
    val n2 = (0.0f+x,  0.0f+y,  -1.0f+z)
    val n3 = (-1.0f+x, 0.0f+y,  0.0f+z)
    val n4 = (0.0f+x,  -1.0f+y, 0.0f+z)
    val n5 = (1.0f+x,  0.0f+y,  0.0f+z)
    
    val geometry = List(v0, v1, v2, v3, v4, v5, v6, v7)
    val uvs = List(t0, t1, t2, t3)
    val normals = List(n0, n1, n2, n3, n4, n5)
    
    //vertex/uv/normal
    val faces = List(((1,1,1), (2,2,1), (3,3,1)), //front
                     ((3,3,1), (4,4,1), (1,1,1)),
                     
                     ((4,1,2), (3,2,2), (7,3,2)), //top
                     ((7,3,2), (8,4,2), (4,1,2)),
                     
                     ((8,1,3), (7,2,3), (6,3,3)), //back
                     ((6,3,3), (5,4,3), (8,1,3)),
                     
                     ((5,1,4), (1,2,4), (4,3,4)), //left
                     ((4,3,4), (8,4,4), (5,1,4)),
                     
                     ((6,2,5), (2,3,5), (1,4,5)), //bottom
                     ((1,4,5), (5,1,5), (6,2,5)),
                     
                     ((2,1,6), (6,2,6), (7,3,6)), //right
                     ((7,3,6), (3,4,6), (2,1,6)))
                     .map(f => {
                       ((f._1._1+(face*8),f._1._2+(face*4),f._1._3+(face*6)),
                        (f._2._1+(face*8),f._2._2+(face*4),f._2._3+(face*6)),
                        (f._3._1+(face*8),f._3._2+(face*4),f._3._3+(face*6)))
                     })
                        
    (geometry, uvs, normals, faces)
  }
  
  def floorObj(xin:Int, yin:Int, zin:Int, wi:Int, he:Int, face:Int): Obj = {
    val x = xin*2 - (wi)
    val y = yin*2
    val z = zin*2 - (he)
    val v0 = ((-1.0f+x), -1.0f+y, (1.0f+z))
    val v1 = ((1.0f+x), -1.0f+y, (1.0f+z))
    val v2 = ((1.0f+x), -1.0f+y, (-1.0f+z))
    val v3 = ((-1.0f+x), -1.0f+y, (-1.0f+z))
    
    val t0 = (0.0f, 0.0f)
    val t1 = (1.0f, 0.0f)
    val t2 = (1.0f, 1.0f)
    val t3 = (0.0f, 1.0f)
    
    val n0 = (0.0f+x, 1.0f+y, 0.0f+z)
    
    val geometry = List(v0, v1, v2, v3)
    val uvs = List(t0, t1, t2, t3)
    val normals = List(n0)
    
    
    //vertex/uv/normal
    val faces = List(((1,1,1), (2,2,1), (3,3,1)),
                     ((3,3,1), (4,4,1), (1,1,1))).map(f => {
                       ((f._1._1+(face*4),f._1._2+(face*4),f._1._3+(face)),
                        (f._2._1+(face*4),f._2._2+(face*4),f._2._3+(face)),
                        (f._3._1+(face*4),f._3._2+(face*4),f._3._3+(face)))
                     })
    
    (geometry, uvs, normals, faces)
  }
  
  def objGeomString(obj:Obj):String = {
    obj._1.foldLeft("")((str, vec) => {
      str + "v " + vec._1 +" "+ vec._2 +" "+ vec._3 +"\n"
    })
  }
  def objTexString(obj:Obj):String = {
    obj._2.foldLeft("")((str, vec) => {
      str + "vt " + vec._1 +" "+ vec._2 +"\n"
    })
  }
  def objNormString(obj:Obj):String = {
    obj._3.foldLeft("")((str, vec) => {
      str + "vn " + vec._1 +" "+ vec._2 +" "+ vec._3 +"\n"
    })
  }
  def objFaceString(obj:Obj):String = {
    obj._4.foldLeft("")((str, f) => {
      str + "f "+ f._1._1 +"/"+ f._1._2 +"/"+ f._1._3 +" " + 
                  f._2._1 +"/"+ f._2._2 +"/"+ f._2._3 +" " +
                  f._3._1 +"/"+ f._3._2 +"/"+ f._3._3 +"\n"
    })
  }
  
}