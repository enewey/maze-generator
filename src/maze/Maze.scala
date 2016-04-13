package maze

class Maze {
  
  type Grid = Array[Array[Space]]
  type Coord = (Int, Int)
  type Floors = List[Coord]
  type Walls = List[Coord]
  type Regions = List[Floors]
  
  val Dirs = List(0,1,2,3)
  val ErrCoord = (-1, -1)
  
  val _rand = scala.util.Random
  
  val _shuf = 0.3
  
  val _rooms = 100 //number of times we attempt to place a room
  val _roomSize = 5 //average size of a room (n x n)
  val _roomVar = 1 //the variance in room sizes
  
  val _removals = 50 //number of dead-ends to remove from maze
  
  def generate(grid:Grid) : Grid = {
    
    //The recursive randomized DFS call
    def makePaths(co:Coord, floors:Floors) : Floors = {
      
      
      //Randomly shuffle a list.. used for randomizing the carving strategy
      def chanceShuffle(odds:Double, list:List[Int]):List[Int] = {
        val rand = scala.util.Random
        if (rand.nextDouble() < odds)
          return rand.shuffle(list)
        else return list
      }
      
      def getDirection():Int = {
        if      (floors.contains((co._1 - 1, co._2))) 2 //going down
        else if (floors.contains((co._1, co._2+1)))   3  //going left
        else if (floors.contains((co._1 + 1, co._2))) 0  //going up
        else if (floors.contains((co._1, co._2-1)))   1  //going right
        else scala.util.Random.nextInt(4)
      }
      val direc = getDirection() //since this might be random, get it once only
      
      //Our strategy is: If we are carving in a specific direction, tend towards that direction
      def carvingStrategy():List[Int] = {
        val d = direc
        val filt = Dirs.filter(_ != d)
        return d :: filt
      }
      
      def validFloor(p:Coord, n:Int):Boolean = (surroundingFloors(p, floors, grid).size < n && isValidCoord(p, grid) && !floors.contains(p))
      
      //Draw two floors at a time... this is the second point
      val sp = cast(direc, co)
      
      if (validFloor(co,2) && validFloor(sp,1)/* && validFloor(tp,1)*/) {
        val f0 = /*tp :: */sp :: co :: floors //add the first and second points to the floors list (i.e. "visited")
        
        //Code for displaying maze while generating
        println(print(carve(f0, grid)))
        Thread.sleep(3L);
        
        //Randomize next direction
        val dirs = chanceShuffle(_shuf, carvingStrategy())
        val casts = List(cast(dirs(0), sp), cast(dirs(1), sp), cast(dirs(2), sp), cast(dirs(3), sp))
                      .filter(c => (surroundingFloors(c, f0, grid).size < 2 && isValidCoord(c, grid) && !f0.contains(c)))         
        return casts.foldLeft(f0)((f, t) => makePaths(t, f))
      }
      else
        return floors
    }
    
    //Get an even-numbered point in the maze
    def randomPoint(w:Int, h:Int): Coord = {
      (_rand.nextInt((w-1)/2)*2 + 1, _rand.nextInt((h-1)/2)*2 + 1)
    }
    
    def placeRooms(num:Int, floors:Floors):(Floors, List[List[Coord]]) = {
      
      def room(f:Floors):Floors = {
        val hv = _roomSize + (_rand.nextInt(_roomVar) * (if (_rand.nextBoolean()) -1 else 1))
        val wv = _roomSize + (_rand.nextInt(_roomVar) * (if (_rand.nextBoolean()) -1 else 1))
        val h = hv-(1-(hv%2))
        val w = wv-(1-(wv%2)) //always odd room size
        val row = _rand.nextInt((grid.length - 1 - h) / 2)*2 + 1 //always odd row/col
        val col = _rand.nextInt((grid(0).length - 1 - w)/2)*2 + 1
        
        val rm = (0 to h-1).toList.foldLeft(List[Coord]())((l, r) => {
          (0 to w-1).toList.foldLeft(l)((l, c) => {
            (r+row, c+col) :: l
          })
        }).filter((c) => isValidCoord(c, grid))
        
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
    
    //find inter-connected regions in the maze
    def makeRegions(w:Walls, f:Floors, regs:Regions):(Floors,Regions) = {
      //println("make regions:\n"+w)
      if (w.isEmpty) 
        (f, regs) //base case
      else {
        val res = makePaths(w(0), f) //carve paths
        //if no new paths were made, move to next wall  
        val path = res.filter(!f.contains(_))
        if (path.isEmpty) 
          makeRegions(w.drop(1), f, regs) //path.isEmpty == unable to draw any floors at this wall.
        else 
          makeRegions(w.filter(!path.contains(_)).filter(surroundingFloors(_, res, grid).size < 1), res, regs :+ path)
      }
    }
        
    def carve(floors:Floors, grid:Grid):Grid = {
      if (floors.isEmpty) return grid
      else return carve(floors.drop(1), poke(floors(0), grid))
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
              //println("Found neighbor: "+p)
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
        val elig = _rand.shuffle(r.filter(surroundingFloors(_, f, grid).size < 4)) //points eligible to connect
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
    
    val rooms = placeRooms(_rooms, List())
    val floors = rooms._1
    val regions = rooms._2
    //after placing rooms, iterate through all remaining wall pieces and start making corridors
    val walls = (0 to (grid.length/2)-1).toList.foldLeft(List[Coord]())((l, r) => {
        (0 to (grid(0).length/2)-1).toList.foldLeft(l)((l, c) => {
           l :+ (r*2 + 1, c*2 + 1)
        })
      })
      .filter(!floors.contains(_))
      .filter(surroundingFloors(_, floors, grid).size < 1)
    
    val maze = makeRegions(walls, floors, regions)
    
    //Return the result as a 2d array
    val conn = connectRegions(maze._1, maze._2.sortBy(_.size))
    return carve(conn, grid)
  }  
   
  //Helpers below here
    
  //Tests if the coordinate is within the boundaries of the maze
  def isValidCoord(co:Coord, g:Grid):Boolean = (!(co._1 < 1 || co._1 > g.length - 2 || co._2 < 1 || co._2 > g(0).length - 2))
  
  //Create a Coord based off an Int. 0 = Up, 1 = Right, 2 = Down, 3 = Left
  def dir(d:Int):Coord = (if (d == 1 || d == 3) 0 else if (d == 0) -1 else 1, if (d == 0 || d == 2) 0 else if (d == 1) 1 else -1)
  
  //Get the neighboring coordinate given a direction
  def cast(d:Int, co:Coord): Coord = (co._1 + dir(d)._1, co._2 + dir(d)._2)
  
  //Pokes a hole in the Grid (used for placing floors for the 2D array representation)
  def poke(co:Coord, g:Grid): Grid = {
    if (co._1 > 0 && co._2 > 0 && co._1 < g.length && co._2 < g(0).length)
      g.updated(co._1, g(co._1).updated(co._2, new Space(false)))
    else g
  }
  
  //Create a new Grid filled with walls
  def build(w:Int, h:Int, g:Grid) : Grid = Array.fill(h)(Array.fill(w)(new Space(true)))
  
  //Prints the spaces within the Grid row by row
  def print(g:Grid) = g.foldLeft("")((s, r) => s + "\n" + r.mkString(""))
  
  //Gets a list of floors that surround a space
  def surroundingFloors(c:Coord, f:Floors, grid:Grid):Floors = {
    val dirs = List(0,1,2,3);
    List(cast(dirs(0), c), cast(dirs(1), c), cast(dirs(2), c), cast(dirs(3), c))
      .filter(d => (isValidCoord(d, grid) && f.contains(d)))
  }
}