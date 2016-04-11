package maze

class Space(isWall:Boolean) {
  def isWall():Boolean = isWall;
  override def toString():String = if (isWall) "#" else " ";
}