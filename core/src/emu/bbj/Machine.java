package emu.bbj;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import emu.bbj.config.AppConfigItem.FileLocation;
import emu.bbj.cpu.Cpu6502;
import emu.bbj.io.Disk;
import emu.bbj.io.Joystick;
import emu.bbj.io.Joystick.JoystickType;
import emu.bbj.io.Keyboard;
import emu.bbj.io.Via;
import emu.bbj.memory.Memory;
import emu.bbj.memory.RamType;
import emu.bbj.snap.Snapshot;
import emu.bbj.video.Ula;

/**
 * Represents the BBC machine.
 * 
 * @author Lance Ewing
 */
public class Machine {

  // Machine components.
  private Memory memory;
  private Ula ula;
  private Via via;
  private Cpu6502 cpu;
  
  // Peripherals.
  private Keyboard keyboard;
  private Joystick joystick;
  private Disk microdisc;

  private boolean paused = true;
  private boolean lastWarpSpeed = false;
  
  private MachineType machineType;
  
  // These control what part of the generate pixel data is rendered to the screen. 
  private int screenLeft;
  private int screenRight;
  private int screenTop;
  private int screenBottom;
  private int screenWidth;
  private int screenHeight;
  
  /**
   * Constructor for Machine.
   */
  public Machine() {
  }
  
  /**
   * Initialises the machine. It will boot in to BASIC with the given RAM configuration.
   * 
   * @param ramType The RAM configuration to use.
   * @param machineType The type of BBC machine, i.e. PAL or NTSC.
   */
  public void init(RamType ramType, MachineType machineType) {
    init(null, null, machineType, ramType, null);
  }
  
  /**
   * Initialises the machine, and optionally loads the given program file (if provided).
   * 
   * @param programFile The internal path to the program file to automatically load and run.
   * @param programType The type of program data, e.g. TAPE, etc.
   * @param machineType The type of BCC machine, i.e. PAL or NTSC.
   * @param ramType The RAM configuration to use.
   * @param fileLocation The location of the file, e.g. internal, external, local, classpath, absolute.
   * 
   */
  public void init(String programFile, String programType, MachineType machineType, RamType ramType, FileLocation fileLocation) {
    byte[] programData = null;
    Snapshot snapshot = null;
    FileHandle fileHandle = null;
    
    this.machineType = machineType;
    
    // If we've been given the path to a program to load, we load the data prior to all
    // other initialisation.
    if ((programFile != null) && (programFile.length() > 0)) {
      try {
        fileLocation = (fileLocation != null? fileLocation : FileLocation.INTERNAL);
        switch (fileLocation) {
          case INTERNAL:
            fileHandle = Gdx.files.internal(programFile);
            break;
          case EXTERNAL:
            fileHandle = Gdx.files.external(programFile);
            break;
          case LOCAL:
            fileHandle = Gdx.files.local(programFile);
            break;
          case ABSOLUTE:
            fileHandle = Gdx.files.absolute(programFile);
            break;
          case CLASSPATH:
            fileHandle = Gdx.files.classpath(programFile);
            break;
        }
        if (fileHandle != null) {
          if (fileHandle.exists()){
            programData = fileHandle.readBytes();
          }
        }
      } catch (Exception e) {
        // Continue with default behaviour, which is to boot in to BASIC.
      }
    }
    
    // Create the microprocessor.
    cpu = new Cpu6502(snapshot);

    // Create the ULA chip and configure it as per the current TV type.
    ula = new Ula(machineType, snapshot);
     
    // Create the peripherals.
    keyboard = new Keyboard();
    joystick = new Joystick(keyboard, JoystickType.ARROW_KEYS);
    
    // Create the VIA chip.
    via = new Via(cpu, keyboard, snapshot);
    
    // Create Microdisc disk controller.
    microdisc = new Disk(cpu);
    
    // Now we create the memory, which will include mapping the ULA chip,
    // the VIA chips, and the creation of RAM chips and ROM chips.
    memory = new Memory(cpu, ula, via, microdisc, snapshot);
    
    // Set up the screen dimensions based on the ULA chip settings. Aspect ratio of 5:4.
    screenWidth = ((machineType.getVisibleScreenHeight() / 4) * 5);
    screenHeight = machineType.getVisibleScreenHeight();
    //screenWidth = machineType.getVisibleScreenWidth();
    //screenHeight = (machineType.getVisibleScreenWidth() / 5) * 4;
    screenLeft = machineType.getHorizontalOffset();
    screenRight = screenLeft + machineType.getVisibleScreenWidth();
    screenTop = machineType.getVerticalOffset();
    screenBottom = screenTop + machineType.getVisibleScreenHeight();

    // Check if the resource parameters have been set.
    if ((programData != null) && (programData.length > 0)) {
      if ("ROM".equals(programType)) {
        // Loads the ROM file over top of the default BASIC ROM.
        memory.loadCustomRom(programData);
        
      } else if ("DISK".equals(programType)) {
        // Insert the disk ready to be booted.
        microdisc.insertDisk(programFile, programData);
      }
    }
    
    // If the state of the machine was not loaded from a snapshot file, then we begin with a reset.
    if (snapshot == null) {
      cpu.reset();
    }
  }
  
  /**
   * Updates the state of the machine of the machine until a frame is complete
   * 
   * @param warpSpeed true If the machine is running at warp speed.
   */
  public void update(boolean warpSpeed) {
    boolean frameComplete = false;
    lastWarpSpeed = warpSpeed;
    do {
      frameComplete |= ula.emulateCycle();
      cpu.emulateCycle();
      via.emulateCycle();
      microdisc.emulateCycle();
    } while (!frameComplete);
  }
  
  /**
   * Gets whether the last frame was updated at warp speed, or not.
   * 
   * @return true if the last frame was updated at warp speed; otherwise false.
   */
  public boolean isLastWarpSpeed() {
    return lastWarpSpeed;
  }
  
  /**
   * @return the screenLeft
   */
  public int getScreenLeft() {
    return screenLeft;
  }

  /**
   * @return the screenRight
   */
  public int getScreenRight() {
    return screenRight;
  }

  /**
   * @return the screenTop
   */
  public int getScreenTop() {
    return screenTop;
  }

  /**
   * @return the screenBottom
   */
  public int getScreenBottom() {
    return screenBottom;
  }

  /**
   * @return the screenWidth
   */
  public int getScreenWidth() {
    return screenWidth;
  }

  /**
   * @return the screenHeight
   */
  public int getScreenHeight() {
    return screenHeight;
  }

  /**
   * Gets the pixels for the current frame from the ULA chip.
   * 
   * @return The pixels for the current frame. Returns null if there isn't one that is ready.
   */
  public short[] getFramePixels() {
    return ula.getFramePixels();
  }

  /**
   * Emulates a single machine cycle.
   * 
   * @return true If the ULA chip has indicated that a frame should be rendered.
   */
  public boolean emulateCycle() {
    boolean render = ula.emulateCycle();
    cpu.emulateCycle();
    via.emulateCycle();
    return render;
  }
  
  /**
   * Pauses and resumes the Machine.
   * 
   * @param paused true to pause the machine, false to resume.
   */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }
  
  /**
   * Returns whether the Machine is paused or not.
   * 
   * @return true if the machine is paused; otherwise false.
   */
  public boolean isPaused() {
    return paused;
  }
  
  /**
   * Invoked when the Machine is being terminated.
   */
  public void dispose() {

  }
  
  /**
   * Gets the MachineType of this Machine, i.e. either PAL or NTSC.
   * 
   * @return The MachineType of this Machine, i.e. either PAL or NTSC.
   */
  public MachineType getMachineType() {
    return machineType;
  }
  
  /**
   * Gets the Keyboard of this Machine.
   * 
   * @return The Keyboard of this Machine.
   */
  public Keyboard getKeyboard() {
    return keyboard;
  }
  
  /**
   * Gets the Joystick of this Machine.
   * 
   * @return The Joystick of this Machine.
   */
  public Joystick getJoystick() {
    return joystick;
  }

  /**
   * Gets the Cpu6502 of this Machine.
   * 
   * @return The Cpu6502 of this Machine.
   */
  public Cpu6502 getCpu() {
    return cpu;
  }
  
  public void printState() {
    cpu.displayCurrentInstruction();
    System.out.println(via.toString());
    System.out.println(ula.toString());
  }
}
