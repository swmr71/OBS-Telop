import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final Set<WsContext> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        });

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                clients.add(ctx);
                System.out.println("新規接続: " + ctx.sessionId());
            });

            ws.onClose(ctx -> {
                clients.remove(ctx);
                System.out.println("切断: " + ctx.sessionId());
            });

            ws.onMessage(ctx -> {
                String message = ctx.message();
                System.out.println("メッセージ受信: " + message);

                for (WsContext client : clients) {
                    try {
                        client.send(message);
                    } catch (Exception e) {
                        clients.remove(client);
                    }
                }
            });
        });

        app.start(8080);
    }
}