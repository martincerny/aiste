/*
 * Copyright (C) 2013 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cuni.amis.aiste.simulations.covergame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;

/**
 * A class that is able to render CG maps to bitmap files.
 * @author Martin
 */
public class CGMapRenderer {
    private static final int TILE_SIZE = 20;
    private static final float STROKE_WIDTH = 2;
    private static final int COVER_WIDTH = 2;
    private static final int NAVPOINT_WIDTH = 2;
    
    private final Logger logger = Logger.getLogger(CGMapRenderer.class);

    private boolean drawNavPoints;
    private boolean drawSpawnLocs;

    public CGMapRenderer(boolean drawNavPoints, boolean drawSpawnLocs) {
        this.drawNavPoints = drawNavPoints;
        this.drawSpawnLocs = drawSpawnLocs;
    }
    
    
    
    protected void drawImpassableSquare(Graphics2D g, int xPixels, int yPixels){
        g.drawRect(xPixels, yPixels, TILE_SIZE, TILE_SIZE);
        g.fillRect(xPixels, yPixels, TILE_SIZE, TILE_SIZE);        
    }
    
    protected void drawImpassableCover(Graphics2D g, int xPixels, int yPixels){
       // g.drawRect(xPixels, yPixels, TILE_SIZE, TILE_SIZE);        
//        g.drawLine(xPixels, yPixels, xPixels + TILE_SIZE, yPixels + TILE_SIZE);
 //       g.drawLine(xPixels + TILE_SIZE, yPixels, xPixels, yPixels + TILE_SIZE);
       // g.fillRect(xPixels, yPixels, TILE_SIZE, TILE_SIZE);        
        drawImpassableSquare(g, xPixels, yPixels);
        Color oldColor = g.getColor();
        g.setColor(Color.WHITE);
        g.drawRect(xPixels + COVER_WIDTH, yPixels + COVER_WIDTH, TILE_SIZE - (2 * COVER_WIDTH), TILE_SIZE - (2 * COVER_WIDTH));
        g.setColor(oldColor);
    }

    protected void drawHorizontalAndVerticalCover(Graphics2D g, int xPixels, int yPixels){
        g.drawRect(xPixels, yPixels, TILE_SIZE, TILE_SIZE);
    }
    
    protected void drawHorizontalCover(Graphics2D g, int xPixels, int yPixels){
        g.drawRect(xPixels, yPixels + (TILE_SIZE / 2) - COVER_WIDTH, TILE_SIZE, COVER_WIDTH * 2);
    }

    protected void drawVerticalCover(Graphics2D g, int xPixels, int yPixels){
        g.drawRect(xPixels+ (TILE_SIZE / 2) - COVER_WIDTH, yPixels, COVER_WIDTH * 2, TILE_SIZE);
    }

    protected void drawSpawningLocation(Graphics2D g, int xPixels, int yPixels){
        String spawnString = "S";
        
        // get metrics from the graphics
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        // get the height of a line of text in this
        // font and render context
        int hgt = metrics.getHeight();
        // get the advance of my text in this font
        // and render context
        int adv = metrics.stringWidth(spawnString);
        g.drawString(spawnString, xPixels + (TILE_SIZE / 2) - (adv / 2), yPixels + (TILE_SIZE / 2) + (hgt / 2));
    }
    
    protected void drawNavPoint(Graphics2D g, int xPixels, int yPixels){
        g.drawOval(xPixels + (TILE_SIZE / 2) - (NAVPOINT_WIDTH / 2), yPixels  + (TILE_SIZE / 2) - (NAVPOINT_WIDTH / 2), NAVPOINT_WIDTH, NAVPOINT_WIDTH);
    }
    
    public void renderMap(CoverGame.StaticDefs defs, File output) throws IOException {
        BufferedImage img = new BufferedImage(defs.levelWidth * TILE_SIZE, defs.levelHeight * TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, defs.levelWidth * TILE_SIZE, defs.levelHeight * TILE_SIZE);
        
        g.setStroke(new BasicStroke(STROKE_WIDTH));
        g.drawRect(0, 0, defs.levelWidth * TILE_SIZE, defs.levelHeight * TILE_SIZE);
        
        for(int x = 0; x < defs.levelWidth; x++){
            for(int y = 0;y < defs.levelHeight; y++){
                final CGSquare square = defs.squares[x][y];
                if(!square.passable){
                    if(!square.horizontalCover){
                        if(square.verticalCover){
                            throw new IllegalArgumentException("Unsupported field type");
                        }
                        drawImpassableSquare(g, x * TILE_SIZE, y * TILE_SIZE);
                    } else {                        
                        if(!square.verticalCover){
                            throw new IllegalArgumentException("Unsupported field type");
                        }
                        drawImpassableCover(g, x * TILE_SIZE, y * TILE_SIZE);
                    }
                } else {
                    if(square.horizontalCover){
                        if(square.verticalCover){
                            drawHorizontalAndVerticalCover(g, x * TILE_SIZE, y * TILE_SIZE);
                        } else {
                            drawHorizontalCover(g, x * TILE_SIZE, y * TILE_SIZE);
                        }
                    } else {
                        if(square.verticalCover){
                            drawVerticalCover(g, x * TILE_SIZE, y * TILE_SIZE);
                        }
                    }
                }
                
                if(drawNavPoints && square.isNavPoint && (!drawSpawnLocs || !defs.playerSpawningLocations.contains(new Loc(x,y)))){
                    drawNavPoint(g, x * TILE_SIZE, y * TILE_SIZE);
                }
            }
        }

        if(drawSpawnLocs){
            for(Loc spawnLoc : defs.playerSpawningLocations){
                drawSpawningLocation(g, spawnLoc.x * TILE_SIZE, spawnLoc.y * TILE_SIZE);
            }
        }
        
        ImageIO.write(img, "png", output);
        logger.info("Map written to " + output.getAbsolutePath());
    }
}
