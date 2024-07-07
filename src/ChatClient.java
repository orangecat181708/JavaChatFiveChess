import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;


/**
 * 客户端存储棋子位置，和是否占位，做出落子是否合法
 * 客户端---》服务端：点击位置
 * 服务端---》客户端：更新后的棋子位置
 * 客户端存储旧的棋子位置，做出落子是否合法
 * 服务端根据客户端提供新的合法落子位置更新存储棋子位置，并做出胜负判断
 */

public class ChatClient extends Application {

    private static String SERVER_HOST;//服务器的主机名或 IP 地址
    private static int SERVER_PORT;//服务器的端口号
    private String username;//当前客户端使用的用户名
    private Socket socket;//用于建立与服务器的连接
    private BufferedReader reader;//用于读取服务器发送的消息
    private PrintWriter writer;//用于向服务器发送消息
    private TextArea messageArea;//JavaFX用户界面中的文本区，用于显示聊天内容
    private JFrame frame = new JFrame();
    private ChessBoard chressboard = new ChessBoard();

    private int owner;//己方颜色
    private int round;//回合:0为自己的回合，1为对方回合

    @Override
    public void start(Stage primaryStage) throws Exception {
// 弹出UI窗口，让用户输入服务器IP地址和端口号
        SERVER_HOST = JOptionPane.showInputDialog("服务器IP地址：");
        SERVER_PORT = Integer.parseInt(JOptionPane.showInputDialog("服务器端口号："));
        if (!isValidPort(String.valueOf(SERVER_PORT))) {
            showErrorDialog("无效的端口号：" + SERVER_PORT); //检测端口号的合法性
            return;
        }
        BorderPane root = new BorderPane();

        VBox vBox = new VBox();
        HBox hBox = new HBox();

        TextField usernameField = new TextField();
        Button connectBtn = new Button("连接");
        Label statusLabel = new Label("未连接");

        messageArea = new TextArea();
        messageArea.setEditable(false);

        TextField inputField = new TextField();
        Button sendBtn = new Button("发送");

        Button chessBtn = new Button("五子棋");
        chessBtn.setVisible(false);

        usernameField.setPromptText("昵称");
        usernameField.setPrefWidth(100);

        connectBtn.setOnAction(event -> {
            String username = usernameField.getText();
            if (username.trim().isEmpty()) {
                showErrorDialog("请输入昵称");
                return;
            }

            /**
             * 五子棋弹窗
             */
            chessBtn.setOnAction(event1 -> {
                connectBtn.setDisable(true);
                ChessFrame();
            });


            try {
                connectToServer(username);
                statusLabel.setText("已连接");
                connectBtn.setDisable(true);
                chessBtn.setVisible(true);
                usernameField.setDisable(true);
                inputField.setDisable(false);
                sendBtn.setDisable(false);
            } catch (IOException ex) {
                showErrorDialog("连接失败：" + ex.getMessage());
            }
        });

        statusLabel.setPrefWidth(50);

        hBox.getChildren().addAll(usernameField, connectBtn, statusLabel,chessBtn);
        hBox.setSpacing(10);
        hBox.setPadding(new Insets(10));
        hBox.setAlignment(Pos.CENTER);

        messageArea.setPrefRowCount(10);
        messageArea.setPrefWidth(500);
        messageArea.setWrapText(true);
        VBox messageBox = new VBox();
        messageBox.getChildren().addAll(new Label("聊天记录:"), messageArea);
        messageBox.setSpacing(10);
        messageBox.setPadding(new Insets(10));
        VBox.setVgrow(messageArea, Priority.ALWAYS);

        inputField.setPromptText("请输入消息...");
        inputField.setDisable(true);
        sendBtn.setDisable(true);
        sendBtn.setOnAction(event -> {
            String message = inputField.getText();
            if (message.trim().isEmpty()) {
                showErrorDialog("消息内容不能为空");
                return;
            }
//发送消息
            sendMessage(message);
// 在窗口中显示自己的消息
            messageArea.appendText(username + " : " + message + "\n");
            inputField.clear();
            inputField.requestFocus();
        });

        VBox inputBox = new VBox();
        inputBox.getChildren().addAll(inputField, sendBtn);
        inputBox.setSpacing(10);
        inputBox.setPadding(new Insets(10));

        vBox.getChildren().addAll(hBox, messageBox, inputBox);
        vBox.setPadding(new Insets(10));
        VBox.setVgrow(messageBox, Priority.ALWAYS);

        root.setCenter(vBox);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("聊天室");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                    reader.close();
                    writer.close();
                } catch (IOException ex) {
// ignore
                }
            }
            Platform.exit();
        });
        primaryStage.show();
    }
    /**
     * 检测端口号的合法性
     * @param portText
     * @return
     */
    private boolean isValidPort(String portText) {
        try {
            int port = Integer.parseInt(portText);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 建立与服务器的连接
     * @param username
     * @throws IOException
     */
    private void connectToServer(String username) throws IOException {
        this.username = username;
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")); // 添加编码方式
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // 添加编码方式

        writer.println(username); // 发送用户名
        writer.flush();

        new Thread(() -> {
            while (true) {
                try {
                    String msg = reader.readLine();
                    if (msg == null) {
                        throw new Exception();
                    }
                    if (msg.startsWith("@Color")) {
                        try {
                            int colorIndex = msg.indexOf("@Color") + 6;
                            if (colorIndex < msg.length()) {
                                owner = Integer.parseInt(msg.substring(colorIndex)) % 2;
                                round = 0;
                                if (owner == 0) {
                                    owner = -1;
                                    round = 1;
                                }
                            } else {
                                throw new Exception("Invalid @Color message format");
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in @Color message: " + msg);
                            e.printStackTrace();
                        }
                        continue;
                    }

                    int atIndex = msg.lastIndexOf("@");
                    if (atIndex != -1 && msg.substring(atIndex).startsWith("@Chess")) {
                        try {
                            int commaIndex = msg.lastIndexOf(",");
                            int dotIndex = msg.lastIndexOf(".");
                            if (commaIndex != -1 && dotIndex != -1 && dotIndex > commaIndex) {
                                int X = Integer.parseInt(msg.substring(atIndex+6, commaIndex));
                                int Y = Integer.parseInt(msg.substring(commaIndex + 1, dotIndex));
                                int other = Integer.parseInt(msg.substring(dotIndex + 1));
                                chressboard.addchess(new Location(X, Y, other));
                                chressboard.addLocation(X, Y, other);
                                round = 0;
                            } else {
                                throw new Exception("Invalid @Chess message format");
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in @Chess message: " + msg);
                            e.printStackTrace();
                        }
                        continue;
                    }
                    if (atIndex !=-1&&msg.substring(atIndex).startsWith("@Win")){
                        try {
                            int WinIndex = msg.indexOf("@Win") + 4;
                            if (WinIndex < msg.length()) {
                                String  Winner = msg.substring(WinIndex);
                                System.out.println(Winner);
                                if (username.equals(Winner)) {
                                    JOptionPane.showMessageDialog(frame, "恭喜获胜", "游戏胜利", JOptionPane.PLAIN_MESSAGE);
                                }else {
                                    JOptionPane.showMessageDialog(frame,"您失败了","游戏失败",JOptionPane.PLAIN_MESSAGE);
                                }

                            } else {
                                throw new Exception("Invalid @Win message format");
                            }
                        }catch (NumberFormatException e){
                            System.out.println("Invalid number format in @Win message: " + msg);
                            e.printStackTrace();
                        }
                        continue;
                    }


                    Platform.runLater(() -> {
                        messageArea.appendText(msg + "\n");
                    });
                } catch (Exception ex) {
                    disconnect();
                    System.out.println(ex.getMessage());
                    break;
                }
            }
        }).start();
    }

    /**
     * 发送消息
     * @param message
     */
    private void sendMessage(String message) {
        writer.println(message);
        writer.flush();
    }

    /**
     * 断开连接
     */
    private void disconnect() {
        try {
            reader.close();
            writer.close();
            socket.close();
            Platform.runLater(() -> {
                messageArea.appendText("已经断开连接\n");
            });
        } catch (IOException ex) {
// ignore
        }
    }

    /**
     * 弹窗报错
     * @param msg
     */
    private void showErrorDialog(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * 五子棋棋盘
     * @param
     */
    public void ChessFrame(){
        //设置窗体标题
        frame.setTitle("五子棋");
        //设置窗体大小
        frame.setSize(518,540);
        //设置窗体居中
        frame.setLocationRelativeTo(null);
        //设置窗体关闭
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //添加棋盘
        frame.add(chressboard);
        //设置窗体显示
        frame.setVisible(true);

        chressboard.addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             *
             * @param e
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                //调用画棋子的方法
                writer.println(play(e));
            }
        });
        }
    private String play(MouseEvent e) {
        //棋盘的格子大小
        int cellsize = chressboard.getCellSize();
        //将鼠标的位置---》棋盘的位置
        int x =(e.getX()-5)/cellsize;
        int y =(e.getY()-5)/cellsize;
        if (chressboard.isLegal(x,y)&&round == 0) {
            chressboard.addchess(new Location(x,y,owner));
            //添加棋子在棋盘上占用的位置
            chressboard.addLocation(x,y,owner);
            round = 1;
            return "@Chess"+x+","+y+"."+owner;
        }
        return "";
    }


    public static void main(String[] args) {
        launch(args);
    }
}
