/*
Copyright (C) 2006 defimedia sa

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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;

import org.jdom.*;

import be.immedia.awt.*;
import be.immedia.ui.player.Link;
import be.immedia.ui.player.MM;
import be.immedia.ui.player.Player;
import be.immedia.ui.player.XMLContent;
import be.immedia.util.Item;
import be.immedia.util.ToolkitHelper;
import be.immedia.util.swing.ImageInZipChooser;
import be.immedia.util.swing.JDateField;
import be.immedia.xml.text.HtmlEditor;

final class DialogComponent extends JDialog
  implements WindowListener,ActionListener
    ,DocumentListener,ItemListener,ChangeListener
{
  private final Editor editor;
  private final HashMap params=new HashMap();
  private final JComponent model;
  private JButton bOk,bCancel;
  private JTextField tfTag,tfMask;
  private JCheckBox cbObligatoire;
  JComponent component;
  String tag,mask;
  boolean obligatoire;
  
  DialogComponent(Editor editor,JComponent component 
      ,String tag,String mask,boolean obligatoire) 
    throws JDOMException, IOException, InstantiationException
      , IllegalArgumentException, SecurityException
      , IllegalAccessException, InvocationTargetException
      , NoSuchMethodException 
  {
    this(editor,component,Player.getTemplate(component.getClass().getName())
        ,tag,mask,obligatoire);
  }
  
  DialogComponent(Editor editor,JComponent component,Document template
      ,String tag,String mask,boolean obligatoire) 
    throws JDOMException, IOException, InstantiationException
      , IllegalArgumentException, SecurityException
      , IllegalAccessException, InvocationTargetException
      , NoSuchMethodException 
  {
    super(editor,true);
    this.editor=editor;
    this.component=component;
    this.tag=tag; this.mask=mask; this.obligatoire=obligatoire;
    Class cl=component.getClass();
    model=(JComponent)cl.newInstance();
    if(cl==JTable.class)
      ((JTable)model).setModel(((JTable)component).getModel());
    else if(cl==JTree.class)
      ((JTree)model).setModel(((JTree)component).getModel());
    else if(cl==JList.class)
      ((JList)model).setModel(((JList)component).getModel());
    else if(cl==JComboBox.class)
      ((JComboBox)model).setModel(((JComboBox)component).getModel());
    else if(cl==JSplitPane.class)
    {
      ((JSplitPane)model).setLeftComponent(new JScrollPane(new JTree()));
      ((JSplitPane)model).setRightComponent(new JScrollPane(new JTree()));
    }
    else if(cl==JScrollPane.class)
    {
      ((JScrollPane)model).setViewportView(new JTree());
      ((JScrollPane)model).setPreferredSize(new Dimension(120,120));
    }
    else if(cl==JToolBar.class)
    {
      ((JToolBar)model).add(new JButton("1"));
      ((JToolBar)model).add(new JButton("2"));
      ((JToolBar)model).add(new JButton("3"));
      ((JToolBar)model).add(new JButton("4"));
    }

    getContentPane().setLayout(new BorderLayout());
    JPanel p=new JPanel(new TableLayout(2));
    getContentPane().add(p,BorderLayout.NORTH);
    Element root=template.getRootElement();
    setTitle(root.getAttributeValue("label"));
    Iterator iterator1,iterator2;
    Element element,option;
    String name,label,property,string;
    Object value;
    JComponent param;
    Vector vector;
    JLabel l;
    int i;
    
    if((root.getAttributeValue("class")!=null)
      &&(cl!=JScrollPane.class)&&(cl!=JSplitPane.class)&&(cl!=JSeparator.class)
      &&(cl!=JProgressBar.class)&&(cl!=JToolBar.class)&&(cl!=JButton.class)
      || XMLContent.class.isAssignableFrom(cl))
    {
      p.add(l=new JLabel("XML : ",JLabel.RIGHT));
      l.setVerticalAlignment(JLabel.TOP);
      p.add(tfTag=new JTextField(tag,32));
    }

    if((cl==JTextField.class)||(cl==JPasswordField.class))
    {
      p.add(l=new JLabel("Masque : ",JLabel.RIGHT));
      l.setVerticalAlignment(JLabel.TOP);
      p.add(tfMask=new JTextField(mask,32));
    }
    if((cl==JTextField.class)||(cl==JTextArea.class)||(cl==JPasswordField.class)
        ||(cl==JDateField.class)||(cl==JList.class)||(cl==JComboBox.class)
        ||(cl==JTree.class)||(cl==HtmlEditor.class)
        ||(cl==MM.class)||(cl==Link.class))
    {
      p.add(l=new JLabel("Obligatoire : ",JLabel.RIGHT));
      l.setVerticalAlignment(JLabel.TOP);
      p.add(cbObligatoire=new JCheckBox("Oui",obligatoire));
    }

    for(iterator1=root.getChildren().iterator();iterator1.hasNext();)
    {
      element=(Element)iterator1.next();
      name=element.getName();
      label=element.getAttributeValue("label");
      property=element.getAttributeValue("property");
      value=Player.getField(component,property);
      if("false".equals(element.getAttributeValue("bean")))
        property="special."+property;
      
      if(name.equals("text"))
      {
        JTextComponent tc;
        string=(value==null)?"":value.toString();
        if("true".equals(element.getAttributeValue("multi")))
          tc=new JTextArea(string,8,32);
        else
          tc=new JTextField(string,32);
        param=tc;
        tc.getDocument().addDocumentListener(this);
      }
      else if(name.equals("choice"))
      {
        int v;
        vector=new Vector(); i=0;
        for(iterator2=element.getChildren("option").iterator();iterator2.hasNext();)
        {
          option=(Element)iterator2.next();
          v=Integer.parseInt(option.getAttributeValue("value"));
          if(((Integer)value).intValue()==v) i=vector.size();
          vector.add(new Item(v,option.getText()));
        }
        JComboBox cb;
        param=cb=new JComboBox(vector);
        cb.addItemListener(this);
        cb.setSelectedIndex(i);
        
      }
      else if(name.equals("integer"))
      {
        JSpinner sp;
        int v,min,max;        
        min=Integer.parseInt(element.getAttributeValue("min"));
        max=Integer.parseInt(element.getAttributeValue("max"));
        v=((Number)value).intValue();
        if(v<min) v=min; else if(v>=max) v=max;
        param=sp=new JSpinner(new SpinnerNumberModel(v,min,max,1));
        sp.addChangeListener(this);
      }
      else if(name.equals("double"))
      {
        JSpinner sp;
        double v,min,max;        
        
        min=Double.parseDouble(element.getAttributeValue("min"));
        max=Double.parseDouble(element.getAttributeValue("max"));
        v=((Number)value).doubleValue();
        if(v<min) v=min; else if(v>max) v=max;
        param=sp=new JSpinner(new SpinnerNumberModel(v,min,max,1));
        sp.addChangeListener(this);
      }
      else if(name.equals("boolean"))
      {
        JCheckBox cb;
        param=cb=new JCheckBox("Oui",Boolean.TRUE.equals(value));
        cb.addItemListener(this);
      }
      else if(name.equals("icon"))
      {
        JButton b;
        param=b=new JButton("Choisir",(Icon)value);
        b.addActionListener(this);
      }
      else if(name.equals("date"))
      {
        JDateField df;
        param=df=new JDateField();
        df.setDate((Date)value);
        df.getDocument().addDocumentListener(this);
      }
      else
        param=new JLabel("?"+name+"?");

      p.add(l=new JLabel(label+" : ",JLabel.RIGHT));
      l.setVerticalAlignment(JLabel.TOP);
      if(param instanceof JTextArea) p.add(new JScrollPane(param));
      else p.add(param);
      
      param.setName(name);
      params.put(property,param);
    }
    
    JComponent c;
    
    if((cl==JSplitPane.class)||(cl==JScrollPane.class)||(cl==JTabbedPane.class))
    {
      c=model;
    }
    else if((cl==JTable.class)||(cl==JTree.class)||(cl==JTextArea.class)
        ||(cl==JList.class))
    {
      c=new JScrollPane(model);
    }
    else if(cl==JToolBar.class)
    {
      p=new JPanel(new BorderLayout());
      p.add(model,BorderLayout.NORTH);
      c=p;
      c.setPreferredSize(new Dimension(180,180));
    }
    else if(cl==JSeparator.class)
    {
      p=new JPanel(new FlowLayout());
      p.add(new JButton("1"));
      p.add(model);
      p.add(new JButton("2"));
      c=p;
    }
    else
    {
      p=new JPanel(new FlowLayout());
      p.add(model);
      c=new JScrollPane(p);
      c.setPreferredSize(new Dimension(240,240));
    }
    getContentPane().add(c,BorderLayout.CENTER);


    p=new JPanel(new FlowLayout());
    getContentPane().add(p,BorderLayout.SOUTH);
    p.add(bOk=new JButton("Ok",ToolkitHelper.getIcon("images/ok.gif"))); 
    bOk.addActionListener(this);
    p.add(bCancel=new JButton("Annuler",ToolkitHelper.getIcon("images/cancel.gif"))); 
    bCancel.addActionListener(this);

    changed(model);
    getRootPane().setDefaultButton(bOk);
    setResizable(false);
    Utils.pack(this);
    setVisible(true);
  }

  public void actionPerformed(ActionEvent e)
  {
    try
    {
      Object source=e.getSource();
      if(source instanceof JButton)
      {
        if(source==bCancel)
        {
          component=null; dispose();
        }
        else if(source==bOk)
        {
          changed(component);
          if(tfTag!=null)
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
          }
          if(tfMask!=null)
          {
            try
            {
              String t=tfMask.getText().trim();
              if(t.length()==0) mask=null;
              else mask=Pattern.compile(t).pattern();
            }
            catch(IllegalNameException ex)
            {
              JOptionPane.showMessageDialog(this,"Le masque de saisie n'est pas valide","Erreur",JOptionPane.ERROR_MESSAGE);
              return;
            }
          }
          if(cbObligatoire!=null) obligatoire=cbObligatoire.isSelected();
          dispose();
        }
        else
        {
          if(params.containsValue(source))
          {
            JButton button=(JButton)source;
//            params.clientXML.getRemoteFile("/template/demarche.xml")
            button.setIcon(ImageInZipChooser.pick(this,Editor.IMAGES,(ImageIcon)button.getIcon()));
            changed(model);
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

  public void windowActivated(WindowEvent e) {}
  public void windowClosed(WindowEvent e) {}
  public void windowClosing(WindowEvent e) { component=null; dispose(); }
  public void windowDeactivated(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}

  private void changed(JComponent model)
  {
    Map.Entry entry;
    String property,name;
    Component component;
    boolean special;
    
    for(Iterator iterator=params.entrySet().iterator();iterator.hasNext();)
    {
      entry=(Map.Entry)iterator.next();
      property=(String)entry.getKey();
      component=(Component)entry.getValue();
      name=component.getName();
      special=property.startsWith("special");
      try
      {
        if(name.equals("text"))
        {
          if(!special)
          {
            String string=((JTextComponent)component).getText();
            Player.setField(model,property,string);
          }
        }
        else if(name.equals("choice"))
        {
          if(!special)
          {
          	if((model instanceof JToolBar)
	              &&(property.equals("Orientation")))
	          {
	          	JToolBar tb=(JToolBar)model;
	          	int orientation=((Item)((JComboBox)component).getSelectedItem()).id;
	          	if(model==this.component)
	          	{
		          	if(tb.getOrientation()!=orientation)
		          	{
		          		Grid previous=(Grid)tb.getComponent(0);
		          		tb.removeAll();
		          		tb.setOrientation(orientation);
		          		tb.add(new Grid(previous,true));
		          	}
	          	}
	          	else
	          	{
	          		tb.setOrientation(orientation);
	          	}
	          }
          	else
          	{
	            Item item=(Item)((JComboBox)component).getSelectedItem();
	            Player.setField(model,property,new Integer(item.id));
          	}
          }
        }
        else if(name.equals("integer"))
        {
          if(!special)
          {
            Player.setField(model,property,((JSpinner)component).getValue());
          }
          else
          {
	          if((model instanceof JTabbedPane)
	              &&(property.equals("special.TabCount")))
	          {
	            JTabbedPane tp=(JTabbedPane)model;
	            int n1=tp.getTabCount();
	            int i,n2=((Number)((JSpinner)component).getValue()).intValue();
	            int n=Math.max(n1,n2);
	            int sel=tp.getSelectedIndex();
	            
	            for(i=0;i<n;i++)
	            {
	              if(i>=n1)
	              {
	                if(model==this.model)
	                  tp.add(String.valueOf(i+1),new JScrollPane(new JTree()));
	                else
	                  tp.add(String.valueOf(i+1),new Grid(editor,1,1));
	              }
	              else if(i>=n2)
	              {
	                tp.remove(i);
	              }
	            }
	            if(sel>=n2) sel=n2-1; if(sel<0) sel=0;
	            if(sel<n2) tp.setSelectedIndex(sel);
	          }
          }
        }
        else if(name.equals("double"))
        {
          if(!special)
          {
            Player.setField(model,property,((JSpinner)component).getValue());
//            Player.setField(model,property,new Double(0.001d*((JSlider)component).getValue()));
          }
        }
        else if(name.equals("icon"))
        {
          if(!special)
          {
            Player.setField(model,property,((JButton)component).getIcon());
          }
        }
        else if(name.equals("boolean"))
        {
          if(!special)
          {
            Player.setField(model,property,new Boolean(((JCheckBox)component).isSelected()));
          }
        }
        else if(name.equals("date"))
        {
          if(!special)
          {
            Player.setField(model,property,((JDateField)component).getDate());
          }
        }
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
      }
    }
    if(model==this.model)
    {
      pack();
      model.revalidate();
      Container container=model.getTopLevelAncestor();
      if(container!=null) container.repaint();
    }
  }
  
  public void changedUpdate(DocumentEvent e) { changed(model); }
  public void insertUpdate(DocumentEvent e) { changed(model); }
  public void removeUpdate(DocumentEvent e) { changed(model); }
  public void itemStateChanged(ItemEvent e) { changed(model); }
  public void stateChanged(ChangeEvent e) { changed(model); }
  
  static Element save(JComponent component) 
    throws JDOMException, IOException, InstantiationException
    , IllegalArgumentException, SecurityException, IllegalAccessException
    , InvocationTargetException, NoSuchMethodException 
  {
    Class cl=component.getClass(),ca;
    Document template=Player.getTemplate(cl.getName());
    JComponent model=(JComponent)cl.newInstance();

    Element element=new Element("component"),e;
    element.setAttribute("class",cl.getName());
    
    String property,string;
    Object value,def;

    for(Iterator iterator=template.getRootElement().getChildren().iterator();iterator.hasNext();)
    {
      e=(Element)iterator.next();
      if("false".equals(e.getAttributeValue("bean"))) continue;

      property=e.getAttributeValue("property");
      if(property.startsWith("@")) continue;
      value=Player.getField(component,property);
      def=Player.getField(model,property);
      
      if(((def==null)&&(value==null))||((def!=null)&&(def.equals(value)))
          ||((def==null)&&("".equals(value))))
      {
      }
      else
      {
        e=new Element("param");
        e.setAttribute("name",property);
        if(value!=null)
        {
          ca=value.getClass();
          e.setAttribute("class",ca.getName());
          if(ca==ImageIcon.class) string=((ImageIcon)value).getDescription();
          else if(ca==Date.class) string=Long.toString(((Date)value).getTime());
          else string=value.toString();
          e.addContent(string);
        }
        element.addContent(e);
      }
    }
    return element;
  }
}
