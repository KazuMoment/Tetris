package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import mino.Block;
import mino.Mino;
import mino.Mino_Bar;
import mino.Mino_L1;
import mino.Mino_L2;
import mino.Mino_Square;
import mino.Mino_T;
import mino.Mino_Z1;
import mino.Mino_Z2;

public class PlayManager {

    // Main Play Area
    final int WIDTH = 360;
    final int HEIGHT = 600;
    public static int left_x;
    public static int right_x;
    public static int top_y;
    public static int bottom_y;

    // Mino
    Mino currentMino;
    final int MINO_START_X;
    final int MINO_START_Y;
    Mino nextMino;
    final int NEXTMINO_X;
    final int NEXTMINO_Y;
    public static ArrayList<Block> staticBlocks = new ArrayList<>();

    // Others
    public static int dropInterval = 60; // mino drops in every 60 frames/sec
    Font munro, hunDin;
    boolean gameOver;

    // Effect
    boolean effectCounterOn;
    int effectCounter;
    ArrayList<Integer> effectY = new ArrayList<>();

    // Score
    int level = 1;
    int lines;
    int score;

    public PlayManager(){
        
        // Main Play Area Frame
        left_x = (GamePanel.WIDTH/2) - (WIDTH/2); // 1280/2 - 360/2 = 460
        right_x = left_x + WIDTH;
        top_y = 50;
        bottom_y = top_y + HEIGHT;

        MINO_START_X = left_x + (WIDTH/2) - Block.SIZE;
        MINO_START_Y = top_y + Block.SIZE;

        NEXTMINO_X = right_x + 175;
        NEXTMINO_Y = top_y + 500; 

        // Set Starting Mino
        currentMino = pickMino();
        currentMino.setXY(MINO_START_X, MINO_START_Y);
        nextMino = pickMino();
        nextMino.setXY(NEXTMINO_X, NEXTMINO_Y);

        // Font
        try {
        InputStream is = getClass().getResourceAsStream("/font/munro.ttf");
        munro = Font.createFont(Font.TRUETYPE_FONT, is);
        is = getClass().getResourceAsStream("/font/HunDIN1451.ttf");
        hunDin = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Mino pickMino(){

        // Pick a random mino
        Mino mino = null;
        int i = new Random().nextInt(7);
        switch(i){
            case 0: mino = new Mino_L1(); break;
            case 1: mino = new Mino_L2(); break;
            case 2: mino = new Mino_Square(); break;
            case 3: mino = new Mino_Bar(); break;
            case 4: mino = new Mino_T(); break;
            case 5: mino = new Mino_Z1(); break;
            case 6: mino = new Mino_Z2(); break;
        }
        return mino;
    }

    public void resetGame(){

        for(int i = staticBlocks.size() - 1; i > - 1; i--){
            // Remove all blocks 
            staticBlocks.remove(i);
        }

        level = 1;
        lines = 0;
        score = 0;
        dropInterval = 60;
        gameOver = false;
        GamePanel.music.play(0, true);
    }
    
    public void update(){
        // Check if currentMino is active
        if (currentMino.active == false){

            // If mino is not active, put it in staticBlocks
            staticBlocks.add(currentMino.b[0]);
            staticBlocks.add(currentMino.b[1]);
            staticBlocks.add(currentMino.b[2]);
            staticBlocks.add(currentMino.b[3]);

            // Check if game is over
            if (currentMino.b[0].x == MINO_START_X && currentMino.b[0].y == MINO_START_Y){
                // Collided with a block and can't move and position same as nextMino's
                gameOver = true;
                GamePanel.music.stop();
                GamePanel.se.play(1, false);
            }

            currentMino.deactivating = false;

            // Replace currentMino with nextMino
            currentMino = nextMino;
            currentMino.setXY(MINO_START_X, MINO_START_Y);
            nextMino = pickMino();
            nextMino.setXY(NEXTMINO_X, NEXTMINO_Y);

            // When a mino becomes inactive, check if a line can be deleted
            checkDelete();

        }
        else{
            currentMino.update();
        }
        

    }

    private void checkDelete(){

        int x = left_x;
        int y = top_y;
        int blockCount = 0;
        int lineCount = 0;

        while(x < right_x && y < bottom_y){

            for (int i = 0; i < staticBlocks.size(); i++){
                if (staticBlocks.get(i).x == x && staticBlocks.get(i).y == y){
                    // Increase the count if there is a static block
                    blockCount++;
                }
            }

            x += Block.SIZE;

            if (x == right_x){

                // If row filled, delete line
                if (blockCount == 12){

                    effectCounterOn = true;
                    effectY.add(y);

                    for(int i = staticBlocks.size() - 1; i > - 1; i--){
                        // Remove all blocks in current y line
                        if(staticBlocks.get(i).y == y){
                            staticBlocks.remove(i);
                        }
                    }

                    lineCount++;
                    lines++;
                    
                    // Drop Speed
                    // If the line score hits a certain number, increase the drop speed
                    // 1 is fastest
                    if (lines % 10 == 0 && dropInterval > 1){
                        
                        level++;
                        if (dropInterval > 10){
                            dropInterval -= 10;
                        }
                        else{
                            dropInterval -= 1;
                        }
                    }

                    // Slide down blocks after deletion
                    for(int i = 0; i < staticBlocks.size(); i++){
                        // If block is above current y, move down by block size
                        if (staticBlocks.get(i).y < y){
                            staticBlocks.get(i).y += Block.SIZE;
                        }
                    }
                }

                blockCount = 0;
                x = left_x;
                y += Block.SIZE; 
            }
        }

        // Add Score
        if (lineCount > 0){
            GamePanel.se.play(4, false);
            int singleLineScore = 10 * level;
            score += singleLineScore * lineCount;
        } 

    }

    public void draw(Graphics2D g2){
        
        // Draw Main Play Area
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(left_x - 4, top_y - 4, WIDTH + 8, HEIGHT + 8);

        // Draw Next Mino Frame
        int x = right_x + 100;
        int y = bottom_y - 200;
        g2.drawRect(x, y, 200, 200);
        g2.setFont(new Font(hunDin.getName(), Font.PLAIN, 30));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString("NEXT", x + 60, y + 60);

        // Draw Score Frame
        g2.drawRect(x, top_y, 250, 300);
        x += 40;
        y = top_y + 90;
        g2.drawString("LEVEL: " + level, x, y); y += 70;
        g2.drawString("LINES: " + lines, x, y); y += 70;
        g2.drawString("SCORE: " + score, x, y); y += 70;


        // Draw currentMino
        if (currentMino != null){
            currentMino.draw(g2);
        }

        // Draw nextMino
        nextMino.draw(g2);

        // Draw staticBlocks
        for (int i = 0; i <  staticBlocks.size(); i++){
            staticBlocks.get(i).draw(g2);
        }

        // Draw Effect
        if (effectCounterOn){
            effectCounter++;

            g2.setColor(Color.red);
            for (int i = 0; i < effectY.size(); i++){
                g2.fillRect(left_x, effectY.get(i), WIDTH, Block.SIZE);
            }

            if (effectCounter == 10){
                effectCounterOn = false;
                effectCounter = 0;
                effectY.clear();
            }

        }

        // Draw Pause or Game Over
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(50f));
        if (gameOver){
            x = left_x + 25;
            y = top_y + 320;
            g2.drawString("GAME OVER", x, y);
            x = left_x - 120;
            y += 70;
            g2.drawString("Press UP or W to try again.", x, y);
        }
        else if (KeyHandler.pausePressed){
            x = left_x + 70;
            y = top_y + 320;
            g2.drawString("PAUSED", x, y);
        }

        // Draw Game title
        x = 120;
        y = top_y + 320;
        g2.setColor(Color.white);
        g2.setFont(new Font("munro", Font.BOLD, 60));
        g2.drawString("TETRIS", x, y);
        
    }

}

