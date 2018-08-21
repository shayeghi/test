/*
 * Copyright 2014 Miklos Csuros (csurosm@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package count.gui;

import count.gui.kit.StringIcon;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 * Our prototypical Swing application with MacOS integration.
 * 
 * Mac integration uses code from Eirik Bjorsnos's Macify package
 * <code>org.simplericity.macify.eawt</code>.
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s csurosm@gmail.com
 * @since November 13, 2007, 12:04 PM
 */
public abstract class App implements InvocationHandler // for Mac application events
{

    /**
     * Cutoff for too many exceptions. 
     * When multiple exceptions are handled and the pop-up 
     * windows start to pile up (counted by {@link #num_caught_exceptions}, the executable simply quits. 
     */
    private static final int TOO_MANY_EXCEPTIONS = 10;

    /**
     * Frame in which the execution is done.
     */
    private final JFrame top_frame;

    /**
     * Main panel (content pane for top frame). 
     */
    private JPanel main_panel;
    
    /**
     * Main panel layout.
     */
    private CardLayout layout;
    
    /**
     * Internal counter for number of exceptions caught. 
     */
    private int num_caught_exceptions = 0;
    
    private final List<WorkSpace> work_space_list = new ArrayList<>();
    private int active_work_space=-1;
    
    
    /**
     * Initialize with a frame.
     * 
     * @param frame root container
     */
    public App(JFrame frame)
    {
        top_frame = frame;
        this.makeThreadExceptionHandler(Thread.currentThread());
        //frame.setUndecorated(true);
        initMainPanel();
        
        initMacApplication();
        initMenu();
    }
    
    public final JFrame getTopFrame(){ return top_frame;}
    
    /**
     * Under what name this program is known. 
     * Extending classes are expected to set their own short name. 
     * 
     * @return executable's title, used in conjunction with {@link #getVersionNumber() }
     */
    protected String getExecutableTitle(){return this.getClass().getName();}

    /**
     * Version number displayed in error messages.
     * Extending classes provide their own version numbers. 
     * 
     * @return a string that is appended to 'v' for display
     */
    protected String getVersionNumber(){ return "0.0";}
    

    /**
     * Called from constructor after top-level container is set.
     * 
     * Sets main panel and layout. 
     */
    private void initMainPanel()
    {
        layout = new CardLayout();
        main_panel=new JPanel(layout);
        main_panel.setOpaque(true);        
        top_frame.setContentPane(main_panel);
        
    }
    
    /** 
     * Brings the work space with the given key in the foreground.
     * 
     * @param workspace_idx index of the workspace to select
     */
    public void show(int workspace_idx)
    {
        if (workspace_idx == -1)
                top_frame.setTitle(getExecutableTitle()); // +" v"+VERSION_NUMBER);
        else
        {
            WorkSpace ws = work_space_list.get(workspace_idx);
            String component_key = ws.getSessionName();
            layout.show(main_panel,component_key);
            top_frame.setTitle(component_key);
        }
        this.active_work_space = workspace_idx;
    }
    
    
    public WorkSpace getActiveWorkSpace()
    {
        if (active_work_space == -1)
            return null;
        else
            return work_space_list.get(active_work_space);
    }

    public void addWorkSpace(WorkSpace work_space)
    {
        work_space_list.add(work_space);
        String key =  work_space.getSessionName();//Integer.toString(work_space_list.size()); // work_space.getAssociatedFile().getName()
        //main_panel.invalidate();
        main_panel.add(work_space, key);
        show(work_space_list.size()-1);
        initMenu();
        main_panel.validate();
    }
    
    public void removeWorkSpace(WorkSpace work_space)
    {
        work_space_list.remove(work_space);
        main_panel.remove(work_space);
        //layout.removeLayoutComponent(work_space);
        show(work_space_list.size()-1);          
        initMenu();

        main_panel.validate();
    }
    
    public int getWorkSpaceCount(){ return work_space_list.size();}
    
    public WorkSpace getWorkSpace(int idx)
    {
        return work_space_list.get(idx);
    }
        
    /**
     * Menu bar at the top-level container
     */
    protected JMenuBar top_menu_bar;
    
    protected JMenu session_menu;
    protected JMenu data_menu;
    protected JMenu rate_menu;
    protected JMenu analysis_menu;
    
    protected JMenu help_menu;
    
    /**
     * Initializes the top menu.
     */
    private void initMenu()
    {
        if (session_menu==null)
            session_menu = new JMenu("Session");
        
        setSessionMenu();
        
        if (data_menu==null)
            data_menu = new JMenu("Data");
        
        setDataMenu();
        
        if (rate_menu==null)
            rate_menu = new JMenu("Rates");
        setRateMenu();
            
        if (analysis_menu==null)
            analysis_menu = new JMenu("Analysis");
        
        setAnalysisMenu();
        
        if (help_menu==null)
            help_menu = new JMenu("Help");
        
        setHelpMenu();

        if (top_menu_bar == null)
        {
            top_menu_bar = new JMenuBar();
            
            top_menu_bar.add(session_menu);
            
            top_menu_bar.add(data_menu);
            top_menu_bar.add(rate_menu);        
            top_menu_bar.add(analysis_menu);
            top_menu_bar.add(help_menu);

            top_frame.setJMenuBar(top_menu_bar);
        }
    }


   /**
     * Sets up the menu items in the <em>Session</em> menu. Calls {@link #addSessionMenuItems() } and adds a Quit option if not a Mac.
     */
    protected void setSessionMenu()
    {
        session_menu.removeAll();
        
        addSessionMenuItems();

        if (!isMac())
        {
            JMenuItem item = new JMenuItem("Quit");
            item.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent E)
                {
                    doQuit();
                }
            });
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            session_menu.add(item);
        }
    }
    
    /**
     * Called by the session menu: adds sessions from {@link #work_space_list}.
     */
    protected void addSessionMenuItems()
    {
        if (!work_space_list.isEmpty())
        {
            session_menu.addSeparator();
            for (int j=0; j<work_space_list.size(); j++)
            {
                WorkSpace work_space = work_space_list.get(j);
                JMenuItem item = null;
                if (j==active_work_space) 
                    item = new JMenuItem(work_space.getSessionName(), StringIcon.createRightPointingFinger());
                else 
                    item = new JMenuItem(work_space.getSessionName());
                item.addActionListener(new WorkSpaceActivator(j));
                session_menu.add(item);
            }
        }
    }
    
    protected abstract void setDataMenu();
    
    protected abstract void setRateMenu();
    
    protected abstract void setAnalysisMenu();
    
    
    private class WorkSpaceActivator implements ActionListener
    {
        private int window_idx;
        WorkSpaceActivator(int window_idx)
        {
            this.window_idx=window_idx;
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            show(window_idx);
            setSessionMenu();
            setDataMenu();
            setRateMenu();
            setAnalysisMenu();
        }
    }
        
    
    /**
     * The default implementation is that an About item is added when necessary.
     */
    protected void setHelpMenu()
    {
        if (help_menu.getItemCount()==0)
        {
            if (Mac_application == null)
            {
                JMenuItem about_item = new JMenuItem(ABOUT_MENU_TEXT);
                about_item.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent E)
                    {
                        doAbout();
                    }
                });
                help_menu.add(about_item);
            }
        }
    }
        
    protected String ABOUT_MENU_TEXT = "About";    
    
    
    /**
     * Pop-up dialog for generic error message. This should be called from within the Event Dispatch Thread.
     * 
     * @param E the exception that is reported here
     * @param title title for the dialog window
     * 
     */
    public void handleException(Throwable E, String title)
    {
        App.this.handleException(E, title, null);
    }
    
    /**
     * Pop-up dialog for generic error message. 
     * This should be called from within the Event Dispatch Thread.
     * 
     * @param E the exception that is reported here
     * @param title title for the dialog window
     * @param more_info Additional information about the error
     */
    public void handleException(Throwable E, String title, String more_info)
    {
        synchronized(this)
        {
            num_caught_exceptions++;
            if (num_caught_exceptions==TOO_MANY_EXCEPTIONS)
            {
                System.err.println("TOO MANY ERRORS in too many threads: "+E);
                E.printStackTrace(System.err);
                System.exit(2016);
            }
        }

        StringWriter SW = new StringWriter();
        E.printStackTrace(new PrintWriter(SW));
        //String stack_trace = SW.toString().trim();
        
        StringBuilder short_message = new StringBuilder();
        short_message.append("<p><b>Awfully sorry - something went astray.</b></p>");
        if (more_info != null)
            short_message.append("<p>").append(more_info).append("</p>");
        short_message.append("<p>If this is surprising, " +
            "then please write to Mikl&oacute;s Cs&#369;r&ouml;s [csurosm@gmail.com] about it:<br />" +
            "click below to display the technical information, and copy the text into your e-mail.</p>");
        
        // system info
        java.util.Date now = java.util.Calendar.getInstance().getTime();
        java.util.Properties Props=System.getProperties();
        String system = Props.getProperty("os.name", "[unknown OS]")+" "+Props.getProperty("os.version","[Unknown version]")+" "+Props.getProperty("os.arch","[Unknown architecture]");
        String java = Props.getProperty("java.vm.name","[Unknown VM]")+" "+Props.getProperty("java.vm.version","[Unknown version]")+" ("+Props.getProperty("java.runtime.version","[Unknwon runtime]")+") "+Props.getProperty("java.vm.info","")+", "+Props.getProperty("java.vm.vendor","[Uknown vendor]");
        
        StringBuilder message = new StringBuilder();
        message.append("<p>Version: ").append(getExecutableTitle()).append(" v").append(getVersionNumber());
        message.append("<br>System: ").append(system);
        message.append("<br>Java engine: ").append(java);
        message.append("<br>UIManager: ").append(javax.swing.UIManager.getLookAndFeel().getName());
        message.append("<br>Thread: ").append(Thread.currentThread()); // EDT unless coding mistake
        message.append("<br>Date: ").append(now);
        message.append("</p>");
        
        message.append("<p><i>Stack trace:</i></p>");
        message.append("<pre>");
        message.append(SW.getBuffer());
        message.append("</pre>");
        
        JEditorPane shortEP = new JEditorPane("text/html", short_message.toString());
        shortEP.setEditable(false);
        shortEP.setBackground(Mien.WARNING_COLOR);
        shortEP.setBorder(BorderFactory.createRaisedBevelBorder());
        
        final JEditorPane EP = new JEditorPane("text/html",message.toString());
        EP.setEditable(false);
        EP.setBorder(BorderFactory.createEtchedBorder());
        
        JPanel msg_display = new JPanel();
        BoxLayout msg_layout  = new javax.swing.BoxLayout(msg_display, BoxLayout.PAGE_AXIS);
        msg_display.setLayout(msg_layout);
        final JCheckBox show_details = new JCheckBox("Show technical information");
        show_details.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent ignored)
                {
                    EP.setVisible(show_details.isSelected());
                }
            });
        show_details.setSelected(false); 
        EP.setVisible(false);
        msg_display.add(shortEP);
        msg_display.add(show_details);
        msg_display.add(EP);
        
        JScrollPane msg_pane = new JScrollPane(msg_display);
        msg_pane.setPreferredSize(new Dimension(900, 600));
        msg_pane.setMinimumSize(new Dimension(80, 30));   
        
        Object[] button_text = {"So be it", "Quit "+getExecutableTitle()};
        
        int ans = JOptionPane.showOptionDialog(getTopFrame(),msg_pane,title,
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.ERROR_MESSAGE,
                null,button_text,button_text[0]);

        //System.out.println("#*"+getClass().getName()+".handleException "+EP.getText()+"\n// "+Thread.currentThread());

        if (ans==1)
            doQuit();
    }    


    /**
     * Turns this guy into the uncaught exception handler for the thread.
     * 
     * @param T a thread
     */
    public final void makeThreadExceptionHandler(Thread T)
    {
        T.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, final Throwable T)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String[] synonyms = 
                        {   "puzzling", 
                            "bewildering", 
                            "perplexing", 
                            "mystifying", 
                            "baffling", 
                            "mysterious", 
                            "peculiar", 
                            "curious", 
                            "bizarre", 
                            "strange", 
                            "weird",
                            "unfathomable",
                            "abstruse",
                            "enigmatic",
                            "unanticipated",
                            "unforeseen",
                            "unexpected",
                            "startling"};
                        java.util.Random RND = new java.util.Random();
                        String error_attribute = synonyms[RND.nextInt(synonyms.length)];
                        String article = "aeiou".indexOf(error_attribute.charAt(0))==-1?"A ":"An ";
                        handleException(T, article+error_attribute+" error...");
                    }
                });
            }            
        });
    }    
    
    /**
     * Called when user selects the About menu point. 
     * To be developed by extending classes. 
     */
   protected void doAbout(){}
    /**
     * Called when user selects the Quit menu point, or programmatically. 
     * 
     * Default implementation calls System.exit(0).
     */
    protected void doQuit()
    {
        System.out.println("#**This is doQuit!");
        System.exit(0);
    }
    

    /**
     * 
     * MacOS X integration --- code based on Eirik Bjorsnos org.simplericity.macify.eawt
     */
    private Object Mac_application = null;
    
    public final boolean isMac()
    {
        return Mac_application != null;
    }
    
    /**
     * Initializes Mac_application
     */
    private void initMacApplication()
    {
        System.setProperty("apple.awt.textantialiasing", "true");            
        try 
        {
            final File file = new File("/System/Library/Java");
            if(file.exists()) 
            {
                ClassLoader scl = ClassLoader.getSystemClassLoader();
                Class clc = scl.getClass();
                if(URLClassLoader.class.isAssignableFrom(clc)) 
                {
                    Method addUrl  = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
                    addUrl.setAccessible(true);
                    addUrl.invoke(scl, new Object[] {file.toURI().toURL()});
                }
            }

            Class<?> application_class = Class.forName("com.apple.eawt.Application");
            Mac_application = application_class.getMethod("getApplication", new Class[0]).invoke(null, new Object[0]);
            Class application_listener_class = Class.forName("com.apple.eawt.ApplicationListener");
            Object listener = Proxy.newProxyInstance(getClass().getClassLoader(), 
                    new Class[]{application_listener_class},
                    this);
            callMacMethod("addApplicationListener",new Class[]{application_listener_class}, new Object[]{listener});
        } catch (ClassNotFoundException e) 
        {
            Mac_application = null;
        } catch (Exception e) 
        {
            handleException(e,"A bug?","There was a problem with the initialization of the application.");
        }
    }

    private Object callMacMethod(String methodname, Class[] classes, Object[] arguments) 
    {
        try 
        {
            if (classes == null) 
            {
                classes = new Class[arguments.length];
                for (int i = 0; i < classes.length; i++) 
                {
                    classes[i] = arguments[i].getClass();

                }
            }
            Method method = Mac_application.getClass().getMethod(methodname, classes);
            return method.invoke(Mac_application, arguments);
        } catch (Exception E) 
        {
            handleException(E, "A bug?", "Problem when trying to invoke method "+methodname+" for "+Mac_application);
            return null;
        }
    }
    
    /**
     * Called with application events. (Needed for MacOS integration --- we check here for Apple events like about and quit)
     * 
     * @param proxy proxy instance
     * @param apple_method the method
     * @param args method arguments
     * @throws Throwable by interface definition; all are caught internally 
     */
    @Override
    public Object invoke(Object proxy,
                     Method apple_method,
                     Object[] args)
              throws Throwable   
    {
        try 
        { // check if Apple event
            String method_name = apple_method.getName();
            if ("handleAbout".equals(method_name))
            {
                doAbout();
                // make sure no other window pops up ...
                Class<?> event_class = Class.forName("com.apple.eawt.ApplicationEvent");
                Method set_handled = event_class.getMethod("setHandled", new Class[]{boolean.class});
                set_handled.invoke(args[0],new Object[]{Boolean.TRUE});
            }
            else if ("handleQuit".equals(method_name))
            {
                    doQuit();
            } else
            {
                //System.out.println("#**D.invoke "+method_name);
            }
        } 
        catch (Exception exception) // all exceptions are handled the same way
                {
            handleException(exception, "A bug?", "An error occurred while trying to handle an Apple event");
        }
        return null;
    }
    
}
