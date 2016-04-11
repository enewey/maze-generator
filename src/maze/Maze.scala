package maze

class Maze {
  
  type Grid = Array[Array[Space]]
  type Coord = (Int, Int)
  
  
  def generate(grid:Grid) : Grid = {
    
    //The recursive randomized DFS call
    def dfs(co:Coord, floors:List[Coord]) : List[Coord] = {
      
      //Gets a list of floors that surround a space
      def surroundingFloors(c:Coord, f:List[Coord]):List[Coord] = {
        val dirs = List(0,1,2,3);
        List(cast(dirs(0), co), cast(dirs(1), co), cast(dirs(2), co), cast(dirs(3), co))
          .filter(c => (isValidCoord(c, grid) && floors.contains(c)))
      }
      
      //Base case. Continue only if...
      //  1) If there is at most one floor neighboring the space already. This maintains the "corridor" look.
      //  2) If the floor is within the boundaries of the maze
      //  3) If it's not already a floor.
      if (surroundingFloors(co, floors).size > 1 || !isValidCoord(co, grid) || floors.contains(co)) {
        return floors
      }
      else {
        val f0 = co :: floors //add the coord to the floors list (i.e. "visited")
        println(print(carve(f0, grid)))
        Thread.sleep(10L);
        val dirs = scala.util.Random.shuffle(List(0,1,2,3)) //randomly visit neighboring spaces
        val casts = List(cast(dirs(0), co), cast(dirs(1), co), cast(dirs(2), co), cast(dirs(3), co))
                      .filter(c => (surroundingFloors(c, f0).size < 2 && isValidCoord(c, grid) && !f0.contains(c)))         
        return casts.foldLeft(f0)((f, t) => dfs(t, f))
      }
    }
    
    def randomStart(w:Int, h:Int): (Coord, Coord) = {
      val rand = scala.util.Random;
      if (rand.nextBoolean()) {
        val b = rand.nextBoolean()
        val y = rand.nextInt(h-2)+1
        ((if (b) 0 else w-1, y), (if (b) 1 else w-2, y))
      }
      else {
        val b = rand.nextBoolean()
        val x = rand.nextInt(w-2)+1
        ((x, if(b) 0 else h-1), (x, if(b) 1 else h-2))
      }
        
    }
    
    def carve(floors:List[Coord], grid:Grid):Grid = {
      if (floors.isEmpty) return grid
      else return carve(floors.drop(1), poke(floors(0), grid))
    }
    
    val st = randomStart(grid.length, grid(0).length)
    return carve(dfs(st._2, List(st._1)), grid)
  }  
   
  //Helpers below here
  
  //Tests if the coordinate is within the boundaries of the maze
  def isValidCoord(co:Coord, g:Grid):Boolean = (!(co._1 < 1 || co._1 > g.length - 2 || co._2 < 1 || co._2 > g(0).length - 2))
  
  //Create a Coord based off an Int. 0 = Up, 1 = Right, 2 = Down, 3 = Left
  def dir(d:Int):Coord = (if (d == 0 || d == 2) 0 else if (d == 1) 1 else -1, if (d == 1 || d == 3) 0 else if (d == 0) 1 else -1)
  
  //Get the neighboring coordinate given a direction
  def cast(d:Int, co:Coord): Coord = (co._1 + dir(d)._1, co._2 + dir(d)._2)
  
  //Pokes a hole in the Grid (used for placing floors)
  def poke(co:Coord, g:Grid): Grid = g.updated(co._1, g(co._1).updated(co._2, new Space(false)))
  
  //Create a new Grid filled with walls
  def build(w:Int, h:Int, g:Grid) : Grid = Array.fill(h)(Array.fill(w)(new Space(true)))
  
  //Prints the spaces within the Grid row by row
  def print(g:Grid) = g.foldLeft("")((s, r) => s + "\n" + r.mkString(""))
  
  //Unused helpers here
  
//  def getCastingDirs(to:Coord, from:Coord): List[Int] = {
//    val net = (to._1 + from._1, to._2 + from._2)
//    if (net._1 == 0) 
//      if (net._2 == 1) List(0,1,3) //Down
//      else             List(1,2,3) //Up
//    else
//      if (net._2 == 1) List(0,2,3) //Right
//      else             List(0,1,2) //Left
//  }
//  def randomDir(): Coord = {
//    dir(scala.util.Random.nextInt(4))
//  }
//  def addCoords(c:Coord, d:Coord):Coord = {
//    (c._1 + d._1, c._2 + d._2)
//  }
}