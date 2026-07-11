import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    // 接続されているすべてのブラウザのセッションを管理
    private static final Set<WsContext> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            // src/main/resources/public の中身をWebページとして配信する設定
            config.staticFiles.add("/public");
        }).start(8080); // ポート8080で起動

        // WebSocketの受付窓口
        app.ws("/ws", ws -> {
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
                    if (client.isOpen()) {
                        client.send(message);
                    }
                }
            });
        });
    }
}
