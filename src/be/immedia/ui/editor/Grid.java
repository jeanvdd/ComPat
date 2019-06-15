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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import be.immedia.awt.TableLayout;
import be.immedia.ui.player.Player;
import be.immedia.util.StringHelper;
import be.immedia.util.ToolkitHelper;
import be.immedia.util.data.MapObjectInt;

public final class Grid extends JPanel
  implements MouseListener,MouseMotionListener//,ActionListener
{
  private static final Border BORDER_NORMAL
    =new CompoundBorder(new EmptyBorder(1,1,1,1),new LineBorder(Color.BLUE,4));
  private static final Border BORDER_SELECTED
    =new CompoundBorder(new EmptyBorder(1,1,1,1),new LineBorder(Color.ORANGE,4));

  private final Editor editor;
  private GridBagLayout layout;
  Wrapper selected;
  
  int dx,dy;
  final boolean fixedx,fixedy;
  private Wrapper wrappers[][];
  
  private boolean dragging;
  private int dragx,dragy;
  private boolean moving;

  Grid(Editor editor,int dx,int dy)
  {
  	this(editor,dx,dy,false,false);
  }
  Grid(Editor editor,int dx,int dy,boolean fixedx,boolean fixedy)
  {
    this.fixedx=fixedx;
    this.fixedy=fixedy;
  	this.editor=editor;
    layout=new GridBagLayout();
    setLayout(layout);
    if(!resizeGrid(dx,dy,false)) throw new IllegalArgumentException("Illegal grid size: "+dx+","+dy);
    addMouseListener(this);
    addMouseMotionListener(this);
  }
  
  /**
   * @param revert indique s'il faut inverser le tableau en le copiant
   */
  Grid(Grid oldgrid,boolean revert)
  {
  	this.editor=oldgrid.editor;
    layout=new GridBagLayout();
    setLayout(layout);
  	if(revert){
  		this.fixedx=oldgrid.fixedy;
  		this.fixedy=oldgrid.fixedx;
      if(!resizeGrid(oldgrid.dy,oldgrid.dx,false)) throw new IllegalArgumentException("Illegal grid size: "+oldgrid.dy+","+oldgrid.dx);
  	}else{
  		this.fixedx=oldgrid.fixedx;
  		this.fixedy=oldgrid.fixedy;
      if(!resizeGrid(oldgrid.dx,oldgrid.dy,false)) throw new IllegalArgumentException("Illegal grid size: "+oldgrid.dx+","+oldgrid.dy);
  	}
    addMouseListener(this);
    addMouseMotionListener(this);
    if(revert){
    	try{
	    	for(int x=0;x<wrappers.length;x++){
	    		for(int y=0;y<wrappers[x].length;y++){
	    			if(oldgrid.wrappers[y][x].component!=null){
		    			oldgrid.wrappers[y][x].cut();
		    			getToolkit().getSystemClipboard().getContents(this);
		    			selected=wrappers[x][y];
		    			selected.paste();
	    			}
	    		}
	    	}
    	}catch(Exception ex){
    		throw new RuntimeException("Problem copying the controls",ex);
    	}
    }else{
    	wrappers=oldgrid.wrappers;
    }
  }

  Grid(Editor editor,Element element)
    throws IllegalArgumentException, SecurityException
      , ClassNotFoundException, InstantiationException, IOException
      , IllegalAccessException, InvocationTargetException
      , NoSuchMethodException
  {
  	if(!element.getName().equals("grid")) 
      throw new IOException("invalid grid: "+element.getName());
    this.editor=editor;
    layout=new GridBagLayout();
    setLayout(layout);
    dx=Integer.parseInt(element.getAttributeValue("dx"));
    dy=Integer.parseInt(element.getAttributeValue("dy"));
    fixedx=Boolean.valueOf(element.getAttributeValue("fixedx")).booleanValue();
    fixedy=Boolean.valueOf(element.getAttributeValue("fixedy")).booleanValue();
    wrappers=new Wrapper[dx][dy];
    for(Iterator iterator=element.getChildren().iterator();iterator.hasNext();)
      new Wrapper((Element)iterator.next()).add(true);
    int x,y;
    for(x=0;x<dx;x++) for(y=0;y<dy;y++)
    { if(wrappers[x][y]==null) new Wrapper(null,x,y,1,1).add(true); }
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public Element save() 
    throws IllegalArgumentException, SecurityException, JDOMException
    , IOException, InstantiationException, IllegalAccessException
    , InvocationTargetException, NoSuchMethodException 
  {
    Element e=new Element("grid")
      .setAttribute("dx",String.valueOf(dx))
      .setAttribute("dy",String.valueOf(dy));
    if(fixedx){
    	e.setAttribute("fixedx",String.valueOf(fixedx));
    }
    if(fixedy){
    	e.setAttribute("fixedy",String.valueOf(fixedy));
    }
    int i,n=getComponentCount();
    Component c;
    Wrapper wrapper;
    ArrayList list=new ArrayList(n);
    for(i=0;i<n;i++)
    {
      c=getComponent(i);
      if(c instanceof Wrapper)
        list.add(c);
    }
    n=list.size();
    Collections.sort(list);
    for(i=0;i<n;i++)
    {
      wrapper=(Wrapper)list.get(i);
      if(wrapper.component!=null)
        e.addContent(wrapper.save());
    }
    return e;
  }
  
  public String saveJava(String classname,String packagename) throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
  	StringBuilder variables=new StringBuilder(),constructor=new StringBuilder();
  	MapObjectInt<String> namescount=new MapObjectInt<String>();
  	Set<String> imports=new TreeSet<String>();
  	saveJava("",variables,constructor,namescount,imports);
  	StringBuilder sb=new StringBuilder();
  	if(packagename!=null && packagename.length()>0) sb.append("package ").append(packagename).append(";\n");
  	sb.append("\n");
  	sb.append("import java.awt.GridBagConstraints;\n");
  	sb.append("import java.awt.GridBagLayout;\n");
  	sb.append("import java.awt.Insets;\n");
  	sb.append("import javax.swing.*;\n");
  	if(!imports.isEmpty()){
  		sb.append("\n");
  		for(String imp:imports){
  			sb.append("import ").append(imp).append(";\n");
  		}
  	}
  	sb.append("\n");
  	sb.append("class ").append(classname).append(" extends JPanel {\n");
  	sb.append("\n");
  	sb.append(variables);
  	sb.append("\n");
  	sb.append(classname).append("() {\n");
  	sb.append("super(new GridBagLayout());\n");
  	sb.append(constructor);
  	sb.append("}\n");
//  	sb.append("public static void main(String[] args) {\n");
//  	sb.append("JFrame frame=new JFrame();\n");
//  	sb.append("frame.setContentPane(new ").append(classname).append("());\n");
//  	sb.append("frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);\n");
//  	sb.append("frame.pack();\n");
//  	sb.append("frame.setVisible(true);\n");
//  	sb.append("}\n");
  	sb.append("\n");
  	sb.append("}\n");
  	return sb.toString();
  }
  
  private void saveJava(String container,StringBuilder variables,StringBuilder constructor,MapObjectInt<String> namescount,Set<String> imports) throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
  	int count=getComponentCount();
  	Component c;
  	Wrapper w;
  	for(int i=0;i<count;i++){
  		c=getComponent(i);
  		if(!(c instanceof Wrapper)) continue;
  		w=(Wrapper)c;
  		w.saveJava(container,variables,constructor,namescount,imports);
  	}
  }
  
  boolean resizeGrid(int dx,int dy,boolean check)
  {
    if((dx<1)||(dy<1)) return false;
    if((dx==this.dx)&&(dy==this.dy)) return true;
    int x,y;
    Wrapper wrapper;
    if((dx<this.dx)||(dy<this.dy))
    {
      for(x=0;x<this.dx;x++) for(y=0;y<this.dy;y++)
      {
        wrapper=wrappers[x][y];
        if(wrapper!=null)
          if(((wrapper.info.gridx+wrapper.info.gridwidth>dx)
              ||(wrapper.info.gridy+wrapper.info.gridheight>dy))
            &&(wrapper.component!=null)) return false;
      }
      if(!check) for(x=0;x<this.dx;x++) for(y=0;y<this.dy;y++)
      {
        wrapper=wrappers[x][y];
        if(wrapper!=null)
          if((wrapper.info.gridx+wrapper.info.gridwidth>dx)
              ||(wrapper.info.gridy+wrapper.info.gridheight>dy))
        {
          if(selected==wrapper) select(null);
          wrapper.remove(true);
        }
      }
    }
    if(!check)
    {
      Wrapper ws[][]=new Wrapper[dx][dy];
      
      for(x=0;x<dx;x++) for(y=0;y<dy;y++)
      {
        if((x<this.dx)&&(y<this.dy)) ws[x][y]=wrappers[x][y];
        if(ws[x][y]==null)
        {
          wrapper=new Wrapper(null,x,y,1,1);
          ws[x][y]=wrapper;
          wrapper.add(false);
        }
      }
      this.wrappers=ws; this.dx=dx; this.dy=dy;
    }
    return true;
  }
  
  private boolean move(boolean check)
  {
    if(dragging)
    {
      Point point=getPoint(dragx,dragy);
      int x,y,x1,y1,x2,y2;
      if(moving)
      {
        x1=point.x; x2=x1+selected.info.gridwidth;
        y1=point.y; y2=y1+selected.info.gridheight;
      }
      else
      {
        x1=selected.info.gridx; 
        x2=point.x+1;
        if(x2<=x1) return false;
        
        y1=selected.info.gridy;
        y2=point.y+1;
        if(y2<=y1) return false;
      }
      for(x=x1;x<x2;x++)
      {
        if((x<0)||(x>=dx)) return false;
        for(y=y1;y<y2;y++)
        {
          if((y<0)||(y>=dy)) return false;
          if((wrappers[x][y]!=selected)&&(wrappers[x][y].component!=null)) return false;
        }
      }
      if(!check)
      {
        selected.info.gridx=x1; selected.info.gridwidth=x2-x1;
        selected.info.gridy=y1; selected.info.gridheight=y2-y1;
        
        Wrapper wrapper;
        wrappers=new Wrapper[dx][dy];
        int i,n=getComponentCount();
        Component c;
        for(i=n-1;i>=0;i--)
        {
          c=getComponent(i);
          if(c instanceof Wrapper)
          {
            wrapper=(Wrapper)c;
            wrapper.remove(false);
            if(wrapper.component!=null)
            {
              for(x=wrapper.info.gridx;x<wrapper.info.gridx+wrapper.info.gridwidth;x++)
                for(y=wrapper.info.gridy;y<wrapper.info.gridy+wrapper.info.gridheight;y++)
                  wrappers[x][y]=wrapper;
            }
          }
        }

        for(x=0;x<dx;x++) for(y=0;y<dy;y++)
        {
          wrapper=wrappers[x][y];
          if(wrapper==null)
          {
            wrapper=new Wrapper(null,x,y,1,1);
            wrapper.add(true);
          }
          else
          {
            if(wrapper.getParent()==null)
              wrapper.add(false);
          }
        }
      }
    }
    return true;
  }
  
  public void paint(Graphics g)
  {
    super.paint(g);
    if(dragging)
    {
//System.out.println("paint Grid");
      g.setColor(move(true)?Color.green:Color.red);

      Rectangle r1=selected.getFullBounds();
      g.drawRect(r1.x,r1.y,r1.width,r1.height);

      Rectangle r2=getBounds(dragx,dragy);
      
      if(!moving) r2.setBounds(r1.x,r1.y,r2.x+r2.width-r1.x,r2.y+r2.height-r1.y);
      g.drawRect(r2.x,r2.y,r2.width,r2.height);
    }
  }
  
  public void paintComponent(Graphics g)
  {
    g.setColor(getBackground());
    int w=getWidth(),h=getHeight();
    g.fillRect(0,0,w,h);

    if(selected!=null)
    {
//System.out.println("paintComponent Grid");

      Rectangle r1=selected.getFullBounds();
      if(dragging)
      {
        g.setColor(Color.yellow);
        g.fillRect(r1.x,r1.y,r1.width,r1.height);

        Rectangle r2=getBounds(dragx,dragy);
//        Wrapper target=getWrapper(dragx,dragy);
//        Rectangle r2=target.getFullBounds();
        if(!moving) r2.setBounds(r1.x,r1.y,r2.x+r2.width-r1.x,r2.y+r2.height-r1.y);

        g.setColor(move(true)?Color.green:Color.red);
        g.fillRect(r2.x,r2.y,r2.width,r2.height);
      }
      else
      {
        g.setColor(Color.gray);
        g.fillRect(r1.x,r1.y,r1.width,r1.height);
      }

      if(selected.component!=null)
      {
        g.setColor(Color.orange);
        g.fillRect(r1.x,r1.y,12,12);
        g.fillRect(r1.x+r1.width-12,r1.y+r1.height-12,12,12);
      }
    }

    int tab[][]=layout.getLayoutDimensions();

    int dx=tab[0].length,dy=tab[1].length;
    if((dx<=0)||(dy<=0)) return;
    
    Point orig=layout.getLayoutOrigin();
    g.setColor(Color.lightGray);
    int i,x,y;
    
    x=orig.x;
    for(i=0;i<dx;i++)
    {
      g.drawLine(x,0,x,h);
      x+=tab[0][i];
    }
    g.drawLine(x,0,x,h);
    
    y=orig.y;
    for(i=0;i<dy;i++)
    {
      g.drawLine(0,y,w,y);
      y+=tab[1][i];
    }
    g.drawLine(0,y,w,y);
  }
  
  void select(Wrapper wrapper)
  {
    if(wrapper!=null) editor.select(this);
    if(selected!=null) { selected.setBorder(BORDER_NORMAL);  selected=null; }
    selected=wrapper;
    if(selected!=null)
    {
      selected.setBorder(BORDER_SELECTED);
      
      if((selected.component!=null)&&(selected.component.isFocusable()))
      {
        selected.component.requestFocusInWindow();
      }
      else
      {
        selected.requestFocusInWindow();
      }
//      setBorder(BORDER_SELECTED);
    }
    else
    {
      dragging=false;
//      setBorder(BORDER_NORMAL);
    }
    repaint();
  }

  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  public void mouseMoved(MouseEvent e)
  {
    int x=e.getX(),y=e.getY();
    Point p=layout.location(x,y);
    if((p.x<dx)&&(p.y<dy))
    {
      Wrapper wrapper=wrappers[p.x][p.y];
      mouseMoved(wrapper,x,y);
    }
  }

  public void mouseClicked(MouseEvent e)
  {
    int x=e.getX(),y=e.getY();
    Point p=layout.location(x,y);
    if((p.x<dx)&&(p.y<dy))
    {
      Wrapper wrapper=wrappers[p.x][p.y];
      mouseClicked(wrapper,x,y,e.getClickCount(),e.getButton(),e.isControlDown());
    }
  }
  
  private Wrapper getWrapper(int x,int y)
  {
    Point p=layout.location(x,y);
    p.setLocation(Math.min(p.x,dx-1),Math.min(p.y,dy-1));
    return wrappers[p.x][p.y];
  }

  private Rectangle getBounds(int x,int y)
  {
    Point p=layout.location(x,y);
    x=Math.min(p.x,dx-1); y=Math.min(p.y,dy-1);

    Point orig=layout.getLayoutOrigin();
    int i,tab[][]=layout.getLayoutDimensions();
    Rectangle r=new Rectangle(orig.x,orig.y,tab[0][x],tab[1][y]);
    for(i=0;i<x;i++) r.x+=tab[0][i];
    for(i=0;i<y;i++) r.y+=tab[1][i];
    return r;
  }

  private Point getPoint(int x,int y)
  {
    Point p=layout.location(x,y);
    p.setLocation(Math.min(p.x,dx-1),Math.min(p.y,dy-1));
    return p;
  }

  public void mousePressed(MouseEvent e)
  {
    int x=e.getX(),y=e.getY();
    Wrapper wrapper=getWrapper(x,y );
    mousePressed(wrapper,x,y,e.getButton());
  }
  
  public void mouseReleased(MouseEvent e)
  {
    int x=e.getX(),y=e.getY();
    Wrapper wrapper=getWrapper(x,y );
    mouseReleased(wrapper,x,y,e.getButton());
  }
  
  public void mouseDragged(MouseEvent e)
  {
    int x=e.getX(),y=e.getY();
    Wrapper wrapper=getWrapper(x,y );
    mouseDragged(wrapper,x,y,e.getClickCount(),e.getButton());
  }

  private int getCursor(Wrapper wrapper,int x,int y)
  {
    if(wrapper.component==null) return Cursor.DEFAULT_CURSOR;
    Rectangle bounds=wrapper.getFullBounds();
    if(!bounds.contains(x,y)) return Cursor.DEFAULT_CURSOR;
    if((x>bounds.x+bounds.width-12)&&(y>bounds.y+bounds.height-12))
      return Cursor.SE_RESIZE_CURSOR; 
    if((x<bounds.x+12)&&(y<bounds.y+12))
      return Cursor.MOVE_CURSOR; 
    return Cursor.DEFAULT_CURSOR; 
  }
  
  private void mouseMoved(Wrapper wrapper,int x,int y)
  {
    if(wrapper==selected)
      setCursor(Cursor.getPredefinedCursor(getCursor(wrapper,x,y)));
    else
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  private void mousePressed(Wrapper wrapper,int x,int y,int button)
  {
    if(button==MouseEvent.BUTTON1)
    {
      select(wrapper);

      int cursor=getCursor(wrapper,x,y);
      setCursor(Cursor.getPredefinedCursor(cursor));
      if(cursor!=Cursor.DEFAULT_CURSOR)
      {
        dragging=true; dragx=x; dragy=y; 
        moving=(cursor==Cursor.MOVE_CURSOR);
        repaint();
      }
    }
  }
  
  private void recalc()
  {
    setFont(Editor.FONT);
    revalidate(); repaint();
  }

  private void mouseReleased(Wrapper wrapper,int x,int y,int button)
  {
    if(dragging)
    {
      move(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      dragging=false;
      recalc();
    }
  }

  private void mouseDragged(Wrapper wrapper,int x,int y,int clicks,int button)
  {
    if(dragging)
    {
      dragx=x; dragy=y;
      repaint(200);
    }
  }

  private void mouseClicked(Wrapper wrapper,int x,int y,int clicks,int button,boolean control) 
  {
    select(wrapper); 
    if(control) editor.position();
    else if((button==MouseEvent.BUTTON1)&&(clicks==2)) editor.config();
    else if(button!=MouseEvent.BUTTON1) editor.popup(this,x,y);
  }
  
  void create(JComponent component) throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException 
  {
    if((selected!=null)&&(selected.component==null))
    {
      DialogComponent dialog=new DialogComponent(editor,component
          ,selected.tag,selected.mask,selected.obligatoire);
      component=dialog.component;
      if(component!=null)
      {
        selected.setComponent(component);
        selected.tag=dialog.tag;
        selected.mask=dialog.mask;
        selected.obligatoire=dialog.obligatoire;
        recalc();
      }
    }
  }
  
  void create(Element element) 
    throws IllegalArgumentException, SecurityException, ClassNotFoundException
      , InstantiationException, IOException, IllegalAccessException
      , InvocationTargetException, NoSuchMethodException
  {
    if((selected!=null)&&(selected.component==null))
    {
      Wrapper wrapper=new Wrapper(element);
      wrapper.info.gridx=selected.info.gridx;
      wrapper.info.gridy=selected.info.gridy;
      wrapper.info.gridwidth=selected.info.gridwidth;
      wrapper.info.gridheight=selected.info.gridheight;
      
      selected.remove(true);
      wrapper.add(true);
      selected=wrapper;
      recalc();
    }
  }
  
  void configurate() throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if((selected!=null)&&(selected.component!=null)&&(!(selected.component instanceof Grid)))
    {
      DialogComponent dialog=new DialogComponent(editor,selected.component
          ,selected.tag,selected.mask,selected.obligatoire);
      if(dialog.component!=null)
      {
        selected.tag=dialog.tag;
        selected.mask=dialog.mask;
        selected.obligatoire=dialog.obligatoire;
        recalc();
      }
    }
  }

  void delete() throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if((selected!=null)&&(selected.component!=null))
    {
      selected.setComponent(null);
      recalc();
    }
  }

  void position() throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if((selected!=null)&&(selected.component!=null))
    {
      selected.new DialogPosition();
    }
  }

  void deleteRow() throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if(selected!=null)
    {
      int y1=selected.info.gridy,y2=y1+selected.info.gridheight;
      if(dy>y2-y1)
      {
        int i,y,r,j,d;
        Component c;
        Wrapper wrapper;
        ArrayList list=new ArrayList();

        for(i=getComponentCount()-1;i>=0;i--)
        {
          c=getComponent(i);
          if(c instanceof Wrapper)
          {
            wrapper=(Wrapper)c;
            wrapper.remove(true);
            if(wrapper.component!=null)
            {
              r=0; j=wrapper.info.gridy; d=wrapper.info.gridheight;
              for(y=y1;y<y2;y++) if((j<=y)&&(j+d>y)) r++;
              wrapper.info.gridheight-=r;
              if(wrapper.info.gridheight>0)
              {
                if(j>y1) wrapper.info.gridy=Math.max(y1,j-(y2-y1));
                list.add(wrapper);
              }
            }
          }
        }
        for(i=list.size()-1;i>=0;i--)
        {
          wrapper=(Wrapper)list.get(i);
          wrapper.add(true);
        }
        resizeGrid(dx,dy-(y2-y1),false);
        select(null);
        recalc();
      }
    }
  }
  
  void deleteColumn() throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if(selected!=null)
    {
      int x1=selected.info.gridx,x2=x1+selected.info.gridwidth;
      if(dx>x2-x1)
      {
        int i,x,r,j,d;
        Component c;
        Wrapper wrapper;
        ArrayList list=new ArrayList();

        for(i=getComponentCount()-1;i>=0;i--)
        {
          c=getComponent(i);
          if(c instanceof Wrapper)
          {
            wrapper=(Wrapper)c;
            wrapper.remove(true);
            if(wrapper.component!=null)
            {
              r=0; j=wrapper.info.gridx; d=wrapper.info.gridwidth;
              for(x=x1;x<x2;x++) if((j<=x)&&(j+d>x)) r++;
              wrapper.info.gridwidth-=r;
              if(wrapper.info.gridwidth>0)
              {
                if(j>x1) wrapper.info.gridx=Math.max(x1,j-(x2-x1));
                list.add(wrapper);
              }
            }
          }
        }
        for(i=list.size()-1;i>=0;i--)
        {
          wrapper=(Wrapper)list.get(i);
          wrapper.add(true);
        }
        resizeGrid(dx-(x2-x1),dy,false);
        select(null);
        recalc();
      }
    }
  }

  void addRow(boolean before) throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if(selected!=null)
    {
      int y=selected.info.gridy; 
      if(!before) y+=selected.info.gridheight;
      int i;
      Component c;
      Wrapper wrapper;
      ArrayList list=new ArrayList();
      for(i=getComponentCount()-1;i>=0;i--)
      {
        c=getComponent(i);
        if(c instanceof Wrapper)
        {
          wrapper=(Wrapper)c; wrapper.remove(false); list.add(wrapper);
          if((wrapper.info.gridy<y)&&(wrapper.info.gridy+wrapper.info.gridheight>y))
            wrapper.info.gridheight++;
          else if(wrapper.info.gridy>=y)
            wrapper.info.gridy++;
        }
      }
      wrappers=new Wrapper[dx][++dy];
      for(i=list.size()-1;i>=0;i--)
      {
        wrapper=(Wrapper)list.get(i);
        wrapper.add(true);
      }
      for(i=0;i<dx;i++)
      { if(wrappers[i][y]==null) new Wrapper(null,i,y,1,1).add(true); }
      recalc();
    }
  }

  void addColumn(boolean before) throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
  {
    if(selected!=null)
    {
      int x=selected.info.gridx; 
      if(!before) x+=selected.info.gridwidth;
      int i;
      Component c;
      Wrapper wrapper;
      ArrayList list=new ArrayList();
      for(i=getComponentCount()-1;i>=0;i--)
      {
        c=getComponent(i);
        if(c instanceof Wrapper)
        {
          wrapper=(Wrapper)c; wrapper.remove(false); list.add(wrapper);
          if((wrapper.info.gridx<x)&&(wrapper.info.gridx+wrapper.info.gridwidth>x))
            wrapper.info.gridwidth++;
          else if(wrapper.info.gridx>=x)
            wrapper.info.gridx++;
        }
      }
      wrappers=new Wrapper[++dx][dy];
      for(i=list.size()-1;i>=0;i--)
      {
        wrapper=(Wrapper)list.get(i);
        wrapper.add(true);
      }
      for(i=0;i<dy;i++)
      { if(wrappers[x][i]==null) new Wrapper(null,x,i,1,1).add(true); }
      recalc();
    }
  }

//-------------------------------------------------------------
  final class Wrapper extends JPanel 
    implements MouseListener,MouseMotionListener,KeyListener,Comparable
  {
    GridBagConstraints info;
    JComponent component;
    
    String tag,mask;
    boolean obligatoire;
    
    Wrapper(JComponent component,int x,int y,int dx,int dy)
    {
      super(new BorderLayout(4,4));
      setOpaque(false);
      setBorder(BORDER_NORMAL);
      info=(GridBagConstraints)Player.CONSTRAINT.clone();
      info.gridx=x; info.gridy=y;
      info.gridwidth=dx; info.gridheight=dy;
      setComponent(component);
//      setPreferredSize(new Dimension(8,8));
    }

    Wrapper(Element element) throws IllegalArgumentException
      , SecurityException, ClassNotFoundException, InstantiationException
      , IOException, IllegalAccessException, InvocationTargetException
      , NoSuchMethodException
    {
      super(new BorderLayout(4,4));
      if(!element.getName().equals("wrapper")) 
        throw new IOException("invalid wrapper: "+element.getName());
//      setPreferredSize(new Dimension(8,8));
      setOpaque(false);
      setBorder(BORDER_NORMAL);
      tag=element.getAttributeValue("tag");
      mask=element.getAttributeValue("mask");
      obligatoire="true".equals(element.getAttributeValue("mandatory"));
      Element c;
      c=element.getChild("constraint");
      info=new GridBagConstraints(
          Integer.parseInt(c.getAttributeValue("gridx"))
          ,Integer.parseInt(c.getAttributeValue("gridy"))
          ,Player.getOrDef(c,"gridwidth",Player.CONSTRAINT.gridwidth)
          ,Player.getOrDef(c,"gridheight",Player.CONSTRAINT.gridheight)
          ,Player.getOrDef(c,"weightx",Player.CONSTRAINT.weightx)
          ,Player.getOrDef(c,"weighty",Player.CONSTRAINT.weighty)
          ,Player.getOrDef(c,"anchor",Player.CONSTRAINT.anchor)
          ,Player.getOrDef(c,"fill",Player.CONSTRAINT.fill)
          ,new Insets(Player.getOrDef(c,"insets.top",Player.CONSTRAINT.insets.top)
              ,Player.getOrDef(c,"insets.left",Player.CONSTRAINT.insets.left)
              ,Player.getOrDef(c,"insets.bottom",Player.CONSTRAINT.insets.bottom)
              ,Player.getOrDef(c,"insets.right",Player.CONSTRAINT.insets.right))
          ,Player.getOrDef(c,"ipadx",Player.CONSTRAINT.ipadx)
          ,Player.getOrDef(c,"ipady",Player.CONSTRAINT.ipady)
        );
      setComponent(Player.load(element.getChild("component")));
      //
      if((component==null)||(component instanceof JSplitPane)
        ||(component instanceof JScrollPane)||(component instanceof JTabbedPane)
        ||(component instanceof JToolBar))
      {
        c=element.getChild("content");
        if(c==null) throw new IOException("no content");
        List list=c.getChildren();
        if(component==null) // Grid
          setComponent(new Grid(editor,(Element)list.get(0)));
        else if(component instanceof JSplitPane)
        {
          JSplitPane split=(JSplitPane)component;
          split.setLeftComponent(new Grid(editor,(Element)list.get(0)));
          split.setRightComponent(new Grid(editor,(Element)list.get(1)));
        }
        else if(component instanceof JScrollPane)
        {
          JScrollPane scroll=(JScrollPane)component;
          scroll.setViewportView(new Grid(editor,(Element)list.get(0)));
        }
        else if(component instanceof JTabbedPane)
        {
          JTabbedPane tabbed=(JTabbedPane)component;
          String string;
          for(int i=0;i<list.size();i++)
          {
            c=(Element)list.get(i);
            if(!c.getName().equals("tab")) 
              throw new IOException("invalid tab: "+c.getName());
            tabbed.add(c.getAttributeValue("title"),new Grid(editor,c.getChild("grid")));
            tabbed.setMnemonicAt(i,Integer.parseInt(c.getAttributeValue("mnemonic")));
            string=c.getAttributeValue("icon");
            if(string!=null) tabbed.setIconAt(i,ToolkitHelper.getIcon(string));
          }
        }
        else if(component instanceof JToolBar)
        {
        	JToolBar bar=(JToolBar)component;
          bar.add(new Grid(editor,(Element)list.get(0)));
        }
      }
      c=element.getChild("border");
      if(c!=null)
      {
        String type=c.getAttributeValue("type");
        if("TitledBorder".equals(type))
          component.setBorder(new TitledBorder(c.getText()));
        else if("BevelBorder".equals(type))
          component.setBorder(new BevelBorder(Integer.parseInt(c.getText())));
        else
          throw new IOException("invalid border: "+type);
      }
      tag=element.getAttributeValue("tag");
      mask=element.getAttributeValue("mask");
      obligatoire="true".equals(element.getAttributeValue("mandatory"));
    }
    
    

    Element save() 
      throws IllegalArgumentException, SecurityException, JDOMException
      , IOException, InstantiationException, IllegalAccessException
      , InvocationTargetException, NoSuchMethodException 
    {
      Element e=new Element("wrapper");
      if(tag!=null) e.setAttribute("tag",tag);
      if(mask!=null) e.setAttribute("mask",mask);
      if(obligatoire) e.setAttribute("mandatory","true");
      
      Element c=new Element("constraint");
      c.setAttribute("gridx",String.valueOf(info.gridx));
      c.setAttribute("gridy",String.valueOf(info.gridy));
      Player.setIfNotDef(c,"gridwidth",info.gridwidth,Player.CONSTRAINT.gridwidth);
      Player.setIfNotDef(c,"gridheight",info.gridheight,Player.CONSTRAINT.gridheight);
      Player.setIfNotDef(c,"weightx",info.weightx,Player.CONSTRAINT.weightx);
      Player.setIfNotDef(c,"weighty",info.weighty,Player.CONSTRAINT.weighty);
      Player.setIfNotDef(c,"anchor",info.anchor,Player.CONSTRAINT.anchor);
      Player.setIfNotDef(c,"fill",info.fill,Player.CONSTRAINT.fill);
      Player.setIfNotDef(c,"insets.top",info.insets.top,Player.CONSTRAINT.insets.top);
      Player.setIfNotDef(c,"insets.left",info.insets.left,Player.CONSTRAINT.insets.left);
      Player.setIfNotDef(c,"insets.bottom",info.insets.bottom,Player.CONSTRAINT.insets.bottom);
      Player.setIfNotDef(c,"insets.right",info.insets.right,Player.CONSTRAINT.insets.right);
      Player.setIfNotDef(c,"ipadx",info.ipadx,Player.CONSTRAINT.ipadx);
      Player.setIfNotDef(c,"ipady",info.ipady,Player.CONSTRAINT.ipady);
      e.addContent(c);
      
      if(!(component instanceof Grid))
        e.addContent(DialogComponent.save(component));
      if(component instanceof Grid)
      {
        Grid grid=(Grid)component;
        e.addContent(new Element("content").addContent(grid.save()));
      }
      else if(component instanceof JSplitPane)
      {
        JSplitPane split=(JSplitPane)component;
        e.addContent(new Element("content")
            .addContent(((Grid)split.getLeftComponent()).save())
            .addContent(((Grid)split.getRightComponent()).save()));
      }
      else if(component instanceof JScrollPane)
      {
        JScrollPane scroll=(JScrollPane)component;
        e.addContent(new Element("content")
            .addContent(((Grid)scroll.getViewport().getView()).save()));
      }
      else if(component instanceof JTabbedPane)
      {
        JTabbedPane tabbed=(JTabbedPane)component;
        Element content=new Element("content"),tab;
        ImageIcon icon;
        for(int i=0;i<tabbed.getTabCount();i++)
        {
          tab=new Element("tab");
          tab.setAttribute("index",String.valueOf(i));
          tab.setAttribute("title",tabbed.getTitleAt(i));
          tab.setAttribute("mnemonic",String.valueOf(tabbed.getMnemonicAt(i)));
          icon=(ImageIcon)tabbed.getIconAt(i);
          if(icon!=null) tab.setAttribute("icon",icon.getDescription());
          tab.addContent(((Grid)tabbed.getComponentAt(i)).save());
          content.addContent(tab);
        }
        e.addContent(content);
      }
      else if(component instanceof JToolBar)
      {
        JToolBar bar=(JToolBar)component;
        e.addContent(new Element("content")
            .addContent(((Grid)bar.getComponent(0)).save()));
      }
      Border border=component.getBorder();
      if(border!=null)
      {
        if(border instanceof TitledBorder)
          e.addContent(new Element("border").setAttribute("type","TitledBorder").addContent(((TitledBorder)border).getTitle()));
        else if(border instanceof BevelBorder)
          e.addContent(new Element("border").setAttribute("type","BevelBorder").addContent(String.valueOf(((BevelBorder)border).getBevelType())));
      }
      return e;
    }
    
    private void saveJava(String container,StringBuilder variables,StringBuilder constructor,MapObjectInt<String> namescount,Set<String> imports) throws IllegalArgumentException, SecurityException, JDOMException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    	if(component==null) return;
    	String classname;
    	if(component instanceof Grid){
    		classname="JPanel";
    	}else{
    		classname=component.getClass().getName();
    		if(classname.startsWith("javax.swing.")) classname=classname.substring(12);
    		else{
    			imports.add(classname);
    			classname=classname.substring(classname.lastIndexOf('.')+1);
    		}
    	}
    	String name=StringHelper.getStringTrimOrNull(component.getName());
    	if(name==null) name="comp";
    	else name=name.replace('-','_');
    	int count=namescount.get(name,0);
    	namescount.put(name,count+1);
    	if(count>0) name=name+count;
    	if(component.getName()==null){
    		constructor.append(classname).append(' ');
    	}else{
    		variables.append("final ").append(classname).append(' ').append(name).append(";\n");
    	}
    	constructor.append(name).append("=new ").append(classname).append("(");
    	if(component instanceof Grid) constructor.append("new GridBagLayout()");
    	constructor.append(");\n");
    	if(!(component instanceof Grid)){
    		for(Element param:DialogComponent.save(component).getChildren("param")){
    			String paramclass=param.getAttributeValue("class");
    			String paramname=param.getAttributeValue("name");
    			//TODO composants imbriqués dans le paramname séparés par des /
    			if("java.lang.String".equals(paramclass)){
    				if("Name".equals(paramname)) continue;
    				constructor.append(name).append(".set").append(paramname).append("(\"").append(param.getText()).append("\");\n");
    			}else if("java.lang.Integer".equals(paramclass)||"java.lang.Long".equals(paramclass)||"java.lang.Boolean".equals(paramclass)){
    				constructor.append(name).append(".set").append(paramname).append("(").append(param.getText()).append(");\n");
    			}else if("java.lang.Float".equals(paramclass)||"java.lang.Double".equals(paramclass)){
    				String value=param.getText();
    				if("Infinity".equals(value)) value=paramclass.substring(10)+".POSITIVE_INFINITY";
    				else if("-Infinity".equals(value)) value=paramclass.substring(10)+".NEGATIVE_INFINITY";
    				constructor.append(name).append(".set").append(paramname).append("(").append(value).append(");\n");
    			}else if("javax.swing.ImageIcon".equals(paramclass)){
    				imports.add("be.immedia.util.ToolkitHelper");
    				constructor.append(name).append(".set").append(paramname).append("(ToolkitHelper.getIcon(\"").append(param.getText()).append("\"));\n");
    			}else{
    				System.err.println("Unknown param type "+paramclass+" for param "+paramname);
    			}
    		}
    	}
    	constructor.append(container).append("add(").append(name)
    	           .append(",new GridBagConstraints(").append(info.gridx)
    	           .append(',').append(info.gridy)
    	           .append(',').append(info.gridwidth)
    	           .append(',').append(info.gridheight)
    	           .append(',').append(info.weightx)
    	           .append(',').append(info.weighty)
    	           .append(',').append(getAnchor(info.anchor))
    	           .append(',').append(getFill(info.fill))
    	           .append(",new Insets(").append(info.insets.top).append(',').append(info.insets.left).append(',').append(info.insets.bottom).append(',').append(info.insets.right).append(')')
    	           .append(',').append(info.ipadx)
    	           .append(',').append(info.ipady)
    	           .append("));\n");
    	if(component instanceof Grid){
    		((Grid)component).saveJava(name+'.',variables,constructor,namescount,imports);
    	}else if(component instanceof JSplitPane){
    		JSplitPane split=(JSplitPane)component;
    		constructor.append("JPanel ").append(name).append("Left=new JPanel(new GridBagLayout());\n");
    		constructor.append(name).append(".setLeftComponent(").append(name).append("Left);\n");
    		((Grid)split.getLeftComponent()).saveJava(name+"Left.",variables,constructor,namescount,imports);
    		constructor.append("JPanel ").append(name).append("Right=new JPanel(new GridBagLayout());\n");
    		constructor.append(name).append(".setRightComponent(").append(name).append("Right);\n");
    		((Grid)split.getRightComponent()).saveJava(name+"Right.",variables,constructor,namescount,imports);
    	}else if(component instanceof JScrollPane){
    		JScrollPane scroll=(JScrollPane)component;
    		constructor.append("JPanel ").append(name).append("View=new JPanel(new GridBagLayout());\n");
    		constructor.append(name).append(".setViewportView(").append(name).append("View);\n");
    		((Grid)scroll.getViewport().getView()).saveJava(name+"View.",variables,constructor,namescount,imports);
    	}else if(component instanceof JTabbedPane){
    		JTabbedPane tabbed=(JTabbedPane)component;
    		for(int i=0;i<tabbed.getTabCount();i++){
    			String tabname=name+"Tab"+i;
    			constructor.append("JPanel ").append(tabname).append("=new JPanel(new GridBagLayout());\n");
    			constructor.append(name).append(".addTab(\"").append(tabbed.getTitleAt(i)).append("\",").append(tabname).append(");\n");
    			((Grid)tabbed.getComponentAt(i)).saveJava(tabname+'.',variables,constructor,namescount,imports);
    		}
    	}
    	//TODO toolbar
    }
    
    private String getAnchor(int anchor) {
    	switch(anchor){
    		case GridBagConstraints.CENTER:    return "GridBagConstraints.CENTER";
    		case GridBagConstraints.NORTH:     return "GridBagConstraints.NORTH";
    		case GridBagConstraints.NORTHEAST: return "GridBagConstraints.NORTHEAST";
    		case GridBagConstraints.EAST:      return "GridBagConstraints.EAST";
    		case GridBagConstraints.SOUTHEAST: return "GridBagConstraints.SOUTHEAST";
    		case GridBagConstraints.SOUTH:     return "GridBagConstraints.SOUTH";
    		case GridBagConstraints.SOUTHWEST: return "GridBagConstraints.SOUTHWEST";
    		case GridBagConstraints.WEST:      return "GridBagConstraints.WEST";
    		case GridBagConstraints.NORTHWEST: return "GridBagConstraints.NORTHWEST";
    		default: return String.valueOf(anchor);
    	}
    }
    
    private String getFill(int fill) {
    	switch(fill){
    		case GridBagConstraints.NONE:       return "GridBagConstraints.NONE";
    		case GridBagConstraints.HORIZONTAL: return "GridBagConstraints.HORIZONTAL";
    		case GridBagConstraints.VERTICAL:   return "GridBagConstraints.VERTICAL";
    		case GridBagConstraints.BOTH:       return "GridBagConstraints.BOTH";
    		default: return String.valueOf(fill);
    	}
    }
    
    void add(boolean grid)
    {
      if(grid)
      {
        int x,y;
        for(x=info.gridx;x<info.gridx+info.gridwidth;x++)
          for(y=info.gridy;y<info.gridy+info.gridheight;y++)
            wrappers[x][y]=this;
      }
      Grid.this.add(this,info);
      addMouseListener(this);
      addMouseMotionListener(this);
      addKeyListener(this);
      setFocusable(true);
    }
    
    void remove(boolean grid)
    {
      if(grid)
      {
        int x,y;
        for(x=info.gridx;x<info.gridx+info.gridwidth;x++)
          for(y=info.gridy;y<info.gridy+info.gridheight;y++)
            wrappers[x][y]=null;
      }
      Grid.this.remove(this);
      removeMouseListener(this);
      removeMouseMotionListener(this);
      removeKeyListener(this);
    }
    
    Rectangle getFullBounds()
    {
      Point orig=layout.getLayoutOrigin();
      int i,tab[][]=layout.getLayoutDimensions();
      int x=info.gridx,y=info.gridy;
      Rectangle r=new Rectangle(orig.x,orig.y,0,0);
      for(i=0;i<x;i++) r.x+=tab[0][i];
      for(i=x;i<x+info.gridwidth;i++) r.width+=tab[0][i];
      for(i=0;i<y;i++) r.y+=tab[1][i];
      for(i=y;i<y+info.gridheight;i++) r.height+=tab[1][i];
      return r;
    }
    
    void setComponent(JComponent c)
    {
      tag=mask=null; obligatoire=false;
      if(component==c) return;
      
      if(component!=null)
      {
        removeAll();
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        component.removeKeyListener(this);
        component=null;
      }
      
      if(c!=null)
      {
        component=c;
        add(component,BorderLayout.CENTER);
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addKeyListener(this);
        component.setFocusable(true);
      }
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    
    public void mouseMoved(MouseEvent e)
    {
      int x=e.getX(),y=e.getY();
      Component c=(Component)e.getSource();
      while(c!=Grid.this) { x+=c.getX(); y+=c.getY(); c=c.getParent(); }
      Grid.this.mouseMoved(this,x,y);
    }

    public void mouseClicked(MouseEvent e)
    {
      int x=e.getX(),y=e.getY();
      Component c=(Component)e.getSource();
      while(c!=Grid.this) { x+=c.getX(); y+=c.getY(); c=c.getParent(); }
      Grid.this.mouseClicked(this,x,y,e.getClickCount(),e.getButton(),e.isControlDown());
    }
    
    public void mousePressed(MouseEvent e)
    {
      int x=e.getX(),y=e.getY();
      Component c=(Component)e.getSource();
      while(c!=Grid.this) { x+=c.getX(); y+=c.getY(); c=c.getParent(); }
      Grid.this.mousePressed(this,x,y,e.getButton());
    }
    
    public void mouseReleased(MouseEvent e)
    {
      int x=e.getX(),y=e.getY();
      Component c=(Component)e.getSource();
      while(c!=Grid.this) { x+=c.getX(); y+=c.getY(); c=c.getParent(); }
      Grid.this.mouseReleased(this,x,y,e.getButton());
    }
    
    public void mouseDragged(MouseEvent e)
    {
      int x=e.getX(),y=e.getY();
      Component c=(Component)e.getSource();
      while(c!=Grid.this) { x+=c.getX(); y+=c.getY(); c=c.getParent(); }
      Grid.this.mouseDragged(this,x,y,e.getClickCount(),e.getButton());
    }
    
    private final class DialogPosition extends JDialog
      implements ActionListener,ItemListener,DocumentListener
    {
      private JRadioButton rbFN,rbFH,rbFV,rbFB;
      private JRadioButton rbA[][];
      private JButton bOk;
      private JTextField fwx,fwy,fpx,fpy,fml,fmr,fmt,fmb;
      
      DialogPosition()
      {
        super(editor,true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        JPanel p,sp;
        ButtonGroup bg;
        JLabel label;
        int x,y;
        getContentPane().add(p=new JPanel(new TableLayout(2)),BorderLayout.CENTER);

        p.add(label=new JLabel("Remplissage : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(sp=new JPanel(new GridLayout(0,1)));
        sp.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bg=new ButtonGroup();
        sp.add(rbFN=new JRadioButton("Aucun",info.fill==GridBagConstraints.NONE));
        sp.add(rbFH=new JRadioButton("Horizontal",info.fill==GridBagConstraints.HORIZONTAL));
        sp.add(rbFV=new JRadioButton("Vertical",info.fill==GridBagConstraints.VERTICAL));
        sp.add(rbFB=new JRadioButton("Les deux",info.fill==GridBagConstraints.BOTH));
        rbFN.addItemListener(this); bg.add(rbFN);
        rbFH.addItemListener(this); bg.add(rbFH);
        rbFV.addItemListener(this); bg.add(rbFV);
        rbFB.addItemListener(this); bg.add(rbFB);
        
        p.add(label=new JLabel("Ancrage : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(sp=new JPanel(new GridLayout(3,3)));
        sp.setBorder(new BevelBorder(BevelBorder.LOWERED));
        rbA=new JRadioButton[3][3];
        bg=new ButtonGroup();
        sp.add(rbA[0][0]=new JRadioButton("NO",info.anchor==GridBagConstraints.NORTHWEST));
        sp.add(rbA[1][0]=new JRadioButton("N",info.anchor==GridBagConstraints.NORTH));
        sp.add(rbA[2][0]=new JRadioButton("NE",info.anchor==GridBagConstraints.NORTHEAST));
        sp.add(rbA[0][1]=new JRadioButton("O",info.anchor==GridBagConstraints.WEST));
        sp.add(rbA[1][1]=new JRadioButton("C",info.anchor==GridBagConstraints.CENTER));
        sp.add(rbA[2][1]=new JRadioButton("E",info.anchor==GridBagConstraints.EAST));
        sp.add(rbA[0][2]=new JRadioButton("SO",info.anchor==GridBagConstraints.SOUTHWEST));
        sp.add(rbA[1][2]=new JRadioButton("S",info.anchor==GridBagConstraints.SOUTH));
        sp.add(rbA[2][2]=new JRadioButton("SE",info.anchor==GridBagConstraints.SOUTHEAST));
        for(x=0;x<3;x++) for(y=0;y<3;y++)
        { rbA[x][y].addItemListener(this); bg.add(rbA[x][y]); }

        p.add(label=new JLabel("Poids X : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fwx=new JTextField(String.valueOf(info.weightx),3));
        fwx.getDocument().addDocumentListener(this);
        p.add(label=new JLabel("Poids Y : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fwy=new JTextField(String.valueOf(info.weighty),3));
        fwy.getDocument().addDocumentListener(this);

        p.add(label=new JLabel("Marge interne H : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fpx=new JTextField(String.valueOf(info.ipadx),3));
        fpx.getDocument().addDocumentListener(this);
        p.add(label=new JLabel("Marge interne V : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fpy=new JTextField(String.valueOf(info.ipady),3));
        fpy.getDocument().addDocumentListener(this);

        p.add(label=new JLabel("Marge ext. gauche : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fml=new JTextField(String.valueOf(info.insets.left),3));
        fml.getDocument().addDocumentListener(this);
        p.add(label=new JLabel("Marge ext droite : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fmr=new JTextField(String.valueOf(info.insets.right),3));
        fmr.getDocument().addDocumentListener(this);

        p.add(label=new JLabel("Marge ext. haut : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fmt=new JTextField(String.valueOf(info.insets.top),3));
        fmt.getDocument().addDocumentListener(this);
        p.add(label=new JLabel("Marge ext. bas : ")); label.setVerticalAlignment(JLabel.TOP);
        p.add(fmb=new JTextField(String.valueOf(info.insets.bottom),3));
        fmb.getDocument().addDocumentListener(this);

        getContentPane().add(p=new JPanel(new FlowLayout()),BorderLayout.SOUTH);
        p.add(bOk=new JButton("OK")); bOk.addActionListener(this);
        
        pack();
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size=getSize();
        Rectangle r=Wrapper.this.getFullBounds();
        Point l=Grid.this.getLocationOnScreen();
        r.translate(l.x,l.y);
        
        if(2*r.x+r.width<screen.width) 
          x=Math.min(r.x+r.width,screen.width-size.width);
        else 
          x=Math.max(r.x-size.width,0);
        if(2*r.y+r.height<screen.height) 
          y=Math.min(r.y+r.height,screen.height-size.height);
        else 
          y=Math.max(r.y-size.height,0);
        
        setLocation(x,y);
        setVisible(true);
      }
      public void actionPerformed(ActionEvent e)
      {
        Object source=e.getSource();
        if(source==bOk) dispose();
      }
      
      public void itemStateChanged(ItemEvent e) 
      {
        if((e.getStateChange()==ItemEvent.SELECTED)&&(e.getSource() instanceof JRadioButton))
        {
          if(rbFH.isSelected()) info.fill=GridBagConstraints.HORIZONTAL;
          else if(rbFV.isSelected()) info.fill=GridBagConstraints.VERTICAL;
          else if(rbFB.isSelected()) info.fill=GridBagConstraints.BOTH;
          else info.fill=GridBagConstraints.NONE;
          
          if(rbA[0][0].isSelected()) info.anchor=GridBagConstraints.NORTHWEST;
          else if(rbA[1][0].isSelected()) info.anchor=GridBagConstraints.NORTH;
          else if(rbA[2][0].isSelected()) info.anchor=GridBagConstraints.NORTHEAST;
          else if(rbA[0][1].isSelected()) info.anchor=GridBagConstraints.WEST;
          else if(rbA[2][1].isSelected()) info.anchor=GridBagConstraints.EAST;
          else if(rbA[0][2].isSelected()) info.anchor=GridBagConstraints.SOUTHWEST;
          else if(rbA[1][2].isSelected()) info.anchor=GridBagConstraints.SOUTH;
          else if(rbA[2][2].isSelected()) info.anchor=GridBagConstraints.SOUTHEAST;
          else info.anchor=GridBagConstraints.CENTER;

          layout.setConstraints(Wrapper.this,info);
          recalc();
        }
      }
      
      private double getDouble(JTextField field,double def)
      {
        double v=def;
        try
        {
          String string=field.getText().trim();
          v=Double.parseDouble(string.replace(',','.'));
          if(v<0) v=0; else if(v>1000) v=1000;
        }
        catch(Exception ex)
        {}
        return v;
      }

      private int getInteger(JTextField field,int def)
      {
        int v=def;
        try
        {
          String string=field.getText().trim();
          v=Integer.parseInt(string);
          if(v<0) v=0; else if(v>1000) v=1000;
        }
        catch(Exception ex)
        {}
        return v;
      }

      private void changed()
      {
        info.weightx=getDouble(fwx,info.weightx);
        info.weighty=getDouble(fwy,info.weighty);
        info.ipadx=getInteger(fpx,info.ipadx);
        info.ipady=getInteger(fpy,info.ipady);
        info.insets.left=getInteger(fml,info.insets.left);
        info.insets.right=getInteger(fmr,info.insets.right);
        info.insets.top=getInteger(fmt,info.insets.top);
        info.insets.bottom=getInteger(fmb,info.insets.bottom);
        layout.setConstraints(Wrapper.this,info);
        Grid.this.setFont(Editor.FONT);
        Grid.this.revalidate(); Grid.this.repaint();
      }
      
      public void changedUpdate(DocumentEvent e) { changed(); }
      public void insertUpdate(DocumentEvent e) { changed(); }
      public void removeUpdate(DocumentEvent e) { changed(); }
    }

    public void keyPressed(KeyEvent e){}
    public void keyReleased(KeyEvent e){}

    public void keyTyped(KeyEvent e)
    {
      try
      {
        if(e.isControlDown())
        {
          switch(e.getKeyChar())
          {
            case 3: copy(); break;
            case 22: paste(); break;
            case 24: cut(); break;
          }
        }
      }
      catch(Exception ex)
      { ex.printStackTrace(); }
    }
    
    void copy() throws Exception
    {
      if(component!=null)
      {
        Element element=save();
        String string=new XMLOutputter().outputString(new Document(element));
        StringSelection selection=new StringSelection(string);
        getToolkit().getSystemClipboard().setContents(selection, selection);
      }
    }
    
    void cut() throws Exception
    {
      if(component!=null)
      {
        copy();
        Grid.this.delete();
      }
    }
    
    void paste() throws Exception
    {
      if(component==null)
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
            
            Document document=new SAXBuilder().build(reader);
            Grid.this.create(document.getRootElement());
          }
          finally
          { reader.close(); }
        }
      }
    }

    public int compareTo(Object o)
    {
      Wrapper w=(Wrapper)o;
      if(info.gridy>w.info.gridy) return 1;
      if(info.gridy<w.info.gridy) return -1;
      if(info.gridx>w.info.gridx) return 1;
      if(info.gridx<w.info.gridx) return -1;
      return 0;
    }
  }
//-------------------------------------------------------------
}
