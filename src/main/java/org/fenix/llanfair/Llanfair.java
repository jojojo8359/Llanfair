package org.fenix.llanfair;

import org.fenix.llanfair.config.Settings;
import org.fenix.llanfair.gui.RunPane;
import org.fenix.utils.Resources;
import org.fenix.utils.UserSettings;
import org.fenix.utils.gui.BorderlessFrame;
import org.fenix.utils.locale.LocaleDelegate;
import org.fenix.utils.locale.LocaleEvent;
import org.fenix.utils.locale.LocaleListener;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main frame executing Llanfair.
 *
 * @author Xavier "Xunkar" Sencert
 * @version 1.5
 */
public class Llanfair extends BorderlessFrame implements TableModelListener, 
		LocaleListener, MouseWheelListener, ActionListener, NativeKeyListener,
		PropertyChangeListener, WindowListener, ComponentListener {

	private static Resources RESOURCES = null;


	static {
		ToolTipManager.sharedInstance().setInitialDelay( 1000 );
		ToolTipManager.sharedInstance().setDismissDelay( 7000 );
		ToolTipManager.sharedInstance().setReshowDelay( 0 );
	}

	// this class only exists so that the "on app quit" logic is somewhere that can be
	// added to the JVM's shutdown hook for Mac OS for when the user uses Cmd+Q to quit
	public class AppShutdown implements Runnable {
		private boolean hasRun = false;

		public void run()
		{
			if (hasRun)
				return;

			Settings.save();
			try {
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e) {

			}

			hasRun = true;
		}
	}

	private Runnable onAppShutdown = new AppShutdown();

	private Run run;
	private RunPane runPane;

	private Actions actions;

	private JPopupMenu popupMenu;

	private volatile boolean lockedHotkeys;
	private volatile boolean serverStarted;
	private volatile boolean ignoreNativeInputs;

	private Dimension preferredSize;

	/**
	 * Creates and initializes the application. As with any Swing application
	 * this constructor should be called from within a thread to avoid
	 * dead-lock.
	 */
	private Llanfair() {
		super( "Llanfair" );

		UserSettings.initDirectory();

		//LocaleDelegate.setDefault( Settings.language.get() );
		LocaleDelegate.setDefault( Locale.ENGLISH );
		LocaleDelegate.addLocaleListener( this );

		RESOURCES = new Resources();
		registerFonts();
		setResizable(Settings.windowUserResizable.get());
		setLookAndFeel();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addComponentListener(this);

		// if a mac user uses Cmd+Q to exit the application that quits the program in a way that doesn't fire
		// the window closed event for some fucked up reason. wow
		// the only "fancy" ways to deal with this problem that i could find are only relevant on Apple's Java 1.6
		// runtime which everyone should avoid nowadays. so we'll do this instead...
		if (System.getProperty("os.name").startsWith("Mac OS")) {
			Runtime.getRuntime().addShutdownHook(new Thread(onAppShutdown));
		}

		run = new Run();
		runPane = null;
		lockedHotkeys = false;
		serverStarted = false;
		ignoreNativeInputs = false;
		preferredSize = null;
		actions = new Actions( this );

		setMenu();

		boolean behaviourSucceed = setBehavior();
		if (!behaviourSucceed)
			return;

		setRun( run );

		setVisible( true );
	}

	/**
	 * Main entry point of the application. This is the method called by Java
	 * when a user executes the JAR. Simply instantiantes a new Llanfair object.
	 * If an argument is passed, the program will not launch but will instead
	 * enter localization mode, dumping all language variables for the specified
	 * locale.
	 *
	 * @param args array of command line parameters supplied at launch
	 */
	public static void main( String[] args ) {
		// latest version of JNativeHook is a bit noisy logging-wise by default
		Logger jnativehookLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		jnativehookLogger.setLevel(Level.WARNING);
		jnativehookLogger.setUseParentHandlers(false);

		/*
		if ( args.length > 0 ) {
			String locale = args[0];
			LocaleDelegate.setDefault( new Locale( locale ) );
			RESOURCES = new Resources();
			dumpLocalization();
			System.exit( 0 );
		}
		*/
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				new Llanfair();
			}
		} );
	}

	/**
	 * Grabs the resources of Llanfair. The Resources object is a front-end
	 * for every classpath resources associated with the application, including
	 * localization strings, icons, and properties.
	 *
	 * @return the resources object
	 */
	public static Resources getResources() {
		return RESOURCES;
	}

	/**
	 * Returns the run currently associated with this instance of Llanfair.
	 * While the run can be empty, it cannot be {@code null}.
	 *
	 * @return the current run
	 */
	Run getRun() {
		return run;
	}

	/**
	 * Sets the run to represent in this application to the given run. If the
	 * GUI does not exist (in other words, we are registering the first run) it
	 * is created on the fly.
	 *
	 * @param run the run to represent, cannot be {@code null}
	 */
	public final void setRun( Run run ) {
		if ( run == null ) {
			throw new NullPointerException( "Null run" );
		}
		this.run = run;
		// If we have a GUI, set the new model; else, create the GUI
		if ( runPane != null ) {
			runPane.setRun( run );
		} else {
			runPane = new RunPane( run );
			add( runPane );
		}
		Settings.setRun( run );
		run.addTableModelListener( this );
		run.addPropertyChangeListener( this );
		MenuItem.setActiveState( run.getState() );

		setPreferredSize( preferredSize );
		pack();

		// Replace the window to the run preferred location; center if none
		Point location = Settings.coordinates.get();
		if ( location == null ) {
			setLocationRelativeTo( null );
		} else {
			setLocation( location );
		}
	}

	public synchronized boolean isServerStarted() {return serverStarted;}

	public synchronized void setServerStarted(boolean serverStarted) {this.serverStarted = serverStarted;}

	public synchronized boolean areHotkeysLocked() {
		return lockedHotkeys;
	}

	public synchronized  void setLockedHotkeys(boolean lockedHotkeys) {
		this.lockedHotkeys = lockedHotkeys;
	}

	/**
	 * Indicates whether or not Llanfair currently ignores all native inputs.
	 * Since native inputs can be caught even when the application does not have
	 * the focus, it is necessary to be able to lock the application when the
	 * user needs to do something else whilst not interfering with the behavior
	 * of Llanfair.
	 *
	 * @return {@code true} if the current instance ignores native inputs
	 */
	public synchronized boolean ignoresNativeInputs() {
		return ignoreNativeInputs;
	}

	/**
	 * Tells Llanfair whether to ignore native input events or not. Since native
	 * inputs can be caught even when the application does not have the focus,
	 * it is necessary to be able to lock the application when the user needs
	 * to do something else whilst not interfering with the behavior of
	 * Llanfair.
	 *
	 * @param ignore if Llanfair must ignore the native inputs or not
	 */
	public synchronized void setIgnoreNativeInputs( boolean ignore ) {
		ignoreNativeInputs = ignore;
	}

	/**
	 * Outputs the given error in a dialog box. Only errors that are made for
	 * and useful to the user need to be displayed that way.
	 *
	 * @param message the localized error message
	 */
	public void showError( String message ) {
		showError(message, null);
	}

	public void showError(String message, Throwable ex) {
		String errorMessage;
		if (ex != null)
			errorMessage = message + "\n\n" + ex.toString();
		else
			errorMessage = message;

		JOptionPane pane = new JOptionPane(errorMessage, JOptionPane.ERROR_MESSAGE);
		JDialog dialog = pane.createDialog(Language.ERROR.get());
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
		dialog.dispose();
	}

	/**
	 * Sets the look and feel of the application. Provides a task bar icon and
	 * a general system dependent theme.
	 */
	private void setLookAndFeel() {
		setIconImage( RESOURCES.getImage( "Llanfair.png" ) );
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName()
			);
		} catch ( Exception ex ) {
			// $FALL-THROUGH$
		}
	}

	/**
	 * Register the fonts provided with Llanfair with its environment.
	 */
	private void registerFonts() {
		InputStream fontFile = RESOURCES.getStream( "digitalism.ttf" );
		try {
			Font digitalism = Font.createFont( Font.TRUETYPE_FONT, fontFile );
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
					digitalism
			);
		} catch ( Exception ex ) {
			// $FALL-THROUGH$
		}
	}

	/**
	 * Writes all values from the {@code Language} enum in a property file.
	 * This method will append all the newly defined entries to the list of
	 * already existing values.
	 */
	private static void dumpLocalization() {
		try {
			String iso = Locale.getDefault().getLanguage();
			FileWriter fw = new FileWriter( "language_" + iso + ".properties" );
			for ( Language lang : Language.values() ) {
				String old = RESOURCES.getString( lang.name() );
				fw.write( lang.name() + " = " );
				if ( old != null ) {
					fw.write( old );
				}
				fw.write( "\n" );
			}
			fw.close();
		} catch ( IOException ex ) {
			// $FALL-THROUGH$
		}
	}

	/**
	 * When the locale changes, we first ask the resources to reload the locale
	 * dependent resources and pass the event to the GUI.
	 */
	@Override public void localeChanged( LocaleEvent event ) {
		RESOURCES.defaultLocaleChanged();
		if ( runPane != null ) {
			runPane.processLocaleEvent( event );
		}
		MenuItem.localeChanged( event );
	}

	/**
	 * If we do not ignore the native inputs, register the input and invokes
	 * a new thread to treat the input whenever possible without hogging the
	 * main thread.
	 */
	@Override public void nativeKeyPressed( final NativeKeyEvent event ) {
		if (Settings.useGlobalHotkeys.get() || this.isFocused()) {
			int keyCode = event.getKeyCode();
			boolean hotkeysEnabler = ( keyCode == Settings.hotkeyLock.get() );

			if ( (!areHotkeysLocked() && !ignoresNativeInputs()) || hotkeysEnabler ) {
				SwingUtilities.invokeLater( new Runnable() {
					@Override public void run() {
						actions.process( event );
					}
				} );
			}
		}
	}

	@Override public void nativeKeyReleased( NativeKeyEvent event ) {}

	@Override public void nativeKeyTyped( NativeKeyEvent event ) {}

	/**
	 * A property change event might be fired from either the settings
	 * singleton or the run itself. In either case, we propagate the event to
	 * our children and update ourself with the new value of the given property.
	 */
	@Override public void propertyChange( PropertyChangeEvent event ) {
		runPane.processPropertyChangeEvent( event );
		String property = event.getPropertyName();

		if ( Run.STATE_PROPERTY.equals( property ) ) {
			MenuItem.setActiveState(run.getState());
			//forceInternalComponentsResize();
		} else if (Run.NAME_PROPERTY.equals(property)) {
			forceInternalComponentsResize();
		} else if (Run.SUBTITLE_PROPERTY.equals(property)) {
			forceInternalComponentsResize();
		} else if (Settings.headerShowAttempts.equals(property)) {
			forceInternalComponentsResize();
		} else if ( Settings.alwaysOnTop.equals( property ) ) {
			setAlwaysOnTop( Settings.alwaysOnTop.get() );
		} else if (Settings.historyRowCount.equals(property)
				|| Settings.graphDisplay.equals(property)
				|| Settings.footerDisplay.equals(property)
				|| Settings.footerUseSplitData.equals(property)
				|| Settings.coreIconSize.equals(property)
				|| Settings.accuracy.equals(property)
				|| Settings.headerShowSubtitle.equals(property)
				|| Settings.headerShowTitle.equals(property)
				|| Settings.historyDeltas.equals(property)
				|| Settings.historySegmentFont.equals(property)
				|| Settings.historyTimeFont.equals(property)
				|| Settings.historyLiveTimes.equals(property)
				|| Settings.historyMerge.equals(property)
				|| Settings.historyBlankRows.equals(property)
				|| Settings.historyIcons.equals(property)
				|| Settings.historyIconSize.equals(property)
				|| Settings.historyMultiline.equals(property)
				|| Settings.coreShowSegmentName.equals(property)
				|| Settings.coreShowSplitTime.equals(property)
				|| Settings.coreShowSegmentTime.equals(property)
				|| Settings.coreShowBestTime.equals(property)
				|| Settings.coreShowIcons.equals(property)
				|| Settings.coreTimerFont.equals(property)
				|| Settings.coreSegmentTimerFont.equals(property)
				|| Settings.coreShowSegmentTimer.equals(property)
				|| Settings.footerShowBestTime.equals(property)
				|| Settings.footerShowDeltaLabels.equals(property)
				|| Settings.footerVerbose.equals(property)
				|| Settings.footerMultiline.equals(property)
		        || Settings.footerShowSumOfBest.equals(property)
				|| Settings.windowUserResizable.equals(property)
				|| Settings.windowWidth.equals(property)
				|| Run.NAME_PROPERTY.equals(property)) {
			setResizable(Settings.windowUserResizable.get());
			MenuItem.enableResizeOptions(Settings.windowUserResizable.get());
			forceResize();
		}
	}

	/**
	 * When the run's table of segments is updated, we ask the main panel to
	 * update itself accordingly and repack the frame as its dimensions may
	 * have changed.
	 */
	@Override public void tableChanged( TableModelEvent event ) {
		runPane.processTableModelEvent( event );
		// No need to recompute the size if we receive a HEADER_ROW UPDATE
		// as we only use them when a segment is moved up or down and when
		// the user cancel any changes made to his run.
		if (    event.getType() == TableModelEvent.UPDATE
			 && event.getFirstRow() == TableModelEvent.HEADER_ROW ) {
			setPreferredSize( preferredSize );
		} else {
			setPreferredSize( null );
		}
		pack();
	}

	/**
	 * When the user scrolls the mouse wheel, we update the graph's scale to
	 * zoom in or out depending on the direction of the scroll.
	 */
	@Override public void mouseWheelMoved( MouseWheelEvent event ) {
		int rotations = event.getWheelRotation();
		float percent = Settings.graphScale.get();
		if ( percent == 0.5F ) {
			percent = 1.0F;
			rotations--;
		}
		float newValue = Math.max( 0.5F, percent + rotations );
		Settings.graphScale.set( newValue );
	}

	/**
	 * When the user clicks on the mouse's right-button, we bring up the
	 * context menu at the click's location.
	 */
	@Override public void mousePressed( MouseEvent event ) {
		super.mousePressed( event );
		if ( SwingUtilities.isRightMouseButton( event ) ) {
			popupMenu.show( this, event.getX(), event.getY() );
		}
	}

	/**
	 * Whenever the frame is being disposed of, we save the settings and
	 * unregister the native hook of {@code JNativeHook}.
	 */
	@Override public void windowClosed( WindowEvent event ) {
		onAppShutdown.run();
	}

	@Override public void windowClosing(WindowEvent event) {}

	@Override public void windowOpened(WindowEvent event) {}

	@Override public void windowActivated(WindowEvent event) {}

	@Override public void windowDeactivated(WindowEvent event) {}

	@Override public void windowIconified(WindowEvent event) {}

	@Override public void windowDeiconified(WindowEvent event) {}

	@Override
	public void componentResized(ComponentEvent e) {
		// we may have to override the width, but allow the height to grow as needed
		// don't have to change anything if autosizing
		if (!Settings.windowUserResizable.get())
			setSize(new Dimension(Settings.windowWidth.get(), getHeight()));
	}

	@Override public void componentMoved(ComponentEvent e) {}

	@Override public void componentShown(ComponentEvent e) {}

	@Override public void componentHidden(ComponentEvent e) {}

	/**
	 * Action events are fired by clicking on the entries of the context menu.
	 */
	@Override public synchronized void actionPerformed( final ActionEvent ev ) {
		MenuItem  source = ( MenuItem ) ev.getSource();

		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				actions.process( ev );
			}
		} );

		if (source.equals(MenuItem.EDIT)) {
		/*
		} else if (source.equals(MenuItem.RESIZE_DEFAULT)) {
			setPreferredSize(null);
			pack();

		} else if (source.equals(MenuItem.RESIZE_PREFERRED)) {
			setPreferredSize(preferredSize);
			pack();
		*/
		}
	}

	/**
	 * Sets the persistent behavior of the application and its components.
	 *
	 * @throws  IllegalStateException if JNativeHook cannot be registered.
	 */
	private boolean setBehavior() {
		boolean registered = registerNativeKeyHook();

		if (!registered) {
			// NOTE: in the event of a failure, JNativeHook now has some ability (on some OS's at least)
			//       to pop up an OS-specific dialog or other action that allows the user to rectify the
			//       problem. e.g. on OS X, if an exception is thrown a dialog telling the user that the
			//       application has requested some accessibility-related access shows up.

			showError(Language.GLOBAL_HOTKEYS_STARTUP_ERROR.get());
			this.dispose();
			return false;
		}

		setAlwaysOnTop(Settings.alwaysOnTop.get());
		addWindowListener(this);
		addMouseWheelListener(this);
		Settings.addPropertyChangeListener(this);
		GlobalScreen.addNativeKeyListener(this);

		return true;
	}

	/**
	 * Initializes the right-click context menu.
	 */
	private void setMenu() {
		popupMenu = MenuItem.getPopupMenu();
		MenuItem.addActionListener( this );
		MenuItem.populateRecentlyOpened();
	}

	/**
	 * Attempts to register a hook to capture system-wide (global) key events.
	 * @return true if the hook was registered, false if not
	 */
	public boolean registerNativeKeyHook() {
		try {
			GlobalScreen.registerNativeHook();
			return true;
		} catch (NativeHookException e) {
			return false;
		}
	}

	private void forceResize() {
		Dimension newSize = new Dimension();
		newSize.height = getHeight();
		if (Settings.windowUserResizable.get())
			newSize.width = getWidth();
		else
			newSize.width = Settings.windowWidth.get();
		setSize(newSize);
		pack();
	}

	private void forceInternalComponentsResize()
	{
		setPreferredSize( preferredSize );
		pack();
	}
}
