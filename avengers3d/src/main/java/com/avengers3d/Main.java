package com.avengers3d;

import com.avengers3d.core.GameLoop;
import com.avengers3d.game.GameApp;

public final class Main {
    public static void main(String[] args) {
        GameApp app = new GameApp();
        try {
            app.init();
            new GameLoop().run(app);
        } finally {
            app.shutdown();
        }
    }
}

/*cd "C:\Users\kunwa\Documents\TIET\Sem 5\Web APP Dev JAVA\Lab\avengers3d\avengers3d"

& "C:\Users\kunwa\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat" `
  --init-script "C:\Users\kunwa\.copilot\session-state\5386ac6f-4528-491f-9d84-940ef5ca6217\run-on-jdk21.init.gradle" `
  run */
// & ".\build\package\Avengers3D\Avengers3D.exe"