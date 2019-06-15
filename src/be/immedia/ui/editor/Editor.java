/*
Copyright (C) 2007 defimedia sa

This file is part of AToms.

AToms is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 2 of the License.

AToms is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
version 2 along with AToms; see the file LICENSE.  If not, see
<http://www.gnu.org/licenses/> or write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package be.immedia.ui.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultTreeModel;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.IllegalNameException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import fr.imag.usybus_widgets_tracker.JFrameConnector;
import be.immedia.atoms.Params;
import be.immedia.awt.ExtensionFileFilter;
import be.immedia.awt.TableLayout;
import be.immedia.awt.Utils;
import be.immedia.ui.player.AbstractXMLModelLoader;
import be.immedia.ui.player.Link;
import be.immedia.ui.player.MM;
import be.immedia.ui.player.Player;
import be.immedia.util.ToolkitHelper;
import be.immedia.util.swing.JDateField;
import be.immedia.util.swing.JDoubleSpinner;
import be.immedia.util.swing.JIntegerSpinner;
import be.immedia.xml.text.HtmlEditor;

public final class Editor extends JFrame
  implements ActionListener
{
  static final Font FONT=new Font("Verdana",Font.PLAIN,11);
  private static final Border BORDER_RAISED=new BevelBorder(BevelBorder.RAISED);
  private static final Border BORDER_LOWERED=new BevelBorder(BevelBorder.LOWERED);
  private static final Font MINI_FONT=new Font("Helvetica",Font.PLAIN,10);
  private static final ExtensionFileFilter FILTER_XML=new ExtensionFileFilter("formulaire","xml");
  private static final ExtensionFileFilter FILTER_JAVA=new ExtensionFileFilter("formulaire","java");
  private static final String BASE_TITLE="Editor";
  static final File IMAGES=new File("images.jar");

  static final Object TABLE_DATA[][];
  static final Object TABLE_COLS[];
  static final Vector LIST_DATA;
  static final DefaultTreeModel TREE_MODEL=new DefaultTreeModel(new CounterTreeNode());
  static
  {
    int i,j,n;
    n=20;
    LIST_DATA=new Vector(n);
    for(i=1;i<=n;i++) LIST_DATA.add(String.valueOf(i));
    
    n=15;
    TABLE_DATA=new Object[n][n];
    TABLE_COLS=new Object[n];
    String string;
    for(j=0;j<n;j++)
    {
      string=String.valueOf((char)('A'+j));
      TABLE_COLS[j]=string;
      for(i=0;i<n;i++) TABLE_DATA[i][j]=string+(i+1);
    }
  }

  private JPanel mainPanel;
  private JScrollPane mainScroll;
  private Grid mainGrid;
  private JPopupMenu popup;
  private Grid selected;
  
  private JMenu mAdd,mTab,mBorder;
  private JMenuItem mDel,mPos,mConfig,mGridDelR,mGridDelC
    ,mGridAddRB,mGridAddRA,mGridAddCB,mGridAddCA,mAddGrid
    ,mBorderT,mBorderR,mBorderL,mBorder0,mTabE,mTabL,mTabR,mTabD;
  private JButton bAddGrid;
  
  private JButton bNew,bSave,bSaveJava,bLoad;
  private JButton bCut,bCopy,bPaste;
  private JButton bPlay;
  
  private File file=new File("./templates/test.xml");
//  private static String clipboard;

  public Editor()
  {
    super(BASE_TITLE);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());

    JToolBar toolbar=new JToolBar("Barre d'outils");
    getContentPane().add(toolbar,BorderLayout.NORTH);
    toolbar.add(bNew=new JButton(ToolkitHelper.getIcon("images/new24.gif")));
    bNew.setToolTipText("Nouveau"); bNew.addActionListener(this);
    toolbar.add(bLoad=new JButton(ToolkitHelper.getIcon("images/open24.gif")));
    bLoad.setToolTipText("Charger"); bLoad.addActionListener(this);
    toolbar.add(bSave=new JButton(ToolkitHelper.getIcon("images/save24.gif")));
    bSave.setToolTipText("Sauver"); bSave.addActionListener(this);
    toolbar.add(bSaveJava=new JButton(ToolkitHelper.getIcon("images/save24.gif")));
    bSaveJava.setToolTipText("Sauver en java"); bSaveJava.addActionListener(this);

    toolbar.addSeparator();
    toolbar.add(bCut=new JButton(ToolkitHelper.getIcon("images/cut.gif")));
    bCut.setToolTipText("Couper"); bCut.addActionListener(this);
    toolbar.add(bCopy=new JButton(ToolkitHelper.getIcon("images/copy.gif")));
    bCopy.setToolTipText("Copier"); bCopy.addActionListener(this);
    toolbar.add(bPaste=new JButton(ToolkitHelper.getIcon("images/paste.gif")));
    bPaste.setToolTipText("Coller"); bPaste.addActionListener(this);

    toolbar.addSeparator();
    toolbar.add(bPlay=new JButton(ToolkitHelper.getIcon("images/mini_X.gif")));
    bPlay.setToolTipText("Tester"); bPlay.addActionListener(this);

    mainPanel=new JPanel(new BorderLayout());
    getContentPane().add(mainPanel,BorderLayout.CENTER);
    
    JToolBar components=new JToolBar("Composants",JToolBar.VERTICAL);
    Container group;
    mainPanel.add(components,BorderLayout.WEST);
    
    components.add(group=createTitle("Textes"));
    group.add(createButton(JLabel.class,"Label"));
    group.add(createButton(JTextField.class,"Texte"));
    group.add(createButton(JTextArea.class,"Champ"));
    group.add(createButton(JPasswordField.class,"Mot de passe"));
    components.add(group=createTitle("Boutons"));
    group.add(createButton(JButton.class,"Bouton"));
    group.add(createButton(JCheckBox.class,"Case à cocher"));
    group.add(createButton(JRadioButton.class,"Radio"));
    group.add(createButton(JToolBar.class,"Barre d'outils"));
    components.add(group=createTitle("Conteneurs"));
    bAddGrid=new JButton(ToolkitHelper.getIcon("components/be.immedia.ui.editor.Grid.gif"));
    group.add(bAddGrid);
    bAddGrid.setToolTipText("Grille");
    bAddGrid.addActionListener(this);
    group.add(createButton(JSplitPane.class,"Diviseur"));
    group.add(createButton(JTabbedPane.class,"Onglets"));
    group.add(createButton(JScrollPane.class,"Zone défilante"));
    components.add(group=createTitle("Structures"));
    group.add(createButton(JList.class,"Liste"));
    group.add(createButton(JTable.class,"Table"));
    group.add(createButton(JComboBox.class,"Liste déroulante"));
    group.add(createButton(JTree.class,"Arbre"));
    components.add(group=createTitle("Spéciaux"));
    group.add(createButton(JSeparator.class,"Séparateur"));
    group.add(createButton(JProgressBar.class,"Barre d'attente"));
    group.add(createButton(JSlider.class,"Curseur"));
    group.add(createButton(JDateField.class,"Date/heure"));
    group.add(createButton(JIntegerSpinner.class,"Entier"));
    group.add(createButton(JDoubleSpinner.class,"Décimal"));
    group.add(createButton(HtmlEditor.class,"HTML"));
    group.add(createButton(MM.class,"MM"));
    group.add(createButton(Link.class,"lien"));

    try
    { group.add(createButton(Class.forName("be.immedia.ui.player.Geo"),"Geo")); }
    catch(Throwable ex)
    {}

    components.add(Box.createGlue());

    JMenu menu1,menu2;
    popup=new JPopupMenu();
    popup.add(menu1=mAdd=new JMenu("Ajouter"));
    
    menu1.add(menu2=new JMenu("Textes"));
    menu2.add(createMenu(JLabel.class,"Label"));
    menu2.add(createMenu(JTextField.class,"Texte"));
    menu2.add(createMenu(JTextArea.class,"Champ"));
    menu2.add(createMenu(JPasswordField.class,"Mot de passe"));

    menu1.add(menu2=new JMenu("Boutons"));
    menu2.add(createMenu(JButton.class,"Bouton"));
    menu2.add(createMenu(JCheckBox.class,"Case à cocher"));
    menu2.add(createMenu(JRadioButton.class,"Radio"));
    menu2.add(createMenu(JToolBar.class,"Barre d'outils"));

    menu1.add(menu2=new JMenu("Conteneurs"));

    mAddGrid=new JMenuItem("Grille",ToolkitHelper.getIcon("components/be.immedia.ui.editor.Grid.gif"));
    menu2.add(mAddGrid);
    mAddGrid.addActionListener(this);

    menu2.add(createMenu(JSplitPane.class,"Diviseur"));
    menu2.add(createMenu(JTabbedPane.class,"Onglets"));
    menu2.add(createMenu(JScrollPane.class,"Zone défilante"));

    menu1.add(menu2=new JMenu("Structures"));
    menu2.add(createMenu(JList.class,"Liste"));
    menu2.add(createMenu(JTable.class,"Table"));
    menu2.add(createMenu(JComboBox.class,"Liste déroulante"));
    menu2.add(createMenu(JTree.class,"Arbre"));

    menu1.add(menu2=new JMenu("Spéciaux"));
    menu2.add(createMenu(JSeparator.class,"Séparateur"));
    menu2.add(createMenu(JProgressBar.class,"Barre d'attente"));
    menu2.add(createMenu(JSlider.class,"Curseur"));
    menu2.add(createMenu(JDateField.class,"Date/heure"));
    menu2.add(createMenu(JIntegerSpinner.class,"Entier"));
    menu2.add(createMenu(JDoubleSpinner.class,"Décimal"));
    menu2.add(createMenu(HtmlEditor.class,"HTML"));
    menu2.add(createMenu(MM.class,"MM"));
    menu2.add(createMenu(Link.class,"lien"));
//    menu2.add(createMenu(JScrollBar.class,"Barre de défilement"));
    
    popup.add(mDel=new JMenuItem("Retirer")).addActionListener(this);
    popup.add(mPos=new JMenuItem("Positionnement")).addActionListener(this);
    popup.add(mConfig=new JMenuItem("Configurer")).addActionListener(this);

    popup.add(menu1=new JMenu("Grille"));
    menu1.add(mGridDelR=new JMenuItem("Retirer la ligne")).addActionListener(this);
    menu1.add(mGridDelC=new JMenuItem("Retirer la colonne")).addActionListener(this);

    menu1.add(menu2=new JMenu("Ajouter une ligne"));
    menu2.add(mGridAddRB=new JMenuItem("Avant")).addActionListener(this);
    menu2.add(mGridAddRA=new JMenuItem("Après")).addActionListener(this);

    menu1.add(menu2=new JMenu("Ajouter une colonne"));
    menu2.add(mGridAddCB=new JMenuItem("Avant")).addActionListener(this);
    menu2.add(mGridAddCA=new JMenuItem("Après")).addActionListener(this);

    popup.add(menu1=mTab=new JMenu("Onglets"));
    menu1.add(mTabE=new JMenuItem("Titre/icône")).addActionListener(this);
    menu1.add(mTabL=new JMenuItem("Déplacer vers gauche")).addActionListener(this);
    menu1.add(mTabR=new JMenuItem("Déplacer vers droite")).addActionListener(this);
    menu1.add(mTabD=new JMenuItem("Supprimer")).addActionListener(this);

    popup.add(menu1=mBorder=new JMenu("Bordure"));
    menu1.add(mBorderT=new JMenuItem("Titre")).addActionListener(this);
    menu1.add(mBorderR=new JMenuItem("Relévé")).addActionListener(this);
    menu1.add(mBorderL=new JMenuItem("Abaissé")).addActionListener(this);
    menu1.add(mBorder0=new JMenuItem("Aucun")).addActionListener(this);

    mainGrid=new Grid(this,4,4);
    mainScroll=new JScrollPane(mainGrid,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    mainPanel.add(mainScroll,BorderLayout.CENTER);
//    mainPanel.add(mainGrid,BorderLayout.CENTER);
    Utils.pack(this,false,false);
    setVisible(true);
  }
  
  private Container createTitle(String title)
  {
    JPanel p=new JPanel(new GridLayout(0,2));
    TitledBorder border;
    p.setBorder(border=new TitledBorder(title));
    border.setTitleFont(MINI_FONT);
//    p.setPreferredSize(new Dimension(64,80));
//    p.setSize(p.getPreferredSize());
//    p.setMaximumSize(p.getPreferredSize());
    return p;
  }

  private JButton createButton(Class cl,String text)
  {
    String name=cl.getName();
    JButton button=new JButton(ToolkitHelper.getIcon("components/"+name+".gif"));
    button.setName(name);
    button.setToolTipText(text);
    button.addActionListener(this);
    button.setPreferredSize(new Dimension(26,26));
    button.setSize(button.getPreferredSize());
    button.setMaximumSize(button.getPreferredSize());
    return button;
  }

  private JMenuItem createMenu(Class cl,String text)
  {
    String name=cl.getName();
    JMenuItem menu=new JMenuItem(text,ToolkitHelper.getIcon("components/"+name+".gif"));
    menu.setName(name);
    menu.addActionListener(this);
    return menu;
  }

  void select(Grid grid)
  {
    if((selected!=null)&&(selected!=grid)) selected.select(null);
    selected=grid;
  }
  
  void popup(Grid grid,int x,int y)
  {
    select(grid);
    boolean empty=(grid.selected.component==null);
    mAdd.setEnabled(empty);
    mDel.setEnabled(!empty);
    mPos.setEnabled(!empty);
//    mConfig.setEnabled(!empty);
    mGridDelR.setEnabled(!grid.fixedy && grid.dy>grid.selected.info.gridheight);
    mGridDelC.setEnabled(!grid.fixedx && grid.dx>grid.selected.info.gridwidth);
    mGridAddRB.setEnabled(!grid.fixedy);
    mGridAddRA.setEnabled(!grid.fixedy);
    mGridAddCB.setEnabled(!grid.fixedx);
    mGridAddCA.setEnabled(!grid.fixedx);
    
    if(empty)
    {
      mTab.setEnabled(false);
      mBorder.setEnabled(false);
    }
    else
    {
      mBorder.setEnabled(true);
      
      if(grid.selected.component instanceof JTabbedPane)
      {
        JTabbedPane tp=(JTabbedPane)grid.selected.component;
        mTab.setEnabled(true);
        mTabL.setEnabled(tp.getSelectedIndex()>0);
        mTabR.setEnabled(tp.getSelectedIndex()<tp.getTabCount()-1);
        mTabD.setEnabled(tp.getTabCount()>1);
      }
      else mTab.setEnabled(false);
    }
    popup.show(grid,x,y);
  }
  
  void config()
  {
    try
    {
	    if((selected!=null)&&(selected.selected!=null)&&(selected.selected.component!=null))
	    {
	      if(selected.selected.component instanceof Grid)
	      {
	        Grid grid=(Grid)selected.selected.component;
	        DialogGrid dialog=new DialogGrid(selected.selected.tag,grid.dx,grid.dy);
	        if((dialog.rows>0)&&(dialog.cols>0))
	        {
	          if(grid.resizeGrid(dialog.cols,dialog.rows,false))
	          {
	            selected.selected.tag=dialog.tag;
	            selected.selected.mask=null;
	            selected.selected.obligatoire=false;
              Utils.setFont(selected,FONT);
	            selected.revalidate(); selected.repaint();
	          }
	          else
	            JOptionPane.showMessageDialog(this,"Taille de grille non valide","Erreur",JOptionPane.ERROR_MESSAGE);
	        }
	      }
	      else selected.configurate();
	    }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this,ex.toString(),"Erreur",JOptionPane.ERROR_MESSAGE);
    }
  }

  void position()
  {
    try
    {
      if(selected!=null) selected.position();
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this,ex.toString(),"Erreur",JOptionPane.ERROR_MESSAGE);
    }
  }
  
  public void loadForm(String filename) throws Exception{
  	file=new File(filename);
  	loadForm();
  }
  
  private void loadForm() throws Exception{
    if(!file.isFile()) return;
    Grid grid=new Grid(this,new SAXBuilder().build(file).getRootElement().getChild("grid"));
    mainPanel.remove(mainScroll);
    
    mainGrid=grid;
    mainScroll=new JScrollPane(mainGrid,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    mainPanel.add(mainScroll,BorderLayout.CENTER);
    selected=null;
    Utils.setFont(mainPanel,FONT);
    mainPanel.revalidate();
    repaint();
    setTitle(BASE_TITLE+" - "+file.toString());
  }
  
  public void actionPerformed(ActionEvent e)
  {
    try
    {
      Object source=e.getSource();
      if((source instanceof JMenuItem)||(source instanceof JButton))
      {
        if(source==mConfig) config();
        else if(source==mDel) { if(selected!=null) selected.delete(); }
        else if(source==mPos) { if(selected!=null) selected.position(); }
        else if(source==mGridDelR) { if(selected!=null) selected.deleteRow(); }
        else if(source==mGridDelC) { if(selected!=null) selected.deleteColumn(); }
        else if(source==mGridAddRB) { if(selected!=null) selected.addRow(true); }
        else if(source==mGridAddRA) { if(selected!=null) selected.addRow(false); }
        else if(source==mGridAddCB) { if(selected!=null) selected.addColumn(true); }
        else if(source==mGridAddCA) { if(selected!=null) selected.addColumn(false); }
        else if((source==mAddGrid)||(source==bAddGrid))
        {
          if(selected!=null)
          { 
            DialogGrid dialog=new DialogGrid(null,0,0);
            if((dialog.rows>0)&&(dialog.cols>0))
            {
              selected.selected.setComponent(new Grid(this,dialog.cols,dialog.rows));
              selected.selected.tag=dialog.tag;
              selected.selected.mask=null;
              selected.selected.obligatoire=false;
              Utils.setFont(selected,FONT);
              selected.revalidate(); selected.repaint();
            }
          }
        }
        else if((source==mBorderT)||(source==mBorderL)||(source==mBorderR)||(source==mBorder0))
        {
          if((selected!=null)&&(selected.selected!=null)&&(selected.selected.component!=null))
          {
            Border border;
            if(source==mBorderT)
            {
              border=selected.selected.component.getBorder();
              String string="";
              if(border instanceof TitledBorder) string=((TitledBorder)border).getTitle();
              string=JOptionPane.showInputDialog(this,"Texte de la bordure : ",string);
              if(string!=null) border=new TitledBorder(string.trim());
            }
            else if(source==mBorderR) border=BORDER_RAISED;
            else if(source==mBorderL) border=BORDER_LOWERED;
            else border=null;
            selected.selected.component.setBorder(border);
          }
        }
        else if((source==mTabE)||(source==mTabL)||(source==mTabR)||(source==mTabD))
        {
          if((selected!=null)&&(selected.selected!=null)&&(selected.selected.component instanceof JTabbedPane))
          {
            JTabbedPane tp=(JTabbedPane)selected.selected.component;
            int n=tp.getTabCount(),i=tp.getSelectedIndex();
            if((i>=0)&&(i<n))
            {
             if(source==mTabD) tp.remove(i);
              else if(source==mTabE)
              {
                JLabel label=new JLabel(tp.getTitleAt(i),tp.getIconAt(i),JLabel.CENTER);
                label.setDisplayedMnemonic(tp.getMnemonicAt(i));
                label=(JLabel)new DialogComponent(this,label,Player.getTemplate("Tab")
                    ,null,null,false).component;
                if(label!=null)
                {
                  tp.setTitleAt(i,label.getText());
                  tp.setIconAt(i,label.getIcon());
                  tp.setMnemonicAt(i,label.getDisplayedMnemonic());
                }
              }
              else if((source==mTabL)&&(i>0))
              { Player.swap(tp,i-1,i); tp.setSelectedIndex(i-1); }
              else if((source==mTabR)&&(i<n-1))
              { Player.swap(tp,i,i+1); tp.setSelectedIndex(i+1); }
            }
          }
        }
        else if(source==bNew)
        {
          mainPanel.remove(mainScroll);
          
          mainGrid=new Grid(this,4,4);
          mainScroll=new JScrollPane(mainGrid,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
          mainPanel.add(mainScroll,BorderLayout.CENTER);
          selected=null;
          Utils.setFont(mainPanel,FONT);
          mainPanel.revalidate(); repaint();
        }
        else if(source==bPlay)
        {
          Document document=new Document(new Element("form").addContent(mainGrid.save()));
          JFrame frame=new JFrame();
          frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
          Player player=new Player(frame,document);
          player.load(new Document(new Element("test")),null,new InterfaceModelLoader(),(Params)null);
          Utils.pack(frame);
          JFrameConnector etc = new JFrameConnector(frame);
          frame.setVisible(true);
        }
        else if(source==bSave||source==bSaveJava)
        {
          String ext;
          if(source==bSaveJava) ext=".java";
          else ext=".xml";
          JFileChooser chooser=new JFileChooser(file.getParentFile());
          if(file.getName().endsWith(ext)) chooser.setSelectedFile(file);
          else
          {
          	String path=file.getAbsolutePath();
          	int index=path.lastIndexOf('.');
          	if(index>0) path=path.substring(0,index);
          	path=path+ext;
          	chooser.setSelectedFile(new File(path));
          }
          chooser.setFileFilter(source==bSaveJava?FILTER_JAVA:FILTER_XML);
          if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
          file=chooser.getSelectedFile();
          if(!file.getName().endsWith(ext))
            file=new File(file.getParent(),file.getName()+ext);
          if(file.isFile())
          {
            int i;
            i=JOptionPane.showConfirmDialog(this,"Voulez-vous écraser ce fichier ?\n"+file.getName()
                ,"Fichier existant",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
            if(i!=JOptionPane.YES_OPTION) return;
          }
          if(source==bSaveJava)
          {
          	String classname=file.getName();
          	classname=classname.substring(0,classname.indexOf('.'));
          	File p=file.getParentFile();
          	StringBuilder pname=new StringBuilder();
          	String packagename=null;
          	while(p!=null){
          		if(pname.length()>0) pname.insert(0,'.');
          		pname.insert(0,p.getName());
          		if("be".equals(p.getName())){
          			packagename=pname.toString();
          			break;
          		}
          		p=p.getParentFile();
          	}
          	String java=mainGrid.saveJava(classname,packagename);
          	FileWriter output=new FileWriter(file);
          	try
          	{
          		output.write(java);
          	}
          	finally
          	{ output.close(); }
          }
          else
          {
          	Document document=new Document(new Element("form").addContent(mainGrid.save()));
          	OutputStream output=new BufferedOutputStream(new FileOutputStream(file));
          	try
          	{
          		new XMLOutputter(Format.getPrettyFormat()).output(document,output);
          	}
          	finally
          	{ output.close(); }
          }
        }
        else if(source==bLoad)
        {
          JFileChooser chooser=new JFileChooser(file.getParentFile());
          chooser.setSelectedFile(file);
          chooser.setFileFilter(FILTER_XML);
          if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
          file=chooser.getSelectedFile();
        	loadForm();
        }
        else if(source==bCut)
        {
          if((selected!=null)&&(selected.selected!=null))
            selected.selected.cut();
/*
          if((selected!=null)&&(selected.selected!=null)&&(selected.selected.component!=null))
          {
            Element element=selected.selected.save();
            String string=new XMLOutputter().outputString(new Document(element));
            StringSelection selection=new StringSelection(string);
            getToolkit().getSystemClipboard().setContents(selection, selection);

            selected.delete();
          }
*/
        }
        else if(source==bCopy)
        {
          if((selected!=null)&&(selected.selected!=null))
            selected.selected.copy();
/*
          if((selected!=null)&&(selected.selected!=null)&&(selected.selected.component!=null))
          {
            Element element=selected.selected.save();
            String string=new XMLOutputter().outputString(new Document(element));
            StringSelection selection=new StringSelection(string);
            getToolkit().getSystemClipboard().setContents(selection, selection);
          }
*/
        }
        else if(source==bPaste)
        {
          if((selected!=null)&&(selected.selected!=null))
            selected.selected.paste();
/*
          if((selected!=null)&&(selected.selected!=null)&&(selected.selected.component==null))
          {
            Transferable transfer=getToolkit().getSystemClipboard().getContents(this);
            if(transfer!=null)
            {
              DataFlavor flavors[]=transfer.getTransferDataFlavors(),flavor=null;
              int i,n=flavors.length;
              for(i=0;(i<n)&&(flavor==null);i++)
              {
                if(flavors[i].getMimeType().startsWith("text/plain"))
                  flavor=flavors[i];
                else if(flavors[i].getMimeType().equals("application/x-java-serialized-object; class=java.lang.String"))
                  flavor=flavors[i];
              }
              if(flavor==null) flavor=DataFlavor.selectBestTextFlavor(flavors);
  
              Reader reader=flavor.getReaderForText(transfer);
              try
              {
                
  //              Document document=new SAXBuilder().build(new StringReader(clipboard));
                Document document=new SAXBuilder().build(reader);
                selected.create(document.getRootElement());
              }
              finally
              { reader.close(); }
            }
          }
*/
        }
        else
        {
          String name=((Component)source).getName();
          if((name!=null)&&(selected!=null)&&(selected.selected!=null))
          {
            Class cl=Class.forName(name);
            JComponent component;
            if(cl==JTree.class) component=new JTree(TREE_MODEL);
            else if(cl==JList.class) component=new JList(LIST_DATA);
            else if(cl==JComboBox.class) component=new JComboBox(LIST_DATA);
            else if(cl==JTable.class) component=new JTable(TABLE_DATA,TABLE_COLS);
            else if(cl==JToolBar.class){
            	component=new JToolBar();
            	component.add(new Grid(this,1,1,((JToolBar)component).getOrientation()==JToolBar.VERTICAL,((JToolBar)component).getOrientation()==JToolBar.HORIZONTAL));
            }
            else if(cl==JScrollPane.class) component=new JScrollPane(new Grid(this,1,1));
            else if(cl==JSplitPane.class) component=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new Grid(this,1,1),new Grid(this,1,1));
            else if(cl==Grid.class) component=new Grid(this,4,4);
            else component=(JComponent)cl.newInstance();
            selected.create(component);
          }
        }
      }
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this,ex.toString(),"Erreur",JOptionPane.ERROR_MESSAGE);
    }
  }
  
  private static int DEFAULT_ROWS=4;
  private static int DEFAULT_COLS=4;
  
  private final class DialogGrid extends JDialog
    implements ActionListener
  {
    private JButton bOk,bCancel;
    private JSpinner spRows,spCols;
    private JTextField tfTag;
    String tag;
    int rows,cols;

    DialogGrid(String tag,int cols,int rows)
    {
      super(Editor.this,true);
      this.tag=tag;
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      getContentPane().setLayout(new BorderLayout());
      JPanel p;
      getContentPane().add(p=new JPanel(new TableLayout(2)),BorderLayout.CENTER);
      p.add(new JLabel("XML : ",JLabel.RIGHT));
      p.add(tfTag=new JTextField(tag,32));

      p.add(new JLabel("Lignes : ",JLabel.RIGHT));
      if(cols<=0) cols=DEFAULT_COLS;
      if(rows<=0) rows=DEFAULT_ROWS;
      p.add(spRows=new JSpinner(new SpinnerNumberModel(rows,1,64,1)));
      p.add(new JLabel("Colonnes : ",JLabel.RIGHT));
      p.add(spCols=new JSpinner(new SpinnerNumberModel(cols,1,64,1)));
      getContentPane().add(p=new JPanel(new FlowLayout()),BorderLayout.SOUTH);
      p.add(bOk=new JButton("Ok",ToolkitHelper.getIcon("images/ok.gif"))); 
      bOk.addActionListener(this);
      p.add(bCancel=new JButton("Annuler",ToolkitHelper.getIcon("images/cancel.gif"))); 
      bCancel.addActionListener(this);
      Utils.pack(this);
      setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
      Object source=e.getSource();
      if(source==bCancel) dispose();
      else if(source==bOk)
      {
        try
        {
          String t=tfTag.getText().trim();
          if(t.length()==0) tag=null;
          else tag=new Element(t).getName();
        }
        catch(IllegalNameException ex)
        {
          JOptionPane.showMessageDialog(this,"Le nom de tag XML n'est pas valide","Erreur",JOptionPane.ERROR_MESSAGE);
          return;
        }
        DEFAULT_ROWS=rows=((Number)spRows.getValue()).intValue();
        DEFAULT_COLS=cols=((Number)spCols.getValue()).intValue();
        dispose();
      }
    }
  }
  
  private class InterfaceModelLoader extends AbstractXMLModelLoader{
	  @Override
    protected Document load(String address) throws Exception 
	  {
	    try
	    {
		    int i=address.indexOf(".zip/");
		    File file;
		    if(i>0)
		    {
		      String zip=address.substring(0,i+4);
		      String filename=address.substring(i+5)+".xml";
		      
			    ZipFile zipFile=new ZipFile(new File("model/"+zip));
			    try
			    {
			      ZipEntry entry=zipFile.getEntry(filename);
			      if((entry==null)||(entry.isDirectory()))
			        throw new FileNotFoundException(filename);
						InputStream input=zipFile.getInputStream(entry);
						try
						{
					    return new SAXBuilder().build(input);
						}
						finally
						{ input.close(); }
			    }
			    finally
			    { zipFile.close(); }
		    }
		
		    file=new File("model/"+address+".xml");
	      
	/*
	      Document document=new SAXBuilder().build(file);
	FileOutputStream fos=new FileOutputStream(file);
	try { new XMLOutputter().output(document,fos); } finally { fos.close(); }
	      return document;
	*/
		    return new SAXBuilder().build(file);
	    }
	    catch(Exception ex)
	    {
	      ex.printStackTrace();
	      System.out.println("model not found: "+address+" "+ex.toString());
	      return null;
	    }
	  }
	  
  }
}
