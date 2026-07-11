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
                String who = String.valueOf(ctx.session.getRemoteAddress());
                System.out.println("新規接続: " + who);
            });

            ws.onClose(ctx -> {
                clients.remove(ctx);
                String who = String.valueOf(ctx.session.getRemoteAddress());
                System.out.println("切断: " + who);
            });

            ws.onMessage(ctx -> {
                String message = ctx.message();
                System.out.println("メッセージ受信: " + message);

                clients.forEach(client -> {
                    try {
                        client.send(message);
                    } catch (Exception ignored) {
                        // 後で removeIf で除去
                    }
                });

                clients.removeIf(client -> !client.session.isOpen());
            });
        });

        app.start(8080);
    }
}