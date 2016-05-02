Functional Maze Generator
======
Developed in Scala for the University of Utah course CS5965 Functional Programming Studio, taught by Matt Flatt.
For the sake of learning, the entirety of this program strictly adheres to the functional programming paradigm.

> Developed alongside my maze-render project: [github link](https://github.com/enewey/maze-render)

> This project based on Bob Nystrom's dungeon generator for rogue-likes. [Blog link here](http://journal.stuffwithstuff.com/2014/12/21/rooms-and-mazes/)

Project Description
------
The goal for this project is to *randomly* generate a large, winding, maze-like dungeon with a start and end point. The maze is a series of walls and floors, where all the floors are guaranteed to be perfectly connected (i.e. every floor space is guaranteed to be accessible). The generated dungeon is then used to generate geometry, which is finally output into .obj files containing vertex, normal, and texture coordinates. These will be used to render the maze as a series of 3D walls and floors.

This dungeon should include several options to fine-tune the look and feel of the dungeon.

1. The size of the maze (width & height).
2. How many "rooms" to generate within the maze.
3. How many dead-ends to leave in the maze.
4. How "winding" the corridors within the maze are.
... The definition of "winding" here is explained below.

###### Rendering project
This project is being developed in conjunction with another project (no repo yet) that will render the output object files as a first-person 3D maze users can actually traverse, with a game loop that contains actual objectives and challenges to overcome.


Running this project
------
The project can be run from the command line using sbt. In the "src" directory, type:
`sbt "run a b c d e f g"`

Note there are **seven** command line arguments to provide. Excluding a single argument will break the program. 

*Perhaps I should make this more user friendly?*

######Command Line Arguments
1. Width of the maze
2. Height of the maze
3. How likely a corridor is to turn a corner rather than be a straight line (0 to 100). The "Winding" factor.
4. How many rooms the maze will attempt to place.
5. Average size of rooms
6. Variance in room size
7. Number of dead-ends to leave in the maze.


Generation Algorithm
------
The maze is a series of floors, stored as a list of (Int, Int) tuples to denote their location in the maze, organized as (row, column). Anything that is not a floor is considered to be a wall. 

####Room Placement
At the start of generation, the program attempts to place a room -- a group of floors aligned in a rectangle -- in the maze X times, where X is one of the arguments provided. This is a random brute-force algorithm: it picks a spot, and tries to place a room. If the room does not overlap with or border any other floors, it places the room in that spot. Otherwise, it picks a new spot and tries again. The generator stops placing rooms after X attempts and moves on to drawing corridors.

####Drawing corridors
After the rooms are placed, the generator fills up the spaces between the rooms with corridors, or lengths of floors. There are a few rules for generating corridors in order to maintain a *nice* looking maze:

1. Corridors must never intersect.
2. Corridors must exist at least one wall apart from each other.

To adhere to these rules, a simple algorithm was developed. Using a somewhat randomized depth-first search, the corridors start drawing from the first eligible point it can find. "Eligible" in this context means a space with no other neighboring floors. From the point, it picks a direction priority (up, down, left, or right), and draws in each direction in order, depth-first.
> Note: The direction priority selection is influenced by the "winding" factor supplied in the command-line arguments. The winding factor dictates how likely the maze is to pick a random new direction rather than remain in a straight line. So if the winding factor is 75, the maze will prioritize the same direction 25% of the time.

When drawing the next corridor, it picks the next two spaces in that direction, and determines if they are eligible spaces. If either point is not eligible, it gives up on drawing in that direction and proceeds to draw in the next direction in the list. Once the generator can no longer draw any floors anywhere -- that is, ALL eligible spaces are filled with corridors -- the maze is done drawing corridors.

One of the challenges in making the maze look *nice* is determining how to draw the maze in a way that only leaves at most one space between the walls. The solution is to draw only on odd-numbered room sizes, and begin drawing corridors on odd-numbered coordinate points. This will leave a nice border of walls around the floors, a single-wall thick. This, naturally, is somewhat abandoned in the final step of the algorithm, but fortunately the *niceness* is still preserved.

####Connecting regions
Now that all the spaces are filled in, the generator is made to **guarantee** that all floors within the maze are made perfectly connected (all floors can be accessed from any other floor one way or another). To do this, the generator finds all connected *Regions*. A Region in this context is a set of floors where each floor can be accessed from every other floor in the set.
> Note: The generator actually keeps track of the maze regions while the maze is being generated. Since the rooms are already guaranteed not to overlap, and the corridors drawn are guaranteed not to intersect with any other floors, each room and set of corridors are therefore individual regions.

With a list of Regions, the generator then works on connecting the regions. It starts with the smallest regions first, and then works up from there. First, it finds a list of eligible "border" points in the Region, which is the floors that have at least one bordering wall (or, in this case, the floors that do *not* have four surrouding floors). Then, it attempts to *cast* through the bordering walls and attempts to find a floor within another region. If it manages to find a floor belonging to another region, those regions are then joined together, with a floor joining the two together. The process then repeats, until all the Regions are joined together into one big Region.

####Trimming dead-ends
At this point, we have fully connected maze with lots of big rooms and dead-ends. We could stop here, and in fact if the relevant argument is 0, the generator *will* stop here; however, we can make the maze look a bit neater if we trim a few of the dead-ends. In fact, if the provided argument is high enough, the generator will remove *all* dead ends, and the maze will then be a series of rooms with connecting corridors, nothing else inbetween.

The process for trimming the dead-ends is easy. It finds a floor with 3 surrounding walls, and removes it. It will do this X times (X being the command-line argument), or until there are no more eligible floors to remove.

####Picking a start and end point
Now that our maze is *almost* done, we need to decide on a starting and ending point in the maze. The strategy used here is not really all that complicated. 

1. For the starting point, we pick a floor at random.
2. For the ending point, it gets a little more involved.
    * First, we define all eligible ending points as floors with a neighboring floor on all sides.
    * When we have our eligible points, we sort the list of points by how far away they are from the starting point. This is done by using the cartesian distance formula, not through any traversal algorithm. I just wanted the start and end to *look* far away from each other, more than anything else.
    * Lastly, we cut the list of eligible points in half, selecting the farthest points away, and then pick a point at random from those points.

With this, we should have a starting and ending point for our maze that, while maybe not super complicated, will at least be aesthetically pleasing (which has been the driving force behind almost the entirety of this project if I'm being honest)

####And we're done!
After trimming the dead-ends, the maze is generated and complete. At this point, the program will output a unicode representation of the maze to the terminal, and wait for user input. Pressing any key will output the generated maze as 3D geometry into two .obj files: one for floors, and one for walls. The maze will also output a **.mdf** file, which is a file containing data to be used for game logic.

.obj File
------
For an explanation of how .obj files are formatted, visit [the wikipedia article](https://en.wikipedia.org/wiki/Wavefront_.obj_file).

The .obj files this project produces is a series of vertex ("v"), texture ("vt"), and normal ("vn") vectors, followed by the polygonal face elements ("f").

######Possible improvements/optimiaztions
This project generates whole cubes for every wall space. This will produce several quads directly next to each other, and therefore never actually be seen or rendered necessarily. This is quite inefficient. I plan to make the project only output geometry for walls that border the floors.

.mdf File
------
The output .mdf file format goes like this:

* The first line is six integers.
    + First two integers represent the number of rows and columns in the maze
    + Next two numbers is the row and column for the starting location
    + Last two numbers is the row and column for the ending position

* Every line there after is a space-separated pair of integers (row column), representing the coordinate points for each **wall** in the maze.

######Possible improvements
I recognize that this file format is pretty clearly a quick-and-dirty, and perhaps naive approach to things. I don't know the best approach to collision detection, unfortunately. Until I do, this will have to suffice.

Contact
------
I love answering questions and taking comments. Please email me: erich.newey *at* gmail *dot* com