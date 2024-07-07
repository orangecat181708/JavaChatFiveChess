import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 棋盘类
 */
public class ChessBoard extends JPanel {

    private static final int CHESSBOARD_SIZE = 15;//棋盘条数
    private int margin = 20;//外边距

    //创建一个容器，用于储存棋子对象
    private List<Location> locationList = new ArrayList<>();
    //创建一个数组，用于表示棋盘上被占用的位置
    private int[][] location = new int[CHESSBOARD_SIZE][CHESSBOARD_SIZE];



    @Override
    public void paint(Graphics g) {
        super.paint(g);
        //调用画棋盘的方法
        drawChessBoard(g);
        drawChess(g);
    }

    /**
     * 画棋盘
     * @param g
     */
    private void drawChessBoard(Graphics g){
        //获取宽度
        int cellsize = (getWidth() - 2*margin)/(CHESSBOARD_SIZE - 1);
        for (int i = 0; i < CHESSBOARD_SIZE; i++) {
            //画横线
            g.drawLine(margin,margin + cellsize*i,getWidth()-margin,margin + cellsize*i);
            //画竖线
g.drawLine(margin + cellsize*i,margin,margin + cellsize*i,getHeight() - margin);
        }
    }
    /**
     * 画棋子
     */
    public void drawChess(Graphics g){
        for (int i = 0; i < locationList.size(); i++) {
            Location loc = locationList.get(i);
            if (loc.getOwner()==1){
                //黑棋
                g.setColor(Color.black);
            }else{
                //白棋
                g.setColor(Color.white);
            }
            int cellsize = (getWidth()-2*margin)/(CHESSBOARD_SIZE-1);
            g.fillOval(loc.getX()*cellsize+margin-cellsize/2,loc.getY()*cellsize+margin-cellsize/2,cellsize,cellsize);
        }

    }

    /**
     * 落子的方法
     */
    public void addchess(Location location){
        locationList.add(location);
        repaint();
    }
    /**
     * 获取格子大小
     */
    public int getCellSize(){
        return (getWidth()-2*margin)/(CHESSBOARD_SIZE-1);
    }

    /**
     * 判断是否可以落子
     */
    public boolean isLegal(int x,int y){
        if (location[x][y] == 0&&x>=0&&x<=CHESSBOARD_SIZE&&y>=0&&y<=CHESSBOARD_SIZE){
            return true;
        }
        return  false;
    }
    /**
     * 落子后占用棋盘的位置
     */
    public void addLocation(int x,int y,int owner){
        location[x][y] = owner;
    }
    /**
     * 判断输赢
     */
    public boolean isWin(int x,int y,int owner){
        //创一个变量用来记录，同一个方向的相同棋子个数
        int sum = 0;
        //判断水平方向
        //水平左边
        for (int i = x-1; i >=0 ; i--) {
            if (location[i][y] == owner){
                sum++;
            }else{
                break;
            }
        }
        //水平右边
        for (int i = x+1; i <= CHESSBOARD_SIZE ; i++) {
            if (location[i][y] == owner){
                sum++;
            }else {
                break;
            }
        }
        if (sum>=4){
            return true;
        }
        sum = 0;
        //判断垂直方向
        for (int i = y-1; i >=0 ; i--) {
            if (location[x][i] == owner){
                sum++;
            }else {
                break;
            }
        }
        for (int i = y+1; i <=CHESSBOARD_SIZE ; i++) {
            if (location[x][i] == owner){
                sum++;
            }else {
                break;
            }
        }
        if (sum>=4){
            return true;
        }
        sum = 0;
        //判断左上方
        for (int i = x-1,j = y-1; i >=0&&j>=0 ; i--,j--) {
            if (location[i][j] == owner){
                sum++;
            }else {
                break;
            }
        }
        for (int i = x+1,j = y+1; i <=CHESSBOARD_SIZE&&j<=CHESSBOARD_SIZE ; i++,j++) {
            if (location[i][j] == owner){
                sum++;
            }else {
                break;
            }
        }
        if (sum>=4){
            return true;
        }
        sum = 0;
        //判断右上方
        for (int i = x+1,j = y-1; i <=CHESSBOARD_SIZE&&j>=0 ; i++,j--) {
            if (location[i][j] == owner){
                sum++;
            }else {
                break;
            }
        }
        for (int i = x-1,j = y+1; i >=0&&j<=CHESSBOARD_SIZE ; i--,j++) {
            if (location[i][j] == owner){
                sum++;
            }else {
                break;
            }
        }
        if (sum>=4){
            return true;
        }
        return false;
    }
    //——————————调试——————————————
    public String getloc(){
        String str = "";
        for (Location l:locationList){
            str = str + ","+l.getX();
        }
        return str;
    }
}
