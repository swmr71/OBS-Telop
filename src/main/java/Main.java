import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final Set<WsContext> clients = ConcurrentHashMap.newKeySet();
    
    // 現在の状態を保持
    private static String currentVideoId = "";
    private static TelopState currentTelop = new TelopState("", false);
    
    static class TelopState {
        String text;
        boolean visible;
        
        TelopState(String text, boolean visible) {
            this.text = text;
            this.visible = visible;
        }
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        });

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                clients.add(ctx);
                String who = String.valueOf(ctx.session.getRemoteAddress());
                System.out.println("新規接続: " + who);
                
                // 接続時に現在の状態をクライアントに送る
                sendCurrentState(ctx);
            });

            ws.onClose(ctx -> {
                clients.remove(ctx);
                String who = String.valueOf(ctx.session.getRemoteAddress());
                System.out.println("切断: " + who);
            });

            ws.onMessage(ctx -> {
                String message = ctx.message();
                
                // 生存確認（ping）のメッセージは無視
                if (message.contains("\"type\":\"ping\"")) {
                    return;
                }
                
                System.out.println("メッセージ受信: " + message);
                
                try {
                    // JSONパース
                    JSONObject json = new JSONObject(message);
                    String type = json.optString("type", "");
                    
                    // YouTube
                    if ("youtube".equals(type)) {
                        currentVideoId = json.optString("data", "");
                        broadcastToAll(message);
                    }
                    // テロップ
                    else if ("telop".equals(type)) {
                        String action = json.optString("action", "");
                        String text = json.optString("data", "");
                        
                        if ("show".equals(action)) {
                            currentTelop = new TelopState(text, true);
                        } else if ("hide".equals(action)) {
                            currentTelop = new TelopState(currentTelop.text, false);
                        }
                        broadcastToAll(message);
                    }
                } catch (Exception e) {
                    System.err.println("JSON解析エラー: " + e.getMessage());
                    broadcastToAll(message);
                }
            });
        });

        app.start(8080);
    }
    
    // 現在の状態を送信
    private static void sendCurrentState(WsContext ctx) {
        // YouTube 状態
        if (!currentVideoId.isEmpty()) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "youtube");
                json.put("data", currentVideoId);
                ctx.send(json.toString());
                System.out.println("状態送信: YouTube ID=" + currentVideoId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // テロップ状態
        if (currentTelop.visible) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "telop");
                json.put("action", "show");
                json.put("data", currentTelop.text);
                ctx.send(json.toString());
                System.out.println("状態送信: テロップ=" + currentTelop.text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // 全クライアントにブロードキャスト
    private static void broadcastToAll(String message) {
        for (WsContext client : clients) {
            if (client.session.isOpen()) {
                client.send(message);
            }
        }
    }
}
