package maze

class Maze(_width:Int, _height:Int, _shuf:Double, _rooms:Int, _roomSize:Int, _roomVar:Int, _removals:Int) {
  
  type Grid = Array[Array[Boolean]] //Mostly un-used... artifact of an old representation of the maze
  type Coord = (Int, Int)
  type Floors = List[Coord]
  type Walls = Floors //Same as floors, for the sake of clarity in the code
  type Regions = List[Floors]
  
  val Dirs = List(0,1,2,3)
  val ErrCoord = (-1, -1)
  
  val _rand = scala.util.Random
  
  //val _size = (_width, _height)
  
  //Generates a dungeon of rooms and corridors.
  def generate() : Floors = {
    //Get an odd-aligned point in the maze
    def randomPoint(w:Int, h:Int): Coord = {
      (_rand.nextInt((w-1)/2)*2 + 1, _rand.nextInt((h-1)/2)*2 + 1)
    }
    
    //Create corridors. A randomized variation of DFS.
    def makePaths(co:Coord, prev:Coord, floors:Floors) : Floors = {
      
      //Randomly shuffle a list.. used for randomizing the carving strategy
      def chanceShuffle(odds:Double, list:List[Int]):List[Int] = {
        val rand = scala.util.Random
        if (rand.nextDouble() < odds)
          return rand.shuffle(list)
        else return list
      }
      
      def getDirection():Int = {
        if      (prev == (co._1 - 1, co._2)) 2 //going down
        else if (prev == (co._1, co._2+1))   3  //going left
        else if (prev == (co._1 + 1, co._2)) 0  //going up
        else if (prev == (co._1, co._2-1))   1  //going right
        else scala.util.Random.nextInt(4)
      }
      val direc = getDirection() //since this might be random, get it once only

      //Our strategy is: If we are carving in a specific direction, tend towards that direction
      def carvingStrategy():List[Int] = {
        val d = direc
        val filt = Dirs.filter(_ != d)
        return d :: filt
      }
      
      //p: Point tested, n: Max number of neighboring floors allowed
      def validFloor(p:Coord, n:Int, fl:Floors):Boolean = (surroundingFloors(p, fl).size <= n && isValidCoord(p) && !fl.contains(p))
      
      if (validFloor(co,0,floors) && (validFloor(prev,1,floors) || prev == ErrCoord)) {
        val f0 = (if (prev != ErrCoord) List(prev, co) else List(co)) ++ floors //add the first and second points to the floors list (i.e. "visited")

        //Code for displaying maze while generating
//        System.out.print("\033\143")
//        println(print(carve(f0, grid)))
//        Thread.sleep(3L);
        
        //Randomize direction priority based on shuffle chance (_shuf)
        val dirs = chanceShuffle(_shuf, carvingStrategy())
        val casts = List((cast2(dirs(0), co), cast(dirs(0), co)), 
                         (cast2(dirs(1), co), cast(dirs(1), co)), 
                         (cast2(dirs(2), co), cast(dirs(2), co)), 
                         (cast2(dirs(3), co), cast(dirs(3), co)))
                      .filter(c => (validFloor(c._1, 0, f0) && validFloor(c._2, 1, f0)))         
        return casts.foldLeft(f0)((f, t) => makePaths(t._1, t._2, f))
      }
      else
        return floors
    }
    
    //Place generated rooms.
    def placeRooms(num:Int, floors:Floors):(Floors, List[List[Coord]]) = {
      
      def room(f:Floors):Floors = {
        val hv = _roomSize + (_rand.nextInt(_roomVar) * (if (_rand.nextBoolean()) -1 else 1))
        val wv = _roomSize + (_rand.nextInt(_roomVar) * (if (_rand.nextBoolean()) -1 else 1))
        val h = hv-(1-(hv%2))
        val w = wv-(1-(wv%2)) //always odd room size
        val row = _rand.nextInt((_height - 1 - h) / 2)*2 + 1 //always odd row/col
        val col = _rand.nextInt((_width - 1 - w)/2)*2 + 1
        
        val rm = (0 to h-1).toList.foldLeft(List[Coord]())((l, r) => {
          (0 to w-1).toList.foldLeft(l)((l, c) => {
            (r+row, c+col) :: l
          })
        }).filter((c) => isValidCoord(c))
        
        //points = walls around the room that should also not intersect with rooms (i.e. we don't want rooms touching)
        val points =  ((-1 to h+1).toList.foldLeft(List[Coord]())((l,v) => (row+v, col-1  )::l)
                    ++ (-1 to h+1).toList.foldLeft(List[Coord]())((l,v) => (row+v, col+w+1)::l)
                    ++ (-1 to w+1).toList.foldLeft(List[Coord]())((l,v) => (row-1,   col+v)::l)
                    ++ (-1 to w+1).toList.foldLeft(List[Coord]())((l,v) => (row+h+1, col+v)::l))

        if (points.filter(f.contains(_)).isEmpty && rm.filter(f.contains(_)).isEmpty) rm
        else List()
      }
      
      
      def recur(n:Int, f:Floors, regs:Regions):(Floors, Regions) = {
        if (n==0) (f, regs)
        else {
          val r = room(f);
          if (r.isEmpty) recur(n-1, f, regs)
          else recur(n-1, r ++ f, regs :+ r)
        }
      }
      
      return recur(num, floors, List())
    }
    
    //Start carving corridors in the maze, making sure not to overlap any other floors
    // Each contiguous set of floors is maintained as an individual Region
    def makeRegions(w:Walls, f:Floors, regs:Regions):(Floors,Regions) = {
      if (w.isEmpty)
        (f, regs) //base case
      else {
        val res = makePaths(w(0), ErrCoord, f) //carve paths
        //if no new paths were made, move to next wall  
        val path = res.filter(!f.contains(_))
        if (path.isEmpty) 
          makeRegions(w.drop(1), f, regs) //path.isEmpty == unable to draw any floors at this wall.
        else 
          makeRegions(w.filter(!path.contains(_)).filter(surroundingFloors(_, res).size < 1), res, regs :+ path)
      }
    }    
    
    //Connect them
    def connectRegions(f:Floors, regs:Regions): Floors = {
      
      //Connects two regions
      def tryConnect(r:Floors): (Floors, Regions) = {
        //Poke at the edges and see what points are in a neighboring region
        //Returns (floor not in this region, wall that connects the two)
        def castThru(point:Coord, d:List[Int]):(Coord, Coord) = {
          if (d.isEmpty) return (point, ErrCoord)
          else {
            val c = cast(d(0), cast(d(0), point))
            if (f.contains(c) && !r.contains(c))
              return (c, cast(d(0), point))
            else
              return castThru(point, d.drop(1))
          }
        }
        //Find the first neighbor using all eligible points        
        def findNeighbor(list:List[Coord]):(Coord, Coord) = {
          if (list.isEmpty) return (ErrCoord, ErrCoord) //this would be pretty bad but could happen i guess
          else {
            val p = castThru(list(0), _rand.shuffle(Dirs));
            if (p._2 == ErrCoord) findNeighbor(list.drop(1))
            else {
              return p
            }
            
          }
        }
        //Find the region that contains the point
        def findRegion(p:Coord, r:Regions):Floors = {
          if (r.isEmpty) return List() //this would make... ZERO SENSE!
          else {
            if (r(0).contains(p)) return r(0)
            else return findRegion(p, r.drop(1))
          }
        }
        
        //Eligible points: points that border a wall
        val elig = _rand.shuffle(r.filter(surroundingFloors(_, f).size < 4)) //points eligible to connect
        val neigh = findNeighbor(elig)
        val reg = findRegion(neigh._1, regs)
        if (neigh._1 != ErrCoord)
          return ((reg.filter(!r.contains(_)) ++ r) :+ neigh._2, regs.filter((g) => (g != reg && g != r))) //add the hole that joins the two regions
        else 
          (List(), List())
      }
      val conn = tryConnect(regs(0))
      if (conn._2.isEmpty) { //base case
        return conn._1 //may return as empty, which would mean something went wrong
      } else { //keep connecting
        connectRegions(f, conn._2 :+ conn._1)
      }
    }
    
    def trimEnds(f:Floors, n:Int):Floors = {
      val elig = f.filter(surroundingFloors(_, f).size < 2)
      if (elig.size == 0) return f //possible base case
      
      def doTrim(ends:Floors, fl:Floors, num:Int): (Floors, Int) = {
        if (ends.isEmpty || num == 0) return (fl, num)
        else {
//          System.out.print("\033\143")
//          println(print(carve(fl, grid)))
//          Thread.sleep(3L);
          
          return doTrim(ends.drop(1), fl.filter(_ != ends(0)), num-1)
        }
      }
      val trim = doTrim(elig, f, n)
      
      if (trim._2 == 0) return trim._1 //another possible base case
      else return trimEnds(trim._1, trim._2)
    }
    println("Width:"+_width + ", Height:"+_height+", Tiles:"+_width*_height)
    
    val rooms = placeRooms(_rooms, List())
    val floors = rooms._1
    val regions = rooms._2
    println("Floors:"+floors.size)
    //after placing rooms, iterate through all remaining wall pieces and start making corridors
    val walls = (0 to (_height/2)-1).toList.foldLeft(List[Coord]())((l, r) => {
        (0 to (_width/2)-1).toList.foldLeft(l)((l, c) => {
           l :+ (r*2 + 1, c*2 + 1)
        })
      })
      .filter(!floors.contains(_))
      .filter(surroundingFloors(_, floors).size < 1)
    
    val maze = makeRegions(walls, floors, regions)
    println("Maze floors:"+maze._1.size)
    val conn = connectRegions(maze._1, maze._2.sortBy(_.size))
    println("Connected floors:"+conn.size)
    return trimEnds(conn, _removals)
    
  }
  
  //Get the geometry of the floors and walls, organized respectively as a tuple
  def getMesh(fl:Floors, wa:Walls):(Geom.Obj, Geom.Obj) = {     
    val floors = fl.foldLeft((Geom.emptyObj, 0))((obj, co) => {
      val o = Geom.floorObj(co._1, 0, co._2, _width, _height, obj._2)
      ((obj._1._1 ++ o._1, obj._1._2 ++ o._2, obj._1._3 ++ o._3, obj._1._4 ++ o._4), obj._2+1)
    })
    
    val walls = wa.foldLeft((Geom.emptyObj, 0))((obj, co) => {
      val o = Geom.cubeObj(co._1, 0, co._2, _width, _height, obj._2)
      ((obj._1._1 ++ o._1, obj._1._2 ++ o._2, obj._1._3 ++ o._3, obj._1._4 ++ o._4), obj._2+1)
    })
    
    (floors._1, walls._1)
  }
  
  //get a set of walls based on a list of floors
  def getWalls(f:Floors):Walls = {
    (0 to (_height)-1).toList.foldLeft(List[Coord]())((l, r) => {
        (0 to (_width)-1).toList.foldLeft(l)((l, c) => {
           l :+ (r, c)
        })
      })
      .filter(!f.contains(_))
  }
   
  //Helpers below here
  //val grid = build(_width, _height, Array())
  def carve(floors:Floors, grid:Grid):Grid = {
    if (floors.isEmpty) return grid
    else return carve(floors.drop(1), poke(floors(0), grid))
  }
  //Tests if the coordinate is within the boundaries of the maze
  def isValidCoord(co:Coord):Boolean = (!(co._1 < 1 || co._1 > _height - 2 || co._2 < 1 || co._2 > _width - 2))
  
  //Create a Coord based off an Int. 0 = Up, 1 = Right, 2 = Down, 3 = Left
  def dir(d:Int):Coord = (if (d == 1 || d == 3) 0 else if (d == 0) -1 else 1, if (d == 0 || d == 2) 0 else if (d == 1) 1 else -1)
  
  //Get the neighboring coordinate given a direction
  def cast(d:Int, co:Coord): Coord = (co._1 + dir(d)._1, co._2 + dir(d)._2)
  def cast2(d:Int, co:Coord): Coord = cast(d, cast(d, co))
  
  //Pokes a hole in the Grid (used for placing floors for the 2D array representation)
  def poke(co:Coord, g:Grid): Grid = {
    if (co._1 > 0 && co._2 > 0 && co._1 < g.length && co._2 < g(0).length)
      g.updated(co._1, g(co._1).updated(co._2, false))
    else g
  }
  
  //Create a new Grid filled with walls
  def build() : Grid = Array.fill(_height)(Array.fill(_width)(true))
  
  //Prints the spaces within the Grid row by row
  def print(g:Grid) = g.foldLeft("")((s, r) => s + "\n" + r.foldLeft("")((st, sp) => st + (if (sp) "\u2593" else "\u2591")))
  
  //Get a string representation of all walls, represented as coordinates
  // The first two numbers in the string are the x/y width/height of the maze.
  def dataString(wa:Walls, w:Int, h:Int):String = {
    wa.foldLeft(w+" "+h)((str, sp) => {
      str + "\n" + sp._1 + " " + sp._2 
    })
  }
  
  //Gets a list of floors that surround a space
  def surroundingFloors(c:Coord, f:Floors):Floors = {
    val dirs = List(0,1,2,3);
    List(cast(dirs(0), c), cast(dirs(1), c), cast(dirs(2), c), cast(dirs(3), c))
      .filter(d => (isValidCoord(d) && f.contains(d)))
  }
}