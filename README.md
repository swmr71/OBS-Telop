# OBS-Telop
[ 遠隔操作パネル ] (スマホや別PCのブラウザ)
       │
       │ (1) YouTubeリンクやテロップ内容を送信 (HTTP or WebSocket)
       ▼
[ Javaサーバー ] (Webサーバー 兼 WebSocketサーバー)
       │
       │ (2) 表示側HTMLへ「動画切り替え」や「テロップ表示」の指示を転送
       ▼
[ OBSのブラウザソース ] (Javaが配信する固定の表示用HTML)


my-obs-app/
├── pom.xml
└── src
    └── main
        ├── java
        │   └── Main.java
        └── resources
            └── public
                ├── admin.html     <- スマホや別PCから開く操作画面
                └── display.html   <- OBSのブラウザソースに読み込ませる画面

