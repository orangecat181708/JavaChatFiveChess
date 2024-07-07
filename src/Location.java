/**
 * 棋子对象
 */
public class Location {
    private int x;  //棋盘上X的坐标
    private int y;  //棋盘上Y的坐标
    private int owner;  //棋子来源：1=玩家1；-1=玩家2；0=空
                        //1:黑棋；2：白棋

    /**
     * 构造函数
     * @param x
     * @param y
     * @param owner
     */
    public Location(int x, int y, int owner) {
        this.x = x;
        this.y = y;
        this.owner = owner;
    }

    /**
     * 获取
     * @return x
     */
    public int getX() {
        return x;
    }

    /**
     * 设置
     * @param x
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * 获取
     * @return y
     */
    public int getY() {
        return y;
    }

    /**
     * 设置
     * @param y
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * 获取
     * @return owner
     */
    public int getOwner() {
        return owner;
    }

    /**
     * 设置
     * @param owner
     */
    public void setOwner(int owner) {
        this.owner = owner;
    }

    public String toString() {
        return "Location{x = " + x + ", y = " + y + ", owner = " + owner + "}";
    }
}
