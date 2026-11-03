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
