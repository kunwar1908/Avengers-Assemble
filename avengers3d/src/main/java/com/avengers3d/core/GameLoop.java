package com.avengers3d.core;

/**
 * Fixed-timestep update, variable-rate render ("Fix Your Timestep" pattern).
 * Physics always sees the same dt, so jumps/gravity are deterministic at any FPS.
 */
public final class GameLoop {
    public interface Game {
        void input();
        void update(float dt);
        void render(float alpha);   // alpha in [0,1): how far we are between two physics steps
        boolean shouldClose();
    }

    private static final double STEP = 1.0 / 60.0;
    private static final double MAX_FRAME = 0.25;   // avoids the "spiral of death" after a stall

    public void run(Game game) {
        long previous = System.nanoTime();
        double accumulator = 0;
        while (!game.shouldClose()) {
            long now = System.nanoTime();
            double frameTime = Math.min((now - previous) / 1_000_000_000.0, MAX_FRAME);
            previous = now;
            accumulator += frameTime;

            game.input();
            while (accumulator >= STEP) {
                game.update((float) STEP);
                accumulator -= STEP;
            }
            game.render((float) (accumulator / STEP)); // use alpha to interpolate render transforms
        }
    }
}
