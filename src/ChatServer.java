
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer extends Application {
    private final int DEFAULT_SERVER_PORT = 8888;//默认的服务器端口号
    private int serverPort;//表示当前服务器开放的端口号
    private ExecutorService executor;//用于管理多个客户端线程，处理客户端请求
    private ServerSocket serverSocket;//用于监听客户端请求
    private List<ClientThread> clients = new ArrayList<>();//存储所有与该服务器相连的 ClientThread 对象
    private TextArea console;//JavaFX用户界面中的文本区，用于显示服务器日志
    private Button startBtn;//JavaFX用户界面中的按钮，用于启动和停止服务器
    private ListView<String> clientListView;//JavaFX用户界面中的列表视图，用于显示所有连接到服务器的客户端
    private TextArea messageArea;//JavaFX用户界面中的文本区，用于显示和编辑聊天内容
    private String serverIP;//当前服务器的 IP 地址
    private FileOutputStream file;//服务器日志文件的输出流
    private FileOutputStream outputStream;//创建输出流的对象，用于修改服务器日志文件
    private ChessBoard chessboard = new ChessBoard();

    public static void main(String[] args) {
        launch(args);
//设置关闭服务器钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
// 这里可以编写服务端进程结束前需要执行的代码
// 比如关闭数据库连接、保存数据等操作

            System.out.println("服务端进程结束"); //在终端发送结束信息
        }));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {


//布局模式
        BorderPane root = new BorderPane();
        VBox vBox = new VBox();
        HBox hBox = new HBox();

//获取服务器IP
        try {
//获取服务器IP
            InetAddress inetAddress = InetAddress.getLocalHost();
            serverIP = inetAddress.getHostAddress();
        } catch (UnknownHostException ex) { //异常处理
            ex.printStackTrace();
        }
        Text labelIP = new Text("服务器地址：" + serverIP); //服务器IP文本框

        TextField portField = new TextField(); //服务器端口号输入框
        startBtn = new Button("启动服务"); //启动服务器按钮
        console = new TextArea();
        console.setEditable(false);

        messageArea = new TextArea();
        messageArea.setEditable(false);

        clientListView = new ListView<>();

// 设置默认的服务器端口号
        serverPort = DEFAULT_SERVER_PORT;
//服务器端口号输入框初始化
        portField.setPromptText("端口号"); //输入框内部提示字
        portField.setText("" + DEFAULT_SERVER_PORT);
//启动服务器按钮按下动作
        startBtn.setOnAction(event -> {
            String portText = portField.getText(); //获取服务器端口号输入框中内容
            if (!isValidPort(portText)) {
                showErrorDialog("无效的端口号：" + portText); //检测端口号的合法性
                return;
            }

            serverPort = Integer.parseInt(portText); //获取服务器端口号输入框中内容
            try {
                startServer(); //启动服务器
                console.appendText("[系统消息] 服务器已启动，端口号：" + serverPort + "\n"); //服务器日志中反馈信息
                startBtn.setDisable(true); //按钮变化成不可点击状态
            } catch (IOException ex) {
                showErrorDialog("服务器启动失败：" + ex.getMessage()); //弹窗反馈错误信息
            }
        });

//组件布局
        hBox.getChildren().addAll(labelIP,portField, startBtn);
        hBox.setSpacing(10);
        hBox.setPadding(new Insets(10));
        hBox.setAlignment(Pos.CENTER);
//服务器日志部分
        console.setFont(Font.font("monospace"));
        console.setPrefRowCount(10);
        console.setPrefWidth(500);
        console.setPrefHeight(400);
        console.setWrapText(true);
        VBox consoleBox = new VBox();
        consoleBox.getChildren().addAll(new Label("服务器日志:"), console);
        consoleBox.setSpacing(10);
        consoleBox.setPadding(new Insets(10));
//创建服务器日志文件
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
        String timestamp = df.format(new Date());
        file = new FileOutputStream(".\\"+timestamp+"日志.txt");
//给输出流添加路径
        outputStream=new FileOutputStream(".\\"+timestamp+"日志.txt");
//客户端消息部分
        messageArea.setFont(Font.font("monospace"));
        messageArea.setPrefRowCount(10);
        messageArea.setPrefWidth(500);
        messageArea.setPrefHeight(400);
        messageArea.setWrapText(true);
        VBox messageBox = new VBox();
        messageBox.getChildren().addAll(new Label("客户端消息:"), messageArea);
        messageBox.setSpacing(10);
        messageBox.setPadding(new Insets(10));
//客户端列表部分
        clientListView.setPrefHeight(400);
        VBox clientBox = new VBox();
        clientBox.getChildren().addAll(new Label("客户端列表:"), clientListView);
        clientBox.setSpacing(10);
        clientBox.setPadding(new Insets(10));
//部分布局
        GridPane gridPane = new GridPane();
        gridPane.add(consoleBox, 0, 0);
        gridPane.add(messageBox, 1, 0);
        gridPane.add(clientBox, 2, 0);
        gridPane.setHgap(10);

        vBox.getChildren().addAll(hBox, gridPane);
        vBox.setPadding(new Insets(10));
        VBox.setVgrow(gridPane, Priority.ALWAYS);

        root.setCenter(vBox);
//主窗口
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("服务端");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    executor.shutdownNow();
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
     * 弹窗报错方法
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
     * 启动服务端的监听
     * @throws IOException
     */
    private void startServer() throws IOException {
        serverSocket = new ServerSocket(serverPort);
        executor = Executors.newCachedThreadPool();
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.execute(new ClientThread(this, socket));
                } catch (IOException ex) {
// ignore
                }
            }
        }).start();
    }

    /**
     * 客户端加入
     * @param client
     */
    public void addClient(ClientThread client) {
        clients.add(client);
        //根据客户端连入的数量来决定棋子的颜色
        client.sendMessage("@Color"+String.valueOf(clients.size()));
        /*if (clients.size()%2==0){
            client.owner = -1;
        }else{
            client.owner = 1;
        }
         */
        Platform.runLater(() -> {
            clientListView.getItems().add(client.username + " [" + client.socket.getInetAddress() + ":" + client.socket.getPort() +"]\n");
            console.appendText(client.username + " [" + client.socket.getInetAddress() + ":" + client.socket.getPort() + "]加入聊天室\n"); //服务器日志中反馈信息
            try {
                outputStream.write((client.username + " [" + client.socket.getInetAddress() + ":" + client.socket.getPort() + "]\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 客户端退出
     * @param client
     */
    public void removeClient(ClientThread client) {
        clients.remove(client);
        Platform.runLater(() -> {
            clientListView.getItems().remove(client.username + " [" + client.socket.getInetAddress() + ":" + client.socket.getPort() + "]\n");
            console.appendText(client.username + " [" + client.socket.getInetAddress() + ":" + client.socket.getPort() + "]离开聊天室\n"); //服务器日志中反馈信息
            try {
                outputStream.write((client.username + " [" + client.socket.getInetAddress() + ":" + client.socket.getPort() + "]\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     *将一个消息广播给所有连接到服务器的客户端，同时将该消息记录到服务器日志中
     * @param message
     * @param excludeClient
     * @throws IOException
     */
    public void broadcast(String message, ClientThread excludeClient) throws IOException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = df.format(new Date());
        outputStream.write(("[" + timestamp + "] " + message + "\n").getBytes());
        for (ClientThread client : clients) {
            if (client != excludeClient) {
                client.sendMessage("[" + timestamp + "] " + message);
            }
        }
    }

    /**
     * 客户端线程
     * 不同的客户端区别在于socket.port,随着客户的增多依次递增
     */
    class ClientThread extends Thread {
        private String username; //用户名
        private ChatServer server;
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;

        /*
        五子棋输入输出
         */

        public ClientThread(ChatServer server, Socket socket) {
            this.server = server;
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));// 添加编码方式
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // 添加编码方式


// 读取客户端传入的用户名
                username = reader.readLine();
                server.addClient(this);
                server.broadcast(username + " 进入聊天室。", null); // 广播欢迎消息
                sendMessage("欢迎来到聊天室！");

                start(); // 启动客户端线程
            } catch (IOException ex) {
                server.removeClient(this);
                try {
                    socket.close();
                } catch (IOException e) {
// ignore
                }
                System.err.println("Error in ClientThread constructor: " + ex);
            }
        }

        /**
         * 发送信息
         * @param message
         */
        public void sendMessage(String message) {
            writer.println(message);
            writer.flush();
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {

                    String message = reader.readLine();
                    if (message == null) {
                        disconnect(); // 客户端断开连接
                        break;
                    }
                    if (message.equals("")){
                        continue;
                    }
                    if (message.startsWith("@Chess")) {
                        try {
                            int commaIndex = message.lastIndexOf(",");
                            int dotIndex = message.lastIndexOf(".");

                            if (commaIndex == -1 || dotIndex == -1 || commaIndex <= 6 || dotIndex <= commaIndex) {
                                throw new Exception("Invalid @Chess message format: " + message);
                            }

                            int X = Integer.parseInt(message.substring(6, commaIndex));
                            int Y = Integer.parseInt(message.substring(commaIndex + 1, dotIndex));
                            int owner = Integer.parseInt(message.substring(dotIndex + 1));

                            chessboard.addchess(new Location(X, Y, owner));
                            chessboard.addLocation(X,Y,owner);
                            if (chessboard.isWin(X,Y,owner)){
                               server.broadcast("@Win"+username,null);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in @Chess message: " + message);
                            e.printStackTrace();
                        } catch (Exception e) {
                            System.err.println("Error processing @Chess message: " + message);
                            e.printStackTrace();
                        }
                        //continue;
                    }


                    server.broadcast(username + ": " + message, this);
                    Platform.runLater(() -> {
                        messageArea.appendText(username + ": " + message + "\n");
                    });
                } catch (IOException ex) {
                    disconnect(); // 客户端读取数据失败，断开连接
                    break;
                }
            }
        }

        /**
         * 断开连接
         */
        public void disconnect() {
            try {
                server.removeClient(this);
                server.broadcast(username + " 离开聊天室。", null); // 广播离开消息
                sendMessage("您已离开聊天室。");
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException ex) {
                System.err.println("Error when disconnecting a client: " + ex);
            }
        }
    }
}