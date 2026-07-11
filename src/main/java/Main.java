import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    // 接続されているすべてのブラウザのセッションを管理
    private static final Set<WsContext> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        // Javalin.create の中で、サーバーの設定と同時にWebSocketのルートも定義する
        Javalin app = Javalin.create(config -> {
            // src/main/resources/public の中身をWebページとして配信する設定
            config.staticFiles.add("/public");

            // 【変更点1】Javalin 6/7では、WebSocketのルート設定は config.jetty.ws() 内で行う
            config.jetty.ws("/ws", ws -> {
                // 新しくブラウザが接続してきたとき
                ws.onConnect(ctx -> {
                    clients.add(ctx);
                    System.out.println("新規接続: " + ctx.sessionId());
                });

                // ブラウザが閉じられたとき
                ws.onClose(ctx -> {
                    clients.remove(ctx);
                    System.out.println("切断: " + ctx.sessionId());
                });

                // 操作画面などからメッセージ（URLなど）が届いたとき
                ws.onMessage(ctx -> {
                    String message = ctx.message();
                    System.out.println("メッセージ受信: " + message);
                    
                    // 接続されているすべてのブラウザへそのまま転送
                    for (WsContext client : clients) {
                        // 【変更点2】Javalin 6/7では、直接 .isOpen() ではなく .session().isOpen() を使用する
                        if (client.session().isOpen()) {
                            client.send(message);
                        }
                    }
                });
            });
        }).start(8080); // ポート8080で起動
    }
}
