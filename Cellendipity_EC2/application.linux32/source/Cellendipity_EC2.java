import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Cellendipity_EC2 extends PApplet {

/*
* Cellendipity for Twitter Bot
* Tuned for export & run in Linux environment
* > No user interaction (mouse or GUI)
* > Run once and output an image file
* Randomise certain parameters each run
*/

Colony colony;      // A Colony object called 'colony'
Parameters p;       // A Parameters object called 'p'
String screendumpPath = "output.png";

public void setup() {
  colorMode(HSB, 360, 255, 255, 255);
  
  // fullScreen();
   // debug
  ellipseMode(RADIUS);
  p = new Parameters();
  colony = new Colony();
  if (p.greyscaleON) {background(p.bkgColGrey); } else {background(p.bkgColor);}
  if (p.debug) {frameRate(1);}
}

public void draw() {
  if (p.trailMode == 1 || p.debug) {background(p.bkgColor);}
  if (p.trailMode == 2) {trails();}
  colony.run();
  manageColony();
  //if (colony.cells.size() == 0) { if ((keyPressed == true) || p.autoRestart) {populateColony(); } } // Repopulate the colony when all the cells have died
}

public void populateColony() {
  if (p.greyscaleON) {background(p.bkgColGrey); } else {background(p.bkgColGrey);} // flush the background
  colony.cells.clear();
  colony = new Colony();
}

public void trails() {
  blendMode(SUBTRACT);
  noStroke();
  fill(1);
  rect(0,0,width,height);
  blendMode(BLEND);
  fill(255);
}

public void manageColony() {
  if (colony.cells.size() == 0 || frameCount > 3600) { //  If an extinction has occurred...
    if (p.screendumpON) {screendump();} //WARNING! ALWAYS repopulate & restart the colony after doing this once!
    exit();
    populateColony(); // .... repopulate the colony
  }
}

// We can add a creature manually if we so desire
public void mousePressed() {
  PVector mousePos = new PVector (mouseX, mouseY);
  PVector vel = PVector.random2D();
  DNA dna = new DNA();
  colony.spawn(mousePos, vel, dna);
}

public void mouseDragged() {
  PVector mousePos = new PVector (mouseX, mouseY);
  PVector vel = PVector.random2D();
  DNA dna = new DNA();
  colony.spawn(mousePos, vel, dna);
}

public void screendump() {
  saveFrame(screendumpPath);
}

public void keyReleased() {
  if (key == '1') {println("1"); p.trailMode = 1;}
  if (key == '2') {println("2"); p.trailMode = 2;}
  if (key == '3') {println("3"); p.trailMode = 3;}
  if (key == 'r') {println("r"); colony.cells.clear();}
  if (key == 'b') {println("b"); background(p.bkgColGrey); }
  if (key == 'd') {println("d"); p.debug = !p.debug; }
  if (key == 'n') {println("n"); p.nucleus = !p.nucleus; }
  if (key == 's') {println("s"); screendump();}
}
class Cell {

  //  Objects
  DNA dna;         // DNA

  // BOOLEAN
  boolean fertile;

  // GROWTH AND REPRODUCTION
  float age;       // Age (nr. of frames since birth)
  float lifespan;
  float fertility; // Condition for becoming fertile
  float maturity;
  float spawnCount;

  // SIZE AND SHAPE
  float cellStartSize;
  float cellEndSize;
  float r;         // Radius
  float flatness;  // To make flatter ellipses (1 = circle)
  float growth;    // Radius grows by this amount per frame
  float drawStep;  // To enable spacing of the drawn object (ellipse)
  float drawStepN;

  // MOVEMENT
  PVector position;
  PVector velocityLinear;
  PVector velocityNoise;
  PVector velocity;
  float noisePercent;
  float spiral;
  float vMax;   // multiplication factor for velocity
  float xoff;       // x offset
  float yoff;       // y offset
  float step;       // step size

  // FILL COLOUR
  int fillColor;   // For HSB you need Hue to be the heading of a PVector
  int spawnCol;      // spawnCol needs to be a GLOBAL variable
  float fill_H;         // Hue (HSB) / Red (RGB)
  float fill_S;         // Saturation (HSB) / Green (RGB)
  float fill_B;         // Brightness (HSB) / Blue (RGB)
  float fillAlpha;      // Transparency (HSB & RGB)

  // FILL COLOUR
  int strokeColor; // For HSB you need Hue to be the heading of a PVector
  float stroke_H;       // Hue (HSB) / Red (RGB)
  float stroke_S;       // Saturation (HSB) / Green (RGB)
  float stroke_B;       // Brightness (HSB) / Blue (RGB)
  float strokeAlpha;    // Transparency (HSB & RGB)

  // CONSTRUCTOR: create a 'cell' object
  Cell (PVector pos, PVector vel, DNA dna_) {
  // OBJECTS
  dna = dna_;

  // DNA gene mapping (17 genes)
  // 0 = fill Hue (0-360)
  // 1 = fill Saturation (0-255)
  // 2 = fill Brightness (0-255)
  // 3 = fill Alpha (0-255)
  // 4 = stroke Hue (0-360)
  // 5 = stroke Saturation (0-255)
  // 6 = stroke Brightness (0-255)
  // 7 = stroke Alpha (0-255)
  // 8 = cellStartSize (10-50) (cellendipity/one uses 0-200)
  // 9 = cellEndSize (5 - 20 %) (cellendipity/one uses 0-50)
  // 10 = lifespan (200-1000)
  // 11 = flatness (0.5 - 2) (or 50-200 %)
  // 12 = spiral screw (-75 to 75)
  // 13 = fertility (70-90%)
  // 14 = spawnCount (1-5)
  // 15 = vMax (Noise) (0-5) (cellendipity/one uses 0-4)
  // 16 = step (Noise) (0.005 - ?)  (cellendipity/one uses 0.001-0.006)
  // 17 = noisePercent (0-1) (0-100%)

  // BOOLEAN
  fertile = false; // A new cell always starts off infertile

  // GROWTH AND REPRODUCTION
  age = 0; // Age is 'number of frames since birth'. A new cell always starts with age = 0. From age comes maturity
  lifespan = dna.genes[10];
  fertility = dna.genes[13] * 0.01f; // How soon will the cell become fertile?
  maturity = map(age, 0, lifespan, 1, 0);
  spawnCount = dna.genes[14]; // Max. number of spawns

  // SIZE AND SHAPE
  cellStartSize = dna.genes[8];
  cellEndSize = cellStartSize * dna.genes[9] * 0.01f;
  r = cellStartSize; // Initial value for radius
  flatness = dna.genes[11] * 0.01f; // To make circles into ellipses. range 0.5 - 2.0

  growth = (cellStartSize-cellEndSize)/lifespan; // Should work for both large>small and small>large
  drawStep = 1;
  drawStepN = 1;

  // MOVEMENT
  position = pos.copy(); //cell has position
  velocityLinear = vel.copy(); //cell has unique basic velocity component
  noisePercent = dna.genes[17] * 0.01f; // How much influence on velocity does Perlin noise have?
  spiral = dna.genes[12] * 0.01f; // Spiral screw amount
  vMax = dna.genes[15]; // Maximum magnitude in velocity components generated by noise
  xoff = random(1000); //Seed for noise
  yoff = random(1000); //Seed for noise
  step = dna.genes[16] * 0.001f; //Step-size for noise

  // COLOUR

  fill_H = dna.genes[0];
  fill_S = dna.genes[1];
  fill_B = dna.genes[2];
  fillColor = color(fill_H, fill_S, fill_B); // Initial color is set
  fillAlpha = dna.genes[3];

  stroke_H = dna.genes[4];
  stroke_S = dna.genes[5];
  stroke_B = dna.genes[6];
  strokeColor = color(stroke_H, stroke_S, stroke_B); // Initial color is set
  strokeAlpha = dna.genes[7];
  }

  public void run() {
    live();
    updatePosition();
    updateSize();
    updateFertility();
    updateColour();
    if (p.wraparound) {checkBoundaryWraparound();}
    display();
    if (p.debug) {cellDebugger();}
  }

  public void live() {
    age ++;
    maturity = map(age, 0, lifespan, 1, 0);
    drawStep --;
    float drawStepStart = map(p.stepSize, 0, 100, 0 , (r *2 + growth));
    if (drawStep < 0) {drawStep = drawStepStart;}
    drawStepN--;
    float drawStepNStart = map(p.stepSizeN, 0, 100, 0 , r *2);
    if (drawStepN < 0) {drawStepN = drawStepNStart;}
  }

  public void updatePosition() { //Update parameters linked to the position
    float vx = map(noise(xoff),0,1,-vMax,vMax);
    float vy = map(noise(yoff),0,1,-vMax,vMax);
    velocityNoise = new PVector(vx,vy);
    xoff += step;
    yoff += step;
    velocity = PVector.lerp(velocityLinear, velocityNoise, noisePercent); //<>// //<>// //<>// //<>//
    float screwAngle = map(maturity, 0, 1, 0, spiral * TWO_PI);
    //if (dna.genes[11] >= 0.5) {screwAngle *= -1;} //IS THIS ACTUALLY NEEDED ANY MORE? (screwAngle is both +ve & -ve)
    velocity.rotate(screwAngle);
    position.add(velocity);
  }

  public void updateSize() {
    // r = ((sin(map(maturity, 1, 0, 0, PI)))+0)*cellStartSize;
    r -= growth;
  }

  public void updateFertility() {
    if (maturity <= fertility) {fertile = true; } else {fertile = false; }
    if (spawnCount == 0) {fertility = 0;} // Once spawnCount has counted down to zero, the cell will spawn no more
  }

  public void updateColour() {
    if (p.fill_STwist > 0) {fill_S = map(maturity, 1, 0, (255-p.fill_STwist), 255); fillColor = color(fill_H, fill_S, fill_B);} // Modulate fill saturation by radius
    if (p.fill_BTwist > 0) {fill_B = map(maturity, 0, 1, (255-p.fill_BTwist), 255); fillColor = color(fill_H, fill_S, fill_B);} // Modulate fill brightness by radius
    if (p.fill_ATwist > 0) {fillAlpha = map(maturity, 0, 1, (255-p.fill_ATwist), 255);} // Modulate fill Alpha by radius
    if (p.fill_HTwist > 0) { // Modulate fill hue by radius. Does not change original hue value but replaces it with a 'twisted' version
      float fill_Htwisted = map(maturity, 1, 0, fill_H, fill_H+p.fill_HTwist);
      if (fill_Htwisted > 360) {fill_Htwisted -= 360;}
      fillColor = color(fill_Htwisted, fill_S, fill_B); //fill colour is updated with new hue value
    }
    if (p.stroke_STwist > 0) {stroke_S = map(maturity, 1, 0, (255-p.stroke_STwist), 255); strokeColor = color(stroke_H, stroke_S, stroke_B);} // Modulate stroke saturation by radius
    if (p.stroke_BTwist > 0) {stroke_B = map(maturity, 0, 1, (255-p.stroke_BTwist), 255); strokeColor = color(stroke_H, stroke_S, stroke_B);} // Modulate stroke brightness by radius
    if (p.stroke_ATwist > 0) {strokeAlpha = map(maturity, 0, 1, (255-p.stroke_ATwist), 255);} // Modulate stroke Alpha by radius
    if (p.stroke_HTwist > 0) { // Modulate stroke hue by radius
      float stroke_Htwisted = map(maturity, 1, 0, stroke_H, stroke_H+p.stroke_HTwist);
      if (stroke_Htwisted > 360) {stroke_Htwisted -= 360;}
      strokeColor = color(stroke_Htwisted, stroke_S, stroke_B); //stroke colour is updated with new hue value
    }
  }

  public void checkBoundaryWraparound() {
    if (position.x > width + r * flatness) {
      position.x = -r * flatness;
    }
    else if (position.x < -r * flatness) {
      position.x = width + r * flatness;
    }
    else if (position.y > height + r * flatness) {
      position.y = -r * flatness;
    }
    else if (position.y < -r * flatness) {
      position.y = height + r * flatness;
    }
  }

  // Death
  public boolean dead() {
    if (age >= lifespan) {return true;} // Death by old age (regardless of size, which may remain constant)
    if (position.x > width + r * flatness || position.x < -r * flatness || position.y > height + r * flatness || position.y < -r * flatness) {return true;} // Death if move beyond canvas boundary
    else { return false; }
    //return false; // Use if no death
  }

  public void display() {
    if (p.strokeDisable) {noStroke();} else {stroke(hue(strokeColor), saturation(strokeColor), brightness(strokeColor), strokeAlpha);}
    if (p.fillDisable) {noFill();} else {fill(hue(fillColor), saturation(fillColor), brightness(fillColor), fillAlpha);}

    float angle = velocity.heading();
    pushMatrix();
    translate(position.x,position.y);
    rotate(angle);
    if (!p.stepped) {
      ellipse(0, 0, r, r * flatness);
      if (p.nucleus && drawStepN < 1) {
        if (fertile) {fill(0); ellipse(0, 0, cellEndSize, cellEndSize * flatness);}
        else {fill(255); ellipse(0, 0, cellEndSize, cellEndSize * flatness);}
      }
    }
    else if (drawStep < 1) { // stepped=true, step-counter is active for cell, draw only when counter=0
      ellipse(0, 0, r, r*flatness);
      if (p.nucleus && drawStepN < 1) { // Nucleus is always drawn when cell is drawn (no step-counter for nucleus)
        if (fertile) {
          fill(0); ellipse(0, 0, cellEndSize, cellEndSize * flatness);
        }
        else {
          fill(255); ellipse(0, 0, cellEndSize, cellEndSize * flatness);
        }
      }
    }
    popMatrix();
  }

  public void checkCollision(Cell other) {       // Method receives a Cell object 'other' to get the required info about the collidee
      PVector distVect = PVector.sub(other.position, position); // Static vector to get distance between the cell & other
      float distMag = distVect.mag();       // calculate magnitude of the vector separating the balls
      if (distMag < (r + other.r)) { conception(other, distVect);} // Spawn a new cell
  }

  public void conception(Cell other, PVector distVect) {
    // Decrease spawn counters.
    spawnCount --;
    other.spawnCount --;

    // Calculate position for spawn based on PVector between cell & other (leaving 'distVect' unchanged, as it is needed later)
    PVector spawnPos = distVect.copy();  // Create spawnPos as a copy of the (already available) distVect which points from parent cell to other
    spawnPos.normalize();
    spawnPos.mult(r);               // The spawn position is located at parent cell's radius
    spawnPos.add(position);

    // Calculate velocity vector for spawn as being centered between parent cell & other
    PVector spawnVel = velocity.copy(); // Create spawnVel as a copy of parent cell's velocity vector
    spawnVel.add(other.velocity);       // Add dad's velocity
    spawnVel.normalize();               // Normalize to leave just the direction and magnitude of 1 (will be multiplied later)

    // Combine the DNA of the parent cells
    DNA childDNA = dna.combine(other.dna);

    // Calculate new fill colour for child (a 50/50 blend of each parent cells)
    int childFillColor = lerpColor(fillColor, other.fillColor, 0.5f);

    // Calculate new stroke colour for child (a 50/50 blend of each parent cells)
    int childStrokeColor = lerpColor(strokeColor, other.strokeColor, 0.5f);

    // Genes for color require special treatment as I want childColor to be a 50/50 blend of parents colors
    // I will therefore overwrite color genes with reverse-engineered values after lerping:
    childDNA.genes[0] = hue(childFillColor); // Get the  lerped hue value and map it back to gene-range
    childDNA.genes[1] = saturation(childFillColor); // Get the  lerped hue value and map it back to gene-range
    childDNA.genes[2] = brightness(childFillColor); // Get the  lerped hue value and map it back to gene-range
    childDNA.genes[4] = hue(childStrokeColor); // Get the  lerped hue value and map it back to gene-range
    childDNA.genes[5] = saturation(childStrokeColor); // Get the  lerped hue value and map it back to gene-range
    childDNA.genes[6] = brightness(childStrokeColor); // Get the  lerped hue value and map it back to gene-range

    //childDNA.mutate(0.01); // Child DNA can mutate. HACKED! Mutation is temporarily disabled!

    // Call spawn method (in Colony) with the new parameters for position, velocity, colour & starting radius)
    // Note: Currently no combining of parent DNA
    colony.spawn(spawnPos, spawnVel, childDNA);


    //Reduce fertility for parent cells by squaring them
    fertility *= fertility;
    fertile = false;
    other.fertility *= other.fertility;
    other.fertile = false;
  }

  public void cellDebugger() { // For debug only
  int rowHeight = 15;
  fill(360, 255);
  textSize(rowHeight);
  text("r:" + r, position.x, position.y + rowHeight * 0);
  text("cellStartSize:" + cellStartSize, position.x, position.y + rowHeight * 1);
  text("cellEndSize:" + cellEndSize, position.x, position.y + rowHeight * 2);
  //text("fill_HR:" + fill_HR, position.x, position.y + rowHeight * 0);
  //text("rMax:" + rMax, position.x, position.y + rowHeight * 0);
  text("growth:" + growth, position.x, position.y + rowHeight * 3);
  //text("age:" + age, position.x, position.y + rowHeight * 0);
  text("maturity:" + maturity, position.x, position.y + rowHeight * 4);
  //text("fertile:" + fertile, position.x, position.y + rowHeight * 0);
  //text("fertility:" + fertility, position.x, position.y + rowHeight * 1);
  //text("spawnCount:" + spawnCount, position.x, position.y + rowHeight * 2);
  //text("x-velocity:" + velocity.x, position.x, position.y + rowHeight * 0);
  //text("y-velocity:" + velocity.y, position.x, position.y + rowHeight * 0);
  //text("velocity heading:" + velocity.heading(), position.x, position.y + rowHeight * 0);
     }



}
class Colony {

  // VARIABLES
  ArrayList<Cell> cells;    // An arraylist for all the cells //<>// //<>// //<>// //<>// //<>//
  int colonyMaxSize = 100;
  float x,y;

  // CONSTRUCTOR: Create a 'Colony' object, initially populated with 'num' cells
  Colony() {
    cells = new ArrayList<Cell>();

    for (int i = 0; i < p.numStrains; i++) {
      DNA dna = new DNA();
      //po = new PVector(x,y);
      //PVector po = new PVector(random(width), random(height));
      for (int j = 0; j < p.strainSize; j++) {
        PVector v = PVector.random2D();   // Initial velocity vector is random
        PVector po = new PVector(random(width), random(height));
        cells.add(new Cell(po, v, dna)); // Add new Cell with DNA
      }
    }
  }

// Spawn a new cell (called by e.g. MousePressed in main, accepting mouse coords for start position)
  public void spawn(PVector mousePos, PVector vel, DNA dna_) {
    cells.add(new Cell(mousePos, vel, dna_));
  }

// Run the colony
  public void run() {
    if (p.debug) {colonyDebugger(); }
    for (int i = cells.size()-1; i >= 0; i--) {  // Iterate backwards through the ArrayList because we are removing items
      Cell c = cells.get(i);                     // Get one cell at a time
      c.run();                                   // Run the cell (grow, move, spawn, check position vs boundaries etc.)
      if (c.dead()) {cells.remove(i);}           // If the cell has died, remove it from the array

      // Iteration to check collision between current cell(i) and the rest
      if (cells.size() <= colonyMaxSize && c.fertile) {             // Don't check for collisons if there are too many cells (wait until some die off)
        for (int others = i-1; others >= 0; others--) {         // Since main iteration (i) goes backwards, this one needs to too
          Cell other = cells.get(others);                       // Get the other cells, one by one
          if (other.fertile) { c.checkCollision(other); }       // Only check for collisions when both cells are fertile
        }
      }
    }
    // If there are too many cells, remove some by 'culling'
   if (cells.size() > colonyMaxSize) { cull(colonyMaxSize); }
  }

  public void cull(int div)  {  // To remove a proportion of the cells from (the oldest part of) the colony
    int cull = (cells.size()/div);
    for (int i = cull; i >= 0; i--) {cells.remove(i); }
  }

  public void colonyDebugger() {  // Displays some values as text at the top left corner (for debug only)
    noStroke();
    fill(0);
    rect(0,0,230,40);
    fill(360);
    textSize(16);
    text("frames" + frameCount + " Nr. cells: " + cells.size() + " MaxLimit:" + colonyMaxSize, 10, 18);
    text("TrailMode: " + p.trailMode + " Debug:" + p.debug, 10, 36);
  }
}
// Class to describe DNA
// Borrowed from 'Evolution EcoSystem'
// by Daniel Shiffman <http://www.shiffman.net>


class DNA {

  float[] genes;  // 'genes' is an array of float values in the range (0-1)

  // Constructor (makes a random DNA)
  DNA() {
      genes = new float[18];  // DNA contains an array called 'genes' with [12] float values

      // DNA gene mapping (18 genes)
      // 0 = fill Hue (0-360)
      // 1 = fill Saturation (0-255)
      // 2 = fill Brightness (0-255)
      // 3 = fill Alpha (0-255)
      // 4 = stroke Hue (0-360)
      // 5 = stroke Saturation (0-255)
      // 6 = stroke Brightness (0-255)
      // 7 = stroke Alpha (0-255)
      // 8 = cellStartSize (10-50) (cellendipity/one uses 0-200)
      // 9 = cellEndSize (5 - 20 %) (cellendipity/one uses 0-50)
      // 10 = lifespan (200-1000)
      // 11 = flatness (50-200 %)
      // 12 = spiral screw (-75 - +75 %)
      // 13 = fertility (70-90%)
      // 14 = spawnCount (1-5)
      // 15 = vMax (Noise) (0-5) (cellendipity/one uses 0-4)
      // 16 = step (Noise) (1 - 6 * 0.001?)  (cellendipity/one uses 0.001-0.006)
      // 17 = noisePercent (0-100%)

// FIXED VALUES
      // genes[0] = 0;    // 0 = fill Hue (0-360)
      // genes[1] = 0;    // 1 = fill Saturation (0-255)
      // genes[2] = 0;    // 2 = fill Brightness (0-255)
      // genes[3] = 100;  // 3 = fill Alpha (0-255)
      // genes[4] = 120;  // 4 = stroke Hue (0-360)
      // genes[5] = 255;  // 5 = stroke Saturation (0-255)
      // genes[6] = 255;  // 6 = stroke Brightness (0-255)
      genes[7] = 18;   // 7 = stroke Alpha (0-255)
      // genes[8] = 25;   // 8 = cellStartSize (10-50) (cellendipity/one uses 0-200)
      // genes[9] = 10;   // 9 = cellEndSize (5 - 20 %) (cellendipity/one uses 0-50)
      // genes[10] = 500; // 10 = lifespan (200-1000)
      genes[11] = 100; // 11 = flatness (50-200 %)
      // genes[12] = -30; // 12 = spiral screw (-75 - +75 %)
      genes[13] = 75;  // 13 = fertility (70-90%)
      genes[14] = 1;   // 14 = spawnCount (1-5)
      // genes[15] = 4;   // 15 = vMax (Noise) (0-5) (cellendipity/one uses 0-4)
      // genes[16] = 5;   // 16 = step (Noise) (1 - 6 * 0.001?)  (cellendipity/one uses 0.001-0.006)
      // genes[17] = 50;  // 17 = noisePercent (0-100%)

      // RANDOMIZED VALUES
            genes[0] = random(360);        // 0 = fill Hue (0-360)
            genes[1] = random(255);        // 1 = fill Saturation (0-255)
            genes[2] = random(255);        // 2 = fill Brightness (0-255)
            genes[3] = random(255);        // 3 = fill Alpha (0-255)
            genes[4] = random(360);        // 4 = stroke Hue (0-360)
            genes[5] = random(255);        // 5 = stroke Saturation (0-255)
            genes[6] = random(255);        // 6 = stroke Brightness (0-255)
            //genes[7] = random(255);        // 7 = stroke Alpha (0-255)
            genes[8] = random(10, 100);    // 8 = cellStartSize (10-50) (cellendipity/one uses 0-200)
            genes[9] = random(5, 20);      // 9 = cellEndSize (5 - 20 %) (cellendipity/one uses 0-50)
            genes[10] = random(200, 1000); // 10 = lifespan (200-1000)
            //genes[11] = random(50, 200);   // 11 = flatness (50-200 %)
            genes[12] = random(-75, 75);   // 12 = spiral screw (-75 - +75 %)
            //genes[13] = random(70, 90);    // 13 = fertility (70-90%)
            //genes[14] = random(1, 5);      // 14 = spawnCount (1-5)
            genes[15] = random(0, 4);      // 15 = vMax (Noise) (0-5) (cellendipity/one uses 0-4)
            genes[16] = random(1, 6);      // 16 = step (Noise) (1 - 6 * 0.001?)  (cellendipity/one uses 0.001-0.006)
            genes[17] = random(100);       // 17 = noisePercent (0-100%)

    }

  DNA(float[] newgenes) {
    genes = newgenes;
  }

  public DNA combine(DNA otherDNA_) { // Returns a new set of DNA consisting of randomly selected genes from both parents
    float[] newgenes = new float[genes.length];
    for (int i = 0; i < newgenes.length; i++) {
      if (random(1) < 0.5f) {newgenes[i] = genes[i];}
      else {newgenes[i] = otherDNA_.genes[i];} // 50/50 chance of copying gene from either 'mother' or 'other'
    }
    return new DNA(newgenes);
  }

  public void geneMutate(float m) {
    // Using the received mutation probability 'm', picks new, fully random values in array spots
    // This method is called from the 'reproduce' method in Cell
    for (int i = 0; i < genes.length; i++) {
      if (random(1) < m) { genes[i] = random(0,1); }
    }
  }
} // End of DNA class
class Parameters {
  boolean debug;
  boolean centerSpawn;
  boolean autoRestart;
  boolean screendumpON;
  boolean veilDrawON;
  boolean veilRepopulateON;
  boolean fillDisable;
  boolean strokeDisable;
  boolean greyscaleON;
  boolean nucleus;
  boolean stepped;
  boolean wraparound;

  int strainSize;
  int numStrains;
  int stepSize;
  int stepSizeN;
  int trailMode;

  int bkgColGrey;
  int bkgColor;

  int fill_HTwist;
  int fill_STwist;
  int fill_BTwist;
  int fill_ATwist;

  int stroke_HTwist;
  int stroke_STwist;
  int stroke_BTwist;
  int stroke_ATwist;

  Parameters() {
    debug = false;
    centerSpawn = false; // true=initial spawn is width/2, height/2 false=random
    autoRestart = false; // If true, will not wait for keypress before starting anew
    screendumpON = true;
    fillDisable = false;
    strokeDisable = false;
    greyscaleON = false;
    nucleus = false;
    stepped = true;
    wraparound = false;

    strainSize = PApplet.parseInt(random(3,20)); // Number of cells in a strain
    numStrains = PApplet.parseInt(random(1,3)); // Number of strains (a group of cells sharing the same DNA)
    stepSize = 0;
    stepSizeN = 50;
    trailMode = 3; // 1=none, 2 = blend, 3 = continuous

    bkgColor = color(random(360), random(255), random(255)); // Background colour
    bkgColGrey = 128;

    fill_HTwist = 18;
    fill_STwist = 0;
    fill_BTwist = 18;
    fill_ATwist = 0;

    stroke_HTwist = 0;
    stroke_STwist = 0;
    stroke_BTwist = 0;
    stroke_ATwist = 0;
  }
}
  public void settings() {  size(1024, 1024);  smooth(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Cellendipity_EC2" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
