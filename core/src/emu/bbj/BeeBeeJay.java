package emu.bbj;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


import com.badlogic.gdx.Game;
import com.badlogic.gdx.Preferences;

import emu.bbj.config.AppConfigItem;
import emu.bbj.config.AppConfigItem.FileLocation;
import emu.bbj.memory.RamType;
import emu.bbj.ui.DialogHandler;

/**
 * The main entry point in to the cross-platform part of the BeeBeeJay emulator. A multi-screen
 * libGDX application needs to extend the Game class, which is what we do here. It allows 
 * us to have other screens, such as various menu screens.
 * 
 * @author Lance Ewing
 */
public class BeeBeeJay extends Game {
  
  /**
   * This is the screen that is used to show the running emulation.
   */
  private MachineScreen machineScreen;
  
  /**
   * This is the screen that shows the boot options and programs to load.
   */
  private HomeScreen homeScreen;
  
  /**
   * Invoked by BeeBeeJay whenever it would like to show a dialog, such as when it needs
   * the user to confirm an action, or to choose a file.
   */
  private DialogHandler dialogHandler;
  
  /**
   * Command line args. Mainly applicable to desktop.
   */
  private String[] args;
  
  /**
   * BeeBeeJay's saved preferences.
   */
  private Preferences preferences;
  
  /**
   * BeeBeeJay application screenshot storage.
   */
  private Preferences screenshotStore;
  
  /**
   * Constructor for BeeBeeJay.
   * 
   * @param dialogHandler
   * @param args Command line args.
   */
  public BeeBeeJay(DialogHandler dialogHandler, String... args) {
    this.dialogHandler = dialogHandler;
    this.args = args;
  }
  
  @Override
  public void create () {
    preferences = Gdx.app.getPreferences("beebeejay.preferences");
    screenshotStore = Gdx.app.getPreferences("beebeejay_screens.store");
    machineScreen = new MachineScreen(this, dialogHandler);
    homeScreen = new HomeScreen(this, dialogHandler);

    if ((args != null) && (args.length > 0)) {
      AppConfigItem appConfigItem = new AppConfigItem();
      appConfigItem.setFilePath(args[0]);
      if ((args[0].toLowerCase().endsWith(".dsk"))) {
        appConfigItem.setFileType("DISK");
      }
      if ((args[0].toLowerCase().endsWith(".tap"))) {
        appConfigItem.setFileType("TAPE");
      }
      appConfigItem.setMachineType(MachineType.PAL);
      appConfigItem.setRam(RamType.RAM_48K);
      appConfigItem.setFileLocation(FileLocation.ABSOLUTE);
      MachineScreen machineScreen = getMachineScreen();
      machineScreen.initMachine(appConfigItem);
      setScreen(machineScreen);
    } else {
      setScreen(homeScreen);
    }
    
    // Stop the back key from immediately exiting the app.
    Gdx.input.setCatchBackKey(true);
  }
  
  /**
   * Gets the MachineScreen.
   * 
   * @return The MachineScreen.
   */
  public MachineScreen getMachineScreen() {
    return machineScreen;
  }
  
  /**
   * Gets the HomeScreen.
   * 
   * @return the HomeScreen.
   */
  public HomeScreen getHomeScreen() {
    return homeScreen;
  }
  
  /**
   * Gets the Preferences for BeeBeeJay.
   * 
   * @return The Preferences for BeeBeeJay.
   */
  public Preferences getPreferences() {
    return preferences;
  }
  
  /**
   * Gets the screenshot store for BeeBeeJay. 
   * 
   * @return The screenshot store for BeeBeeJay.
   */
  public Preferences getScreenshotStore() {
    return screenshotStore;
  }
  
  @Override
  public void dispose () {
    super.dispose();
    
    // For now we'll dispose the MachineScreen here. As the emulator grows and
    // adds more screens, this may be managed in a different way. Note that the
    // super dispose does not call dispose on the screen.
    machineScreen.dispose();
    homeScreen.dispose();
    
    // Save the preferences when the emulator is closed.
    preferences.flush();
    screenshotStore.flush();
  }
}
