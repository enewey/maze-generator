package maze

class Maze {
  
  type Grid = Array[Array[Space]]
  type Coord = (Int, Int)
  
  val _rand = scala.util.Random
  
  val _shuf = 0.5
  val _rooms = 200
  val _roomSize = 6
  val _roomVar = 3
  
  def generate(grid:Grid) : Grid = {
    
    //The recursive randomized DFS call
    def dfs(co:Coord, floors:List[Coord]) : List[Coord] = {
            
      //Randomly shuffle a list
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
        else {
          //this will hit on the very first pass through of dfs
          scala.util.Random.nextInt(4)
        }
      }
      
      //Our strategy is: If we are carving in a specific direction, stick with that direction
      def carvingStrategy():List[Int] = {
        val d = getDirection()
        val filt = List(0,1,2,3).filter(_ != d)
        return d :: filt
      }
      
      //We draw two floors at a time here
      val sp = cast(getDirection(), co)
      
      if (surroundingFloors(co, floors, grid).size > 1 || !isValidCoord(co, grid) || floors.contains(co)
          || surroundingFloors(sp, floors, grid).size > 0 || !isValidCoord(sp, grid) || floors.contains(sp))
        return floors
      else {
        val f0 = sp :: co :: floors //add the coord to the floors list (i.e. "visited")
        println(print(carve(f0, grid)))
        Thread.sleep(4L);
        
        val dirs = chanceShuffle(_shuf, carvingStrategy())
        val casts = List(cast(dirs(0), sp), cast(dirs(1), sp), cast(dirs(2), sp), cast(dirs(3), sp))
                      .filter(c => (surroundingFloors(c, f0, grid).size < 2 && isValidCoord(c, grid) && !f0.contains(c)))         
        return casts.foldLeft(f0)((f, t) => dfs(t, f))
      }
    }
    
    def randomStart(w:Int, h:Int): (Coord, Coord) = {
      if (_rand.nextBoolean()) {
        val b = _rand.nextBoolean()
        val y = _rand.nextInt(h-(h%2)+1)-1
        ((if (b) 0 else w-1, y), (if (b) 1 else w-2, y))
      }
      else {
        val b = _rand.nextBoolean()
        val x = _rand.nextInt((w - (w%2))+1)-1
        ((x, if(b) 0 else h-1), (x, if(b) 1 else h-2))
      }    
    }
    
    def placeRooms(num:Int, floors:List[Coord]):List[Coord] = {
      
      def room(f:List[Coord]):List[Coord] = {
        val h = _roomSize + (_rand.nextInt(_roomVar) * (if (_rand.nextBoolean()) -1 else 1))
        val w = _roomSize + (_rand.nextInt(_roomVar) * (if (_rand.nextBoolean()) -1 else 1))
        val row = _rand.nextInt(grid.length - 2 - h) + 1
        val col = _rand.nextInt(grid(0).length - 2 - w) + 1
        
        val rm = (0 to h).toList.foldLeft(List[Coord]())((l, r) => {
          (0 to w).toList.foldLeft(l)((l, c) => {
            (r+row, c+col) :: l
          })
        })
        
        val points =  ((-1 to h+1).toList.foldLeft(List[Coord]())((l,v) => (row+v, col-1  )::l)
                    ++ (-1 to h+1).toList.foldLeft(List[Coord]())((l,v) => (row+v, col+w+1)::l)
                    ++ (-1 to w+1).toList.foldLeft(List[Coord]())((l,v) => (row-1,   col+v)::l)
                    ++ (-1 to w+1).toList.foldLeft(List[Coord]())((l,v) => (row+h+1, col+v)::l))

        if (points.filter(f.contains(_)).isEmpty && rm.filter(f.contains(_)).isEmpty) rm
        else List()
      }
      
      def recur(n:Int, f:List[Coord]):List[Coord] = {
        if (n==0) f
        else recur(n-1, room(f) ++ f)
      }
      
      return recur(num, floors)
    }
    
    def carve(floors:List[Coord], grid:Grid):Grid = {
      if (floors.isEmpty) return grid
      else return carve(floors.drop(1), poke(floors(0), grid))
    }
    
    val st = randomStart(grid.length, grid(0).length)
    val floors = dfs(st._2, placeRooms(_rooms, List(st._1)))
    println("Initial flooring laid")
    val maze = (1 to grid.length-2).toList.foldLeft(List[Coord]())((l, r) => {
      (1 to grid(0).length-2).toList.foldLeft(l)((l, c) => {
         l :+ (r, c)
      })
    })
    .filter(!floors.contains(_))
    .filter(surroundingFloors(_, floors, grid).size < 1)
    .foldLeft(floors)((f, c) => {
        val d = dfs(c, f)
        if (d.size != f.size) d
        else f
    })
    println("Maze done")
    
    return carve(maze, grid)
  }  
   
  //Helpers below here
  
  //Tests if the coordinate is within the boundaries of the maze
  def isValidCoord(co:Coord, g:Grid):Boolean = (!(co._1 < 1 || co._1 > g.length - 2 || co._2 < 1 || co._2 > g(0).length - 2))
  
  //Create a Coord based off an Int. 0 = Up, 1 = Right, 2 = Down, 3 = Left
  def dir(d:Int):Coord = (if (d == 1 || d == 3) 0 else if (d == 0) -1 else 1, if (d == 0 || d == 2) 0 else if (d == 1) 1 else -1)
  
  //Get the neighboring coordinate given a direction
  def cast(d:Int, co:Coord): Coord = (co._1 + dir(d)._1, co._2 + dir(d)._2)
  
  //Pokes a hole in the Grid (used for placing floors)
  def poke(co:Coord, g:Grid): Grid = g.updated(co._1, g(co._1).updated(co._2, new Space(false)))
  
  //Create a new Grid filled with walls
  def build(w:Int, h:Int, g:Grid) : Grid = Array.fill(h)(Array.fill(w)(new Space(true)))
  
  //Prints the spaces within the Grid row by row
  def print(g:Grid) = g.foldLeft("")((s, r) => s + "\n" + r.mkString(""))
  
  //Gets a list of floors that surround a space
  def surroundingFloors(c:Coord, f:List[Coord], grid:Grid):List[Coord] = {
    val dirs = List(0,1,2,3);
    List(cast(dirs(0), c), cast(dirs(1), c), cast(dirs(2), c), cast(dirs(3), c))
      .filter(d => (isValidCoord(d, grid) && f.contains(d)))
  }
}